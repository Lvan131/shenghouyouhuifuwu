const { request } = require('./request')

async function fetchMyActivities() {
  await getApp().ensureLogin()
  return request({
    url: '/users/me/activities'
  })
}

async function fetchMySubscriptions() {
  await getApp().ensureLogin()
  return request({
    url: '/merchants/subscriptions'
  })
}

async function fetchUserState() {
  const [activities, subscriptions] = await Promise.all([
    fetchMyActivities(),
    fetchMySubscriptions()
  ])

  const joinedMap = {}
  const activityMap = {}
  activities.forEach((item) => {
    joinedMap[item.activityId] = item.status
    activityMap[item.activityId] = item
  })

  const subscriptionMap = {}
  subscriptions.forEach((item) => {
    subscriptionMap[item.merchantId] = true
  })

  return {
    activities,
    subscriptions,
    joinedMap,
    activityMap,
    subscriptionMap
  }
}

module.exports = {
  fetchMyActivities,
  fetchMySubscriptions,
  fetchUserState
}
