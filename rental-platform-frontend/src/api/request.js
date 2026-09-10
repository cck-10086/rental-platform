import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  response => {
    // 文件下载（blob）响应直接返回，不做业务码判断
    if (response.config.responseType === 'blob') return response
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      return Promise.reject(res)
    }
    return res
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      ElMessage.warning('登录已过期，请重新登录')
    } else if (error.response && error.response.status === 403) {
      ElMessage.warning('无权限访问')
    } else {
      ElMessage.error(error.response?.data?.message || '网络请求失败，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
