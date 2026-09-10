import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), warning: vi.fn(), success: vi.fn() }
}))
vi.mock('@/router', () => ({ default: { push: vi.fn() } }))

import request from '@/api/request'
import { ElMessage } from 'element-plus'
import router from '@/router'

describe('axios 请求封装', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('请求自动携带 Bearer token', async () => {
    let captured = null
    request.defaults.adapter = async (config) => {
      captured = config
      return {
        data: { code: 200, message: 'success', data: null },
        status: 200,
        statusText: 'OK',
        headers: {},
        config
      }
    }
    localStorage.setItem('token', 'abc')
    await request.get('/test')
    expect(captured.headers.Authorization).toBe('Bearer abc')
  })

  it('HTTP 401 清空 token 并跳转登录', async () => {
    request.defaults.adapter = async () => {
      const err = new Error('Unauthorized')
      err.response = { status: 401, data: { message: '未登录' } }
      throw err
    }
    localStorage.setItem('token', 'abc')
    await expect(request.get('/test')).rejects.toThrow()
    expect(localStorage.getItem('token')).toBeNull()
    expect(router.push).toHaveBeenCalledWith('/login')
    expect(ElMessage.warning).toHaveBeenCalled()
  })
})
