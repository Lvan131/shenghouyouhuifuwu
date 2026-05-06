const { request } = require('../../utils/request')

Page({
  data: {
    announcements: []
  },

  async onShow() {
    await this.loadAnnouncements()
  },

  async loadAnnouncements() {
    try {
      const announcements = await request({
        url: '/announcements'
      })
      this.setData({
        announcements
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
    }
  }
})
