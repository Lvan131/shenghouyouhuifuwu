const { request } = require('../../utils/request')

Page({
  data: {
    offers: []
  },

  async onShow() {
    await this.loadOffers()
  },

  async loadOffers() {
    try {
      await getApp().ensureLogin()
      const offers = await request({
        url: '/users/me/activities'
      })
      this.setData({
        offers
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

  goRate(e) {
    const { merchantId, merchantName, activityId, title } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/merchant-rating/index?merchantId=${merchantId}&merchantName=${encodeURIComponent(merchantName || '')}&activityId=${activityId}&title=${encodeURIComponent(title || '')}`
    })
  },

  async cancelJoin(e) {
    const activityId = e.currentTarget.dataset.activityId
    try {
      await request({
        url: `/activities/${activityId}/cancel-join`,
        method: 'POST'
      })
      wx.showToast({
        title: '已取消报名',
        icon: 'success'
      })
      await this.loadOffers()
    } catch (error) {
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none'
      })
    }
  }
})
