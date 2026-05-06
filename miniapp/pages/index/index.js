const { request } = require('../../utils/request')
const { fetchUserState } = require('../../utils/user-state')
const { activitySummary, formatDistance } = require('../../utils/display')

const pageText = {
  heroTitle: '\u6821\u56ed\u5468\u8fb9\u4f18\u60e0\uff0c\u4e00\u7ad9\u83b7\u53d6',
  heroSubtitle: '\u641c\u7d22\u5546\u5bb6\u548c\u6d3b\u52a8\uff0c\u6309\u5546\u6237\u5206\u7c7b\u5feb\u901f\u7b5b\u9009\u3002',
  systemAnnouncements: '\u7cfb\u7edf\u516c\u544a',
  noAnnouncements: '\u6682\u65e0\u7cfb\u7edf\u516c\u544a',
  searchPlaceholder: '\u641c\u7d22\u5546\u5bb6\u6216\u6d3b\u52a8',
  search: '\u641c\u7d22',
  sortLabel: '\u6392\u5e8f\u65b9\u5f0f',
  activityList: '\u6d3b\u52a8\u5217\u8868',
  cancelJoin: '\u53d6\u6d88\u62a5\u540d',
  joinActivity: '\u62a5\u540d\u6d3b\u52a8',
  quotaLabel: '\u540d\u989d',
  unlimitedQuota: '\u4e0d\u9650',
  joinedLabel: '\u5df2\u62a5\u540d',
  ratingInfoLabel: '\u8bc4\u5206',
  timeLabel: '\u65f6\u95f4',
  emptyActivities: '\u5f53\u524d\u5206\u7c7b\u4e0b\u6682\u65e0\u6d3b\u52a8',
  loadFailed: '\u52a0\u8f7d\u5931\u8d25',
  actionFailed: '\u64cd\u4f5c\u5931\u8d25',
  cancelSuccess: '\u5df2\u53d6\u6d88\u62a5\u540d',
  noRating: '\u6682\u65e0\u8bc4\u4ef7'
}

const categoryOptions = [
  '\u5168\u90e8',
  '\u9910\u996e',
  '\u96f6\u552e',
  '\u751f\u6d3b\u670d\u52a1'
]

const sortOptions = [
  { label: '\u6309\u8ddd\u79bb\u6392\u5e8f', value: 'distance' },
  { label: '\u6309\u70ed\u5ea6\u6392\u5e8f', value: 'popularity' },
  { label: '\u6309\u8bc4\u5206\u6392\u5e8f', value: 'rating' }
]

function normalizeNumber(value, fallback = 0) {
  const result = Number(value)
  return Number.isFinite(result) ? result : fallback
}

function distanceValue(value) {
  const distance = Number(value)
  return Number.isFinite(distance) && distance >= 0 ? distance : Number.MAX_SAFE_INTEGER
}

function compareNumbers(left, right, order = 'asc') {
  if (left === right) {
    return 0
  }
  return order === 'desc' ? right - left : left - right
}

function formatRatingText(avgScore, ratingCount) {
  const count = normalizeNumber(ratingCount)
  const score = Number(avgScore)
  if (count <= 0 || !Number.isFinite(score)) {
    return pageText.noRating
  }
  return `${score.toFixed(1)}\u5206 \u00b7 ${count}\u6761\u8bc4\u4ef7`
}

function decorateActivities(activities, joinedMap) {
  return activities.map((item, index) => ({
    ...item,
    originalIndex: index,
    joinedCountValue: normalizeNumber(item.joinedCount),
    joinedStatus: joinedMap[item.activityId] || '',
    distanceText: formatDistance(item.distanceKm),
    ratingText: formatRatingText(item.avgScore, item.ratingCount),
    summaryText: activitySummary(item)
  }))
}

function sortActivities(activities, sortType) {
  return activities.slice().sort((left, right) => {
    if (sortType === 'popularity') {
      const joinedCompare = compareNumbers(left.joinedCountValue, right.joinedCountValue, 'desc')
      if (joinedCompare !== 0) {
        return joinedCompare
      }
    } else if (sortType === 'rating') {
      const scoreCompare = compareNumbers(
        normalizeNumber(left.avgScore),
        normalizeNumber(right.avgScore),
        'desc'
      )
      if (scoreCompare !== 0) {
        return scoreCompare
      }

      const ratingCountCompare = compareNumbers(
        normalizeNumber(left.ratingCount),
        normalizeNumber(right.ratingCount),
        'desc'
      )
      if (ratingCountCompare !== 0) {
        return ratingCountCompare
      }
    }

    const distanceCompare = compareNumbers(
      distanceValue(left.distanceKm),
      distanceValue(right.distanceKm)
    )
    if (distanceCompare !== 0) {
      return distanceCompare
    }

    return compareNumbers(left.originalIndex, right.originalIndex)
  })
}

