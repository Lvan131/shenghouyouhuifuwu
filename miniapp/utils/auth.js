function saveSession(app, loginData, loginMode) {
  app.globalData.token = loginData.token
  app.globalData.userInfo = loginData
  app.globalData.loginMode = loginMode
  wx.setStorageSync('token', loginData.token)
  wx.setStorageSync('userInfo', loginData)
  wx.setStorageSync('loginMode', loginMode)
}

function clearSession(app) {
  app.globalData.token = ''
  app.globalData.userInfo = null
  app.globalData.loginMode = 'none'
  wx.removeStorageSync('token')
  wx.removeStorageSync('userInfo')
  wx.setStorageSync('loginMode', 'none')
}

function loginWithMockUser(app, mockOpenid = 'mock-user-001', nickname = '张三') {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.baseUrl}/auth/wechat-login`,
      method: 'POST',
      data: {
        mockOpenid,
        nickname
      },
      success(res) {
        const payload = res.data || {}
        if (payload.code !== 200) {
          reject(new Error(payload.message || '微信登录失败'))
          return
        }
        const loginData = payload.data
        saveSession(app, loginData, 'wechat')
        resolve(loginData)
      },
      fail(err) {
        reject(err)
      }
    })
  })
}

function loginWithPassword(app, username, password) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.baseUrl}/auth/password-login`,
      method: 'POST',
      data: {
        username,
        password
      },
      success(res) {
        const payload = res.data || {}
        if (payload.code !== 200) {
          reject(new Error(payload.message || '账号登录失败'))
          return
        }
        const loginData = payload.data
        if (loginData.role !== 'USER') {
          reject(new Error('小程序仅支持用户账号登录'))
          return
        }
        saveSession(app, loginData, 'password')
        resolve(loginData)
      },
      fail(err) {
        reject(err)
      }
    })
  })
}

function registerWithPassword(app, payload) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.baseUrl}/auth/register`,
      method: 'POST',
      data: payload,
      success(res) {
        const responsePayload = res.data || {}
        if (responsePayload.code !== 200) {
          reject(new Error(responsePayload.message || '注册失败'))
          return
        }
        const loginData = responsePayload.data
        if (loginData.role !== 'USER') {
          reject(new Error('注册结果异常，请稍后重试'))
          return
        }
        saveSession(app, loginData, 'password')
        resolve(loginData)
      },
      fail(err) {
        reject(err)
      }
    })
  })
}

function needsProfileCompletion(app) {
  const userInfo = app.globalData.userInfo || {}
  return !!(app.globalData.token && userInfo.needProfileCompletion)
}

module.exports = {
  clearSession,
  loginWithMockUser,
  loginWithPassword,
  needsProfileCompletion,
  registerWithPassword,
  saveSession
}
