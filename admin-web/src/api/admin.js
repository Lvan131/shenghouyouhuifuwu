import request from './request'

export function getAdminUsers() {
  return request.get('/admin/users')
}

export function createAdminUser(data) {
  return request.post('/admin/users', data)
}

export function updateAdminUser(accountId, data) {
  return request.put(`/admin/users/${accountId}`, data)
}

export function deleteAdminUser(accountId) {
  return request.delete(`/admin/users/${accountId}`)
}

export function getAdminMerchants() {
  return request.get('/admin/merchants')
}

export function createAdminMerchant(data) {
  return request.post('/admin/merchants', data)
}

export function updateAdminMerchant(merchantId, data) {
  return request.put(`/admin/merchants/${merchantId}`, data)
}

export function deleteAdminMerchant(merchantId) {
  return request.delete(`/admin/merchants/${merchantId}`)
}