Page({
  data: {
    pageText,
    keyword: '',
    currentCategory: categoryOptions[0],
    categoryOptions,
    sortOptions,
    sortIndex: 0,
    currentSort: sortOptions[0].value,
    currentSortLabel: sortOptions[0].label,
    loading: false,
    rawActivities: [],
    activities: [],
    announcements: [],
    currentAnnouncementIndex: 0,
    joinedMap: {},
    inputFocused: false
  },

  async onLoad() {
    await this.loadData()
  },

  async onShow() {
    if (this.data.activities.length || this.data.announcements.length) {
      await this.loadData()
    }
  },

  onUnload() {
    this.clearAnnouncementRotation()
  },

  onHide() {
    this.clearAnnouncementRotation()
  },

  async onPullDownRefresh() {
    await this.loadData()
    wx.stopPullDownRefresh()
  },

  onSearchInput(e) {
    this.setData({
      keyword: e.detail.value
    })
  },

  onInputFocus() {
    this.setData({
      inputFocused: true
    })
  },

  onInputBlur() {
    this.setData({
      inputFocused: false
    })
  },

  clearKeyword() {
    this.setData({
      keyword: '',
      inputFocused: true
    })
  },

  async onSearchConfirm() {
    await this.loadData()
  },

  async onCategoryTap(e) {
    this.setData({
      currentCategory: e.currentTarget.dataset.category
    })
    await this.loadData()
  },

  onSortChange(e) {
    const sortIndex = Number(e.detail.value) || 0
    const currentSortOption = sortOptions[sortIndex] || sortOptions[0]
    this.setData({
      sortIndex,
      currentSort: currentSortOption.value,
      currentSortLabel: currentSortOption.label,
      activities: sortActivities(this.data.rawActivities, currentSortOption.value)
    })
  },

  startAnnouncementRotation() {
    this.clearAnnouncementRotation()
    this.noticeTimer = setInterval(() => {
      const announcements = this.data.announcements || []
      if (announcements.length <= 1) {
        return
      }
      this.setData({
        currentAnnouncementIndex: (this.data.currentAnnouncementIndex + 1) % announcements.length
      })
    }, 10000)
  },

  clearAnnouncementRotation() {
    if (this.noticeTimer) {
      clearInterval(this.noticeTimer)
      this.noticeTimer = null
    }
  },

  async loadData() {
    const app = getApp()
    this.setData({ loading: true })
    try {
      await app.ensureLogin()
      const merchantType = this.data.currentCategory === categoryOptions[0] ? '' : this.data.currentCategory
      const [activities, announcements, userState] = await Promise.all([
        request({
          url: '/activities',
          data: {
            keyword: this.data.keyword.trim(),
            merchantType
          }
        }),
        request({ url: '/announcements' }),
        fetchUserState()
      ])

      const decoratedActivities = decorateActivities(activities, userState.joinedMap)

      this.setData({
        rawActivities: decoratedActivities,
        activities: sortActivities(decoratedActivities, this.data.currentSort),
        announcements,
        currentAnnouncementIndex: 0,
        joinedMap: userState.joinedMap
      })
      this.startAnnouncementRotation()
    } catch (error) {
      wx.showToast({
        title: error.message || pageText.loadFailed,
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  goDetail(e) {
    wx.navigateTo({
      url: `/pages/activity-detail/index?id=${e.currentTarget.dataset.id}`
    })
  },

  async handleJoinToggle(e) {
    const activityId = e.currentTarget.dataset.activityId
    const joined = e.currentTarget.dataset.joined === 'true'
    try {
      await getApp().ensureLogin()
      if (!joined) {
        wx.navigateTo({
          url: `/pages/activity-detail/index?id=${activityId}`
        })
        return
      }
      await request({
        url: `/activities/${activityId}/cancel-join`,
        method: 'POST'
      })
      wx.showToast({
        title: pageText.cancelSuccess,
        icon: 'success'
      })
      await this.loadData()
    } catch (error) {
      wx.showToast({
        title: error.message || pageText.actionFailed,
        icon: 'none'
      })
    }
  }
})
