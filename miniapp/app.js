let localConfig = {}

try {
  localConfig = require('./config')
} catch (error) {
  localConfig = {}
}

App({
  globalData: {
    baseUrl: localConfig.baseUrl || 'http://127.0.0.1:8080/api',
    token: '',
    userInfo: null,
    loginMode: 'none'
  },

  onLaunch() {
    this.globalData.token = wx.getStorageSync('token') || ''
    this.globalData.userInfo = wx.getStorageSync('userInfo') || null
    this.globalData.loginMode = wx.getStorageSync('loginMode') || 'none'
  },

  ensureLogin() {
    if (this.globalData.token) {
      return Promise.resolve(this.globalData.userInfo)
    }
    return Promise.reject(new Error('请先登录'))
  }
})
