const { request } = require('../../utils/request')

function createForm(profile = {}) {
  return {
    realName: profile.realName || '',
    userNo: profile.userNo || '',
    userType: profile.userType || 'STUDENT',
    phone: profile.phone || '',
    avatarUrl: profile.avatarUrl || '',
    password: '',
    confirmPassword: ''
  }
}

Page({
  data: {
    loading: false,
    form: createForm()
  },

  async onShow() {
    await this.loadProfile()
  },

  async loadProfile() {
    const app = getApp()
    if (!app.globalData.token) {
      wx.reLaunch({
        url: '/pages/login/index'
      })
      return
    }
    this.setData({ loading: true })
    try {
      const profileData = await request({
        url: '/users/me/profile'
      })
      this.setData({
        form: createForm(profileData.profile || {})
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

  onFieldInput(e) {
    const { field } = e.currentTarget.dataset
    this.setData({
      [`form.${field}`]: e.detail.value
    })
  },

  onTypeChange(e) {
    const value = Number(e.detail.value) === 1 ? 'TEACHER' : 'STUDENT'
    this.setData({
      'form.userType': value
    })
  },

  async submit() {
    const form = this.data.form
    if (!form.realName || !form.userNo || !form.phone || !form.password || !form.confirmPassword) {
      wx.showToast({
        title: '请填写完整资料',
        icon: 'none'
      })
      return
    }
    if (form.password !== form.confirmPassword) {
      wx.showToast({
        title: '两次输入的密码不一致',
        icon: 'none'
      })
      return
    }
    try {
      const profileData = await request({
        url: '/users/me/onboarding',
        method: 'PUT',
        data: {
          realName: form.realName,
          userNo: form.userNo,
          userType: form.userType,
          phone: form.phone,
          avatarUrl: form.avatarUrl,
          password: form.password
        }
      })
      const app = getApp()
      const userInfo = Object.assign({}, app.globalData.userInfo || {}, {
        displayName: profileData.displayName || form.realName,
        profileCompleted: true,
        passwordConfigured: true,
        needProfileCompletion: false
      })
      app.globalData.userInfo = userInfo
      wx.setStorageSync('userInfo', userInfo)
      wx.showToast({
        title: '资料已完善',
        icon: 'success'
      })
      wx.switchTab({
        url: '/pages/index/index'
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '提交失败',
        icon: 'none'
      })
    }
  }
})
