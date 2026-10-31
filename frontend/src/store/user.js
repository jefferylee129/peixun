import { defineStore } from 'pinia'
import { login as loginApi, me as meApi } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: null,
    roles: [],
    perms: [],
    menus: [],
    loaded: false
  }),
  getters: {
    isAdmin: state => state.perms.includes('*')
  },
  actions: {
    hasPerm(code) {
      if (!code) return true
      return this.perms.includes('*') || this.perms.includes(code)
    },
    async login(form) {
      const res = await loginApi(form)
      this.token = res.data.token
      localStorage.setItem('token', res.data.token)
    },
    async fetchMe() {
      const res = await meApi()
      this.user = res.data.user
      this.roles = res.data.roles || []
      this.perms = res.data.perms || []
      this.menus = res.data.menus || []
      this.loaded = true
    },
    logout() {
      localStorage.removeItem('token')
      this.token = ''
      this.user = null
      this.roles = []
      this.perms = []
      this.menus = []
      this.loaded = false
    }
  }
})
