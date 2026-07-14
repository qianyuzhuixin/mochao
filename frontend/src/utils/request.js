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

    // Blob 响应（如文件导出）直接返回，不走 JSON 解析
    if (response.config.responseType === 'blob') {
      return response.data
    }

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
      const isAdminPath = router.currentRoute.path.startsWith('/admin')
      Message.error(isAdminPath ? '没有管理员权限' : '没有权限访问')
      if (isAdminPath) {
        router.push('/')
      }
    } else if (status === 429) {
      // 限流提示 — 优先取后端返回的中文消息，fallback 带等待秒数
      const backendMsg = error.response.data && error.response.data.message
      const retryAfter = error.response.headers['retry-after']
      if (backendMsg) {
        Message.warning(backendMsg)
      } else if (retryAfter) {
        Message.warning(`操作过于频繁，请等待 ${retryAfter} 秒后再试`)
      } else {
        Message.warning('操作过于频繁，请稍后再试')
      }
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
