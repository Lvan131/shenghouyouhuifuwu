function request({ url, method = 'GET', data = {}, header = {} }) {
  const app = getApp()
  const token = app.globalData.token

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.baseUrl}${url}`,
      method,
      data,
      header: {
        Authorization: token ? `Bearer ${token}` : '',
        ...header
      },
      success(res) {
        const payload = res.data || {}
        if (payload.code === 200) {
          resolve(payload.data)
          return
        }
        reject(new Error(payload.message || 'Request failed'))
      },
      fail(err) {
        const message = err && err.errMsg && err.errMsg.includes('fail')
          ? 'Backend service is unavailable. Start backend on port 8080 first.'
          : 'Network request failed. Please try again.'
        reject(new Error(message))
      }
    })
  })
}

module.exports = {
  request
}
