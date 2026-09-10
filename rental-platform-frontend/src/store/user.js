import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')

  const setToken = (t) => { token.value = t; localStorage.setItem('token', t) }
  const setUser = (u) => { user.value = u }
  const logout = () => { token.value = ''; user.value = null; localStorage.removeItem('token') }

  const fetchUser = async () => {
    if (!token.value) return
    try {
      const res = await getUserInfo()
      user.value = res.data
    } catch { logout() }
  }

  return { user, token, setToken, setUser, logout, fetchUser }
})
