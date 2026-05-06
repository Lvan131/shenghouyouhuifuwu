const { request } = require('../../utils/request')
const { fetchUserState } = require('../../utils/user-state')
const { activitySummary, formatDistance, formatRating } = require('../../utils/display')

function decorateActivities(activities, joinedMap) {
  return activities.map((item) => ({
    ...item,
    joinedStatus: joinedMap[item.activityId] || '',
    distanceText: formatDistance(item.distanceKm),
    summaryText: activitySummary(item)
  }))
}

Page({
  data: {
    merchantId: '',
    merchantName: '',
    ratingText: '暂无评价',
    activities: []
  },

  async onLoad(options) {
    const merchantName = options.merchantName ? decodeURIComponent(options.merchantName) : ''
    this.setData({
      merchantId: options.merchantId || '',
      merchantName,
      ratingText: formatRating(options.avgScore, options.ratingCount)
    })
    if (merchantName) {
      wx.setNavigationBarTitle({
        title: merchantName
      })
    }
    await this.loadActivities()
  },

  async onShow() {
    if (this.data.merchantId) {
      await this.loadActivities()
    }
  },

  async loadActivities() {
    try {
      await getApp().ensureLogin()
      const [activities, userState] = await Promise.all([
        request({
          url: `/merchants/${this.data.merchantId}/activities`
        }),
        fetchUserState()
      ])
      const decoratedActivities = decorateActivities(activities, userState.joinedMap)
      let ratingText = this.data.ratingText
      if (decoratedActivities.length) {
        ratingText = formatRating(decoratedActivities[0].avgScore, decoratedActivities[0].ratingCount)
      }
      this.setData({
        activities: decoratedActivities,
        ratingText
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
    }
  },

  goDetail(e) {
    wx.navigateTo({
      url: `/pages/activity-detail/index?id=${e.currentTarget.dataset.id}`
    })
  },

  async handleJoinToggle(e) {
    const activityId = e.currentTarget.dataset.activityId
    const joined = e.currentTarget.dataset.joined === 'true'
    try {
      await getApp().ensureLogin()
      if (!joined) {
        wx.navigateTo({
          url: `/pages/activity-detail/index?id=${activityId}`
        })
        return
      }
      await request({
        url: `/activities/${activityId}/cancel-join`,
        method: 'POST'
      })
      wx.showToast({
        title: '已取消报名',
        icon: 'success'
      })
      await this.loadActivities()
    } catch (error) {
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none'
      })
    }
  }
})
