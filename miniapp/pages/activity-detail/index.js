const { request } = require('../../utils/request')
const { fetchUserState } = require('../../utils/user-state')
const { formatDistance, formatRating } = require('../../utils/display')

function decorateScheduleOptions(options = []) {
  return options.map((item) => ({
    ...item,
    date: item.date,
    label: `${item.date} · 剩余 ${item.remainingQuota} / ${item.dailyQuota}`,
    fullText: item.full ? '当日名额已满，请选择其他日期' : ''
  }))
}

Page({
  data: {
    activityId: null,
    detail: null,
    loading: false,
    joinedStatus: '',
    subscribed: false,
    distanceText: '',
    merchantRatingText: '暂无评价',
    scheduleOptions: [],
    selectedScheduleIndex: 0,
    selectedSchedule: null,
    joinedRecord: null,
    joinDisabled: false
  },

  async onLoad(options) {
    this.setData({
      activityId: options.id
    })
    await this.loadDetail()
  },

  async onShow() {
    if (this.data.activityId) {
      await this.loadDetail()
    }
  },

  updateSelectedSchedule(index) {
    const selectedSchedule = this.data.scheduleOptions[index] || null
    this.setData({
      selectedScheduleIndex: index,
      selectedSchedule,
      joinDisabled: this.data.joinedStatus !== 'JOINED' && !!(selectedSchedule && selectedSchedule.full)
    })
  },

  resolveDefaultScheduleIndex(scheduleOptions, joinedRecord) {
    if (joinedRecord && joinedRecord.participationDate) {
      const joinedIndex = scheduleOptions.findIndex((item) => item.date === joinedRecord.participationDate)
      if (joinedIndex >= 0) {
        return joinedIndex
      }
    }
    const availableIndex = scheduleOptions.findIndex((item) => !item.full)
    return availableIndex >= 0 ? availableIndex : 0
  },

  async loadDetail() {
    if (!this.data.activityId) {
      return
    }
    this.setData({ loading: true })
    try {
      await getApp().ensureLogin()
      const [detail, userState] = await Promise.all([
        request({
          url: `/activities/${this.data.activityId}`
        }),
        fetchUserState()
      ])
      const merchantId = detail.merchant.id
      const joinedRecord = userState.activityMap[this.data.activityId] || null
      const scheduleOptions = decorateScheduleOptions(detail.scheduleOptions || [])
      const selectedScheduleIndex = this.resolveDefaultScheduleIndex(scheduleOptions, joinedRecord)

      this.setData({
        detail,
        joinedStatus: userState.joinedMap[this.data.activityId] || '',
        subscribed: Boolean(userState.subscriptionMap[merchantId]),
        distanceText: formatDistance(detail.merchant.distanceKm),
        merchantRatingText: formatRating(detail.merchant.avgScore, detail.merchant.ratingCount),
        scheduleOptions,
        joinedRecord
      })
      this.updateSelectedSchedule(selectedScheduleIndex)
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  onScheduleChange(e) {
    this.updateSelectedSchedule(Number(e.detail.value) || 0)
  },

  async handleJoinToggle() {
    try {
      await getApp().ensureLogin()
      if (this.data.joinedStatus === 'JOINED') {
        await request({
          url: `/activities/${this.data.activityId}/cancel-join`,
          method: 'POST'
        })
        wx.showToast({
          title: '已取消报名',
          icon: 'success'
        })
        await this.loadDetail()
        return
      }

      if (!this.data.selectedSchedule) {
        wx.showToast({
          title: '请选择参加日期',
          icon: 'none'
        })
        return
      }

      if (this.data.selectedSchedule.full) {
        wx.showToast({
          title: '当日活动名额已满，请选择其他日期',
          icon: 'none'
        })
        return
      }

      await request({
        url: `/activities/${this.data.activityId}/join`,
        method: 'POST',
        data: {
          participationDate: this.data.selectedSchedule.date
        }
      })
      wx.showToast({
        title: '报名成功',
        icon: 'success'
      })
      await this.loadDetail()
    } catch (error) {
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none'
      })
    }
  },

  async handleSubscribeToggle() {
    if (!this.data.detail) {
      return
    }
    const merchantId = this.data.detail.merchant.id
    try {
      await getApp().ensureLogin()
      await request({
        url: this.data.subscribed
          ? `/merchants/${merchantId}/unsubscribe`
          : `/merchants/${merchantId}/subscribe`,
        method: 'POST'
      })
      wx.showToast({
        title: this.data.subscribed ? '已取消订阅' : '订阅成功',
        icon: 'success'
      })
      await this.loadDetail()
    } catch (error) {
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none'
      })
    }
  }
})
