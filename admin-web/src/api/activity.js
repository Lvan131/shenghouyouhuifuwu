import request from './request'

export function getMerchantDashboard() {
  return request.get('/merchant/dashboard')
}

export function getMerchantActivities() {
  return request.get('/merchant/activities')
}

export function createMerchantActivity(payload) {
  return request.post('/merchant/activities', payload)
}

export function updateMerchantActivity(activityId, payload) {
  return request.put(`/merchant/activities/${activityId}`, payload)
}

export function offlineMerchantActivity(activityId) {
  return request.post(`/merchant/activities/${activityId}/offline`)
}

export function getPendingActivities() {
  return request.get('/admin/activities/pending')
}

export function getAuditActivities() {
  return request.get('/admin/activities')
}

export function getActivityDetail(activityId) {
  return request.get(`/activities/${activityId}`)
}

export function auditActivity(activityId, payload) {
  return request.post(`/admin/activities/${activityId}/audit`, payload)
}
