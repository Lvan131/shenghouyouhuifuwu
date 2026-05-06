const { request } = require('../../utils/request')
const { fetchUserState } = require('../../utils/user-state')
const { activitySummary, formatDistance } = require('../../utils/display')

function decorateRecommendations(recommendations, joinedMap) {
  return recommendations.map((item) => ({
    ...item,
    joinedStatus: joinedMap[item.activityId] || '',
    distanceText: formatDistance(item.distanceKm),
    summaryText: activitySummary(item)
  }))
}

Page({
  data: {
    loading: false,
    recommendations: [],
    joinedMap: {}
  },

  async onLoad() {
    await this.loadRecommendations()
  },

  async onShow() {
    if (this.data.recommendations.length) {
      await this.loadRecommendations()
    }
  },

  async onPullDownRefresh() {
    await this.loadRecommendations()
    wx.stopPullDownRefresh()
  },

  async loadRecommendations() {
    this.setData({ loading: true })
    try {
      await getApp().ensureLogin()
      const [recommendations, userState] = await Promise.all([
        request({
          url: '/merchants/recommendations'
        }),
        fetchUserState()
      ])
      this.setData({
        recommendations: decorateRecommendations(recommendations, userState.joinedMap),
        joinedMap: userState.joinedMap
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
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
      await this.loadRecommendations()
    } catch (error) {
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none'
      })
    }
  }
})
