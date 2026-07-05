import axios from 'axios'
import { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token && !config.skipAuth) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

service.interceptors.response.use(
  (response) => {
    const { data, config } = response
    const code = parseInt(data?.code) || 0
    if (code === 200) {
      return data
    }

    if (code === -1) {
      if (!config.url?.includes('/login')) {
        ElMessage.error(data.msg || '登录过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('privacyAccepted')
        window.location.href = '/auth/login'
        return Promise.reject(new Error(data.msg || '登录过期'))
      }

      ElMessage.error(data.msg || '登录失败')
      return Promise.reject(new Error(data.msg || '登录失败'))
    }

    return Promise.reject(new Error(data ? (data.msg || '请求失败') : '响应数据为空'))
  },
  (error) => {
    const message = error.response?.data?.msg || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default service
