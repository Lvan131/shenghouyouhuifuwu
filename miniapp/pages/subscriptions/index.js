const { request } = require('../../utils/request')

Page({
  data: {
    merchants: []
  },

  async onShow() {
    await this.loadSubscriptions()
  },

  async loadSubscriptions() {
    try {
      await getApp().ensureLogin()
      const merchants = await request({
        url: '/merchants/subscriptions'
      })
      this.setData({
        merchants
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
    }
  },

  async cancelSubscription(e) {
    const merchantId = e.currentTarget.dataset.merchantId
    try {
      await getApp().ensureLogin()
      await request({
        url: `/merchants/${merchantId}/unsubscribe`,
        method: 'POST'
      })
      wx.showToast({
        title: '已取消订阅',
        icon: 'success'
      })
      await this.loadSubscriptions()
    } catch (error) {
      wx.showToast({
        title: error.message || '取消失败',
        icon: 'none'
      })
    }
  }
})
