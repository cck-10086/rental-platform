import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import Login from '@/views/Login.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() })
}))
vi.mock('@/api/user', () => ({ login: vi.fn() }))
vi.mock('@/store/user', () => ({
  useUserStore: () => ({ setToken: vi.fn(), setUser: vi.fn() })
}))

describe('Login.vue 登录页', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('渲染平台标题与登录表单', () => {
    const wrapper = mount(Login, { global: { plugins: [ElementPlus] } })
    expect(wrapper.text()).toContain('智能租房合同审查平台')
    expect(wrapper.find('input[placeholder="用户名"]').exists()).toBe(true)
    expect(wrapper.find('input[placeholder="密码"]').exists()).toBe(true)
  })
})
