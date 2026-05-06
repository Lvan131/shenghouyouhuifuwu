const { request } = require('../../utils/request')
const { fetchMySubscriptions } = require('../../utils/user-state')
const { formatDistance, formatRating } = require('../../utils/display')

const categoryOptions = ['全部', '餐饮', '零售', '生活服务']

function decorateMerchants(merchants, subscriptionMap) {
  return merchants.map((item) => ({
    ...item,
    subscribed: Boolean(subscriptionMap[item.merchantId]),
    distanceText: formatDistance(item.distanceKm),
    ratingText: formatRating(item.avgScore, item.ratingCount)
  }))
}

Page({
  data: {
    keyword: '',
    currentCategory: '全部',
    categoryOptions,
    merchants: [],
    subscriptionMap: {}
  },

  async onLoad() {
    await this.loadMerchants()
  },

  async onShow() {
    if (this.data.merchants.length) {
      await this.loadMerchants()
    }
  },

  onSearchInput(e) {
    this.setData({
      keyword: e.detail.value
    })
  },

  async onSearchConfirm() {
    await this.loadMerchants()
  },

  async onCategoryTap(e) {
    this.setData({
      currentCategory: e.currentTarget.dataset.category
    })
    await this.loadMerchants()
  },

  async onPullDownRefresh() {
    await this.loadMerchants()
    wx.stopPullDownRefresh()
  },

  async loadMerchants() {
    try {
      await getApp().ensureLogin()
      const merchantType = this.data.currentCategory === '全部' ? '' : this.data.currentCategory
      const [merchants, subscriptions] = await Promise.all([
        request({
          url: '/merchants/nearby',
          data: {
            keyword: this.data.keyword.trim(),
            merchantType
          }
        }),
        fetchMySubscriptions()
      ])
      const subscriptionMap = {}
      subscriptions.forEach((item) => {
        subscriptionMap[item.merchantId] = true
      })
      this.setData({
        merchants: decorateMerchants(merchants, subscriptionMap),
        subscriptionMap
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
    }
  },

  goMerchantActivities(e) {
    const { merchantId, merchantName, avgScore, ratingCount } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/merchant-activity-list/index?merchantId=${merchantId}&merchantName=${encodeURIComponent(merchantName)}&avgScore=${avgScore || 0}&ratingCount=${ratingCount || 0}`
    })
  },

  async subscribeMerchant(e) {
    const merchantId = e.currentTarget.dataset.merchantId
    const subscribed = e.currentTarget.dataset.subscribed === 'true'
    try {
      await request({
        url: subscribed ? `/merchants/${merchantId}/unsubscribe` : `/merchants/${merchantId}/subscribe`,
        method: 'POST'
      })
      wx.showToast({
        title: subscribed ? '已取消订阅' : '订阅成功',
        icon: 'success'
      })
      await this.loadMerchants()
    } catch (error) {
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none'
      })
    }
  }
})
