import { ref } from 'vue'
import type { AxiosRequestConfig, AxiosResponse } from 'axios'
import api from '@/services/api'

export function useAxios<T = any>(url: string, options?: AxiosRequestConfig) {
  const data = ref<T | null>(null)
  const loading = ref(false)
  const error = ref<Error | null>(null)

  const execute = async (overrideOptions?: AxiosRequestConfig) => {
    loading.value = true
    error.value = null

    try {
      const response: AxiosResponse<T> = await api(url, {
        ...options,
        ...overrideOptions
      })
      data.value = response.data
      return response.data
    } catch (e) {
      error.value = e as Error
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    data,
    loading,
    error,
    execute
  }
}
