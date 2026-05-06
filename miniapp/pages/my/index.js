const { request } = require('../../utils/request')
const { fetchMyActivities, fetchMySubscriptions } = require('../../utils/user-state')
const { clearSession } = require('../../utils/auth')

function createProfileForm(profile = {}) {
  return {
    realName: profile.realName || '',
    userNo: profile.userNo || '',
    userType: profile.userType || 'STUDENT',
    phone: profile.phone || '',
    avatarUrl: profile.avatarUrl || ''
  }
}

Page({
  data: {
    guestMode: false,
    profile: null,
    offersPreview: [],
    subscriptionsPreview: [],
    editVisible: false,
    passwordVisible: false,
    form: createProfileForm(),
    passwordForm: {
      oldPassword: '',
      newPassword: ''
    }
  },

  async onShow() {
    await this.loadPageData()
  },

  async loadPageData() {
    const app = getApp()
    if (!app.globalData.token && app.globalData.loginMode === 'none') {
      this.setData({
        guestMode: true,
        profile: null,
        offersPreview: [],
        subscriptionsPreview: []
      })
      return
    }

    try {
      await app.ensureLogin()
      const [profileData, offers, subscriptions] = await Promise.all([
        request({
          url: '/users/me/profile'
        }),
        fetchMyActivities(),
        fetchMySubscriptions()
      ])
      const profile = profileData.profile
      this.setData({
        guestMode: false,
        profile,
        offersPreview: offers.slice(0, 2),
        subscriptionsPreview: subscriptions.slice(0, 2),
        form: createProfileForm(profile)
      })
    } catch (error) {
      this.setData({ guestMode: true })
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
    }
  },

  openEdit() {
    this.setData({
      editVisible: true
    })
  },

  closeEdit() {
    this.setData({
      editVisible: false
    })
  },

  openPasswordDialog() {
    this.setData({
      passwordVisible: true,
      passwordForm: {
        oldPassword: '',
        newPassword: ''
      }
    })
  },

  closePasswordDialog() {
    this.setData({
      passwordVisible: false
    })
  },

  onFieldInput(e) {
    const { field } = e.currentTarget.dataset
    this.setData({
      [`form.${field}`]: e.detail.value
    })
  },

  onPasswordInput(e) {
    const { field } = e.currentTarget.dataset
    this.setData({
      [`passwordForm.${field}`]: e.detail.value
    })
  },

  onTypeChange(e) {
    const value = Number(e.detail.value) === 1 ? 'TEACHER' : 'STUDENT'
    this.setData({
      'form.userType': value
    })
  },

  async saveProfile() {
    if (!this.data.form.userNo) {
      wx.showToast({
        title: '请填写学工号',
        icon: 'none'
      })
      return
    }
    try {
      await request({
        url: '/users/me/profile',
        method: 'PUT',
        data: this.data.form
      })
      wx.showToast({
        title: '保存成功',
        icon: 'success'
      })
      this.setData({ editVisible: false })
      await this.loadPageData()
    } catch (error) {
      wx.showToast({
        title: error.message || '保存失败',
        icon: 'none'
      })
    }
  },

  async savePassword() {
    if (!this.data.passwordForm.oldPassword || !this.data.passwordForm.newPassword) {
      wx.showToast({
        title: '请填写完整密码信息',
        icon: 'none'
      })
      return
    }
    try {
      await request({
        url: '/users/me/password',
        method: 'PUT',
        data: this.data.passwordForm
      })
      wx.showToast({
        title: '密码修改成功',
        icon: 'success'
      })
      this.setData({
        passwordVisible: false,
        passwordForm: {
          oldPassword: '',
          newPassword: ''
        }
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '修改失败',
        icon: 'none'
      })
    }
  },

  logout() {
    clearSession(getApp())
    this.setData({
      guestMode: true,
      profile: null,
      offersPreview: [],
      subscriptionsPreview: []
    })
    wx.showToast({
      title: '已退出登录',
      icon: 'success'
    })
    wx.reLaunch({
      url: '/pages/login/index'
    })
  },

  goOffers() {
    wx.navigateTo({
      url: '/pages/my-offers/index'
    })
  },

  goSubscriptions() {
    wx.navigateTo({
      url: '/pages/subscriptions/index'
    })
  }
})
