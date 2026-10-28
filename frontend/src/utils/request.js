import axios from 'axios'
import router from '@/router'

const request = axios.create({
  timeout: 15000
})

// 请求拦截 - 自动带上 token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// 响应拦截 - 统一处理401，并把后端 msg 暴露给页面
request.interceptors.response.use(
  res => res.data,
  err => {
    const status = err.response?.status
    const message = err.response?.data?.msg || err.message || '请求失败'

    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      router.push('/login')
    }

    err.userMessage = message
    return Promise.reject(err)
  }
)

export default request