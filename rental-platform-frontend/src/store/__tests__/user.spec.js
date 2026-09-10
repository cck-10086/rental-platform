import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/api/user', () => ({
  getUserInfo: vi.fn()
}))

import { useUserStore } from '@/store/user'
import { getUserInfo } from '@/api/user'

describe('用户状态 store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('setToken 写入 localStorage', () => {
    const store = useUserStore()
    store.setToken('token-1')
    expect(store.token).toBe('token-1')
    expect(localStorage.getItem('token')).toBe('token-1')
  })

  it('setUser 保存用户信息', () => {
    const store = useUserStore()
    store.setUser({ username: 'admin', role: 'admin' })
    expect(store.user.username).toBe('admin')
    expect(store.user.role).toBe('admin')
  })

  it('logout 清空登录态', () => {
    const store = useUserStore()
    store.setToken('token-1')
    store.setUser({ username: 'admin' })
    store.logout()
    expect(store.token).toBe('')
    expect(store.user).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('fetchUser 成功时保存用户', async () => {
    getUserInfo.mockResolvedValue({ code: 200, data: { username: 'probe', role: 'user' } })
    const store = useUserStore()
    store.setToken('token-1')
    await store.fetchUser()
    expect(store.user.username).toBe('probe')
  })

  it('fetchUser 失败时登出', async () => {
    getUserInfo.mockRejectedValue(new Error('fail'))
    const store = useUserStore()
    store.setToken('token-1')
    store.setUser({ username: 'x' })
    await store.fetchUser()
    expect(store.user).toBeNull()
  })
})
