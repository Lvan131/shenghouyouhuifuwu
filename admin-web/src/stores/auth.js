import { defineStore } from 'pinia'
import { getCurrentUser, passwordLogin } from '../api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),
  getters: {
    role: (state) => state.userInfo?.role || '',
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    async login(payload) {
      const loginData = await passwordLogin(payload)
      this.token = loginData.token
      localStorage.setItem('token', loginData.token)
      await this.fetchCurrentUser()
    },
    async fetchCurrentUser() {
      const userInfo = await getCurrentUser()
      this.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
