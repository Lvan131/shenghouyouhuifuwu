const { request } = require('../../utils/request')
const { formatRating } = require('../../utils/display')

function buildStars(selectedScore) {
  return [1, 2, 3, 4, 5].map((score) => ({
    score,
    active: score <= selectedScore
  }))
}

Page({
  data: {
    merchantId: '',
    merchantName: '',
    activityId: '',
    activityTitle: '',
    score: 5,
    content: '',
    stars: buildStars(5),
    canRate: false,
    rated: false,
    reason: '',
    ratingText: '暂无评价',
    submitting: false
  },

  async onLoad(options) {
    this.setData({
      merchantId: options.merchantId || '',
      merchantName: options.merchantName ? decodeURIComponent(options.merchantName) : '',
      activityId: options.activityId || '',
      activityTitle: options.title ? decodeURIComponent(options.title) : ''
    })
    await this.loadRatingInfo()
  },

  async loadRatingInfo() {
    try {
      await getApp().ensureLogin()
      const result = await request({
        url: `/merchants/${this.data.merchantId}/rating/me`,
        data: {
          activityId: this.data.activityId
        }
      })
      const score = Number(result.score) || 5
      this.setData({
        merchantName: result.merchantName || this.data.merchantName,
        activityId: result.activityId || this.data.activityId,
        activityTitle: result.activityTitle || this.data.activityTitle,
        score,
        content: result.content || '',
        stars: buildStars(score),
        canRate: Boolean(result.canRate),
        rated: Boolean(result.rated),
        reason: result.reason || '',
        ratingText: formatRating(result.avgScore, result.ratingCount)
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
    }
  },

  selectScore(e) {
    const score = Number(e.currentTarget.dataset.score) || 5
    this.setData({
      score,
      stars: buildStars(score)
    })
  },

  onContentInput(e) {
    this.setData({
      content: e.detail.value
    })
  },

  async submitRating() {
    if (!this.data.canRate || this.data.submitting) {
      return
    }
    try {
      this.setData({ submitting: true })
      await request({
        url: `/merchants/${this.data.merchantId}/ratings`,
        method: 'POST',
        data: {
          activityId: Number(this.data.activityId),
          score: this.data.score,
          content: this.data.content.trim()
        }
      })
      wx.showToast({
        title: this.data.rated ? '评价已更新' : '评价成功',
        icon: 'success'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 500)
    } catch (error) {
      wx.showToast({
        title: error.message || '提交失败',
        icon: 'none'
      })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
