import request from './request'

export function passwordLogin(payload) {
  return request.post('/auth/password-login', payload)
}

export function getCurrentUser() {
  return request.get('/auth/me')
}

