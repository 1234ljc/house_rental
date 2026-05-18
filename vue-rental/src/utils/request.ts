import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { ApiResponse } from '@/types/user'

const request: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080/api',  // ✅ 关键修复
  timeout: 10000,
})

// 请求拦截器（保持不变）
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    if (!config.headers['Content-Type']) {
      config.headers['Content-Type'] = 'application/json'
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器（保持不变）
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 如果是blob类型的响应（文件下载），直接返回
    if (response.config.responseType === 'blob') {
      return response.data
    }
    
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    } else {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
  },
  (error) => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      ElMessage.error('登录已过期，请重新登录')
    } else if (error.response?.status === 404) {
      ElMessage.error('请求接口不存在')
    } else if (error.response?.status === 500) {
      ElMessage.error('服务器错误')
    } else {
      ElMessage.error(error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default request