import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { useAuthStore } from '@/stores/auth'

const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const authStore = useAuthStore()
    // 如果请求已经显式指定了 Authorization（如 refreshToken），不要覆盖
    if (authStore.token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 统一处理 ApiResponse 格式
api.interceptors.response.use(
  (response: AxiosResponse) => {
    // 如果是 2xx 状态码，直接返回 data
    // 业务错误在 response.data 中通过 code 判断
    return response
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    // 刷新接口本身失败不重试，避免递归
    const isRefreshRequest = originalRequest.url?.includes('/auth/refresh-token')

    // 401/403 且未重试过 → 尝试刷新 Token
    if ((error.response?.status === 401 || error.response?.status === 403)
        && !originalRequest._retry && !isRefreshRequest) {
      originalRequest._retry = true

      const authStore = useAuthStore()
      try {
        const newToken = await authStore.refreshAccessToken()
        if (newToken && originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return api(originalRequest)
        }
      } catch {
        authStore.logout()
        window.location.href = '/auth/login'
      }
    }

    // 业务错误 (如验证码错误、用户名已存在等) - 从 response.data 中提取 message
    if (error.response?.status !== 401) {
      const serverMessage = (error.response?.data as any)?.message
      if (serverMessage) {
        error.message = serverMessage
      }
    }

    return Promise.reject(error)
  }
)

export interface UpdateRecordNameDTO {
  recordId: number
  name: string
}

// Interview APIs
export const getInterviewRecords = () => api.get<any>('/interview/records')
export const getQARecords = (recordId: number) => api.get<any>(`/interview/records/${recordId}`)
export const updateRecordName = (data: UpdateRecordNameDTO) => api.put('/interview/records/name', data)
export const deleteInterviewRecord = (recordId: number) => api.delete(`/interview/records/${recordId}`)

export default api
