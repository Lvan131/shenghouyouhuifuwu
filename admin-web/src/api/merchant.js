import request from './request'

export function getCurrentMerchantInfo() {
  return request.get('/merchants/me')
}

export function updateCurrentMerchantInfo(payload) {
  return request.put('/merchants/me', payload)
}

export function getCurrentMerchantRatings() {
  return request.get('/merchants/me/ratings')
}
