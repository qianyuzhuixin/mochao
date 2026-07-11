import axios from 'axios'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import { getToken, removeToken, removeUserInfo } from './auth'
import router from '@/router'

const service = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    NProgress.start()
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    NProgress.done()
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    NProgress.done()
    const res = response.data

    if (res.code === 200) {
      return res.data
    }

    if (res.code === 401) {
      Message.error('登录已过期，请重新登录')
      removeToken()
      removeUserInfo()
      router.push('/login')
      return Promise.reject(new Error(res.message || '未授权'))
    }

    Message.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  error => {
    NProgress.done()
    const status = error.response && error.response.status

    if (status === 401) {
      Message.error('登录已过期，请重新登录')
      removeToken()
      removeUserInfo()
      router.push('/login')
    } else if (status === 403) {
      Message.error('没有权限访问')
    } else if (status === 404) {
      Message.error('请求资源不存在')
    } else if (status >= 500) {
      Message.error('服务器错误，请稍后重试')
    } else if (error.message === 'Network Error') {
      Message.error('网络异常，请检查网络连接')
    } else if (error.code === 'ECONNABORTED') {
      Message.error('请求超时，请稍后重试')
    } else {
      Message.error(error.message || '请求失败')
    }

    return Promise.reject(error)
  }
)

export default service
