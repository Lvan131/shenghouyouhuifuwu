const {
  loginWithMockUser,
  loginWithPassword,
  needsProfileCompletion,
  registerWithPassword
} = require('../../utils/auth')

function createLoginForm() {
  return {
    username: '',
    password: ''
  }
}

function createRegisterForm() {
  return {
    realName: '',
    userNo: '',
    userType: 'STUDENT',
    phone: '',
    password: '',
    confirmPassword: ''
  }
}

Page({
  data: {
    activeMode: 'wechat',
    loginForm: createLoginForm(),
    registerForm: createRegisterForm()
  },

  onShow() {
    const app = getApp()
    if (!app.globalData.token) {
      return
    }
    if (needsProfileCompletion(app)) {
      wx.reLaunch({
        url: '/pages/profile-setup/index'
      })
      return
    }
    wx.switchTab({
      url: '/pages/index/index'
    })
  },

  switchMode(e) {
    this.setData({
      activeMode: e.currentTarget.dataset.mode
    })
  },

  onLoginInput(e) {
    const { field } = e.currentTarget.dataset
    this.setData({
      [`loginForm.${field}`]: e.detail.value
    })
  },

  onRegisterInput(e) {
    const { field } = e.currentTarget.dataset
    this.setData({
      [`registerForm.${field}`]: e.detail.value
    })
  },

  onRegisterTypeChange(e) {
    const value = Number(e.detail.value) === 1 ? 'TEACHER' : 'STUDENT'
    this.setData({
      'registerForm.userType': value
    })
  },

  navigateAfterLogin() {
    const app = getApp()
    if (needsProfileCompletion(app)) {
      wx.reLaunch({
        url: '/pages/profile-setup/index'
      })
      return
    }
    wx.switchTab({
      url: '/pages/index/index'
    })
  },

  async handleWechatLogin() {
    try {
      await loginWithMockUser(getApp())
      wx.showToast({
        title: '登录成功',
        icon: 'success'
      })
      this.navigateAfterLogin()
    } catch (error) {
      wx.showToast({
        title: error.message || '登录失败',
        icon: 'none'
      })
    }
  },

  async handlePasswordLogin() {
    const { username, password } = this.data.loginForm
    if (!username || !password) {
      wx.showToast({
        title: '请输入账号和密码',
        icon: 'none'
      })
      return
    }
    try {
      await loginWithPassword(getApp(), username, password)
      wx.showToast({
        title: '登录成功',
        icon: 'success'
      })
      this.navigateAfterLogin()
    } catch (error) {
      wx.showToast({
        title: error.message || '登录失败',
        icon: 'none'
      })
    }
  },

  async handleRegister() {
    const form = this.data.registerForm
    if (!form.realName || !form.userNo || !form.phone || !form.password || !form.confirmPassword) {
      wx.showToast({
        title: '请填写完整注册信息',
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
      await registerWithPassword(getApp(), {
        realName: form.realName,
        userNo: form.userNo,
        userType: form.userType,
        phone: form.phone,
        password: form.password
      })
      wx.showToast({
        title: '注册成功',
        icon: 'success'
      })
      this.setData({
        loginForm: {
          username: form.userNo,
          password: form.password
        },
        registerForm: createRegisterForm()
      })
      this.navigateAfterLogin()
    } catch (error) {
      wx.showToast({
        title: error.message || '注册失败',
        icon: 'none'
      })
    }
  }
})
