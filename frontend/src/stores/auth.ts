import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'
import api from '@/services/api'
import { useChatStore } from './chat'

// Backend response types
interface BackendProfile {
  userId: number
  username: string
  avatar?: string
  age?: number
  bio?: string
}

interface TokenResponse {
  token: string
  refreshToken: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const user = ref<User | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  const login = async (username: string, password: string) => {
    const response = await api.post<{ data: { token: string; refreshToken: string } }>('/auth/login', {
      username,
      password
    })

    const newToken = response.data.data.token
    const newRefreshToken = response.data.data.refreshToken

    token.value = newToken
    refreshToken.value = newRefreshToken

    localStorage.setItem('token', newToken)
    localStorage.setItem('refreshToken', newRefreshToken)

    // Store userId separately for easy access
    try {
      const payload = JSON.parse(atob(newToken.split('.')[1]))
      if (payload.userId) {
        localStorage.setItem('userId', String(payload.userId))
      }
    } catch (e) {
      console.error('Failed to parse token:', e)
    }

    await fetchCurrentUser()
    
    const chatStore = useChatStore()
    chatStore.connectWebSocket(newToken)

    return user.value
  }

  const register = async (username: string, email: string, password: string, code: string) => {
    await api.post('/auth/register', {
      username,
      email,
      password,
      code
    })
  }

  const loginByEmail = async (email: string, code: string) => {
    const response = await api.post<{ data: TokenResponse }>('/auth/login-by-email', {
      email,
      code
    })

    const newToken = response.data.data.token
    const newRefreshToken = response.data.data.refreshToken

    token.value = newToken
    refreshToken.value = newRefreshToken

    localStorage.setItem('token', newToken)
    localStorage.setItem('refreshToken', newRefreshToken)

    try {
      const payload = JSON.parse(atob(newToken.split('.')[1]))
      if (payload.userId) {
        localStorage.setItem('userId', String(payload.userId))
      }
    } catch (e) {
      console.error('Failed to parse token:', e)
    }

    await fetchCurrentUser()
    
    const chatStore = useChatStore()
    chatStore.connectWebSocket(newToken)
    
    return user.value
  }

  const forgotPassword = async (email: string, code: string, newPassword: string) => {
    await api.post('/auth/forgot-password', {
      email,
      code,
      newPassword
    })
  }

  const updateProfile = async (data: { username?: string; avatar?: string; age?: number; bio?: string }) => {
    await api.put('/auth/profile', data)
    // Re-fetch user profile to ensure data consistency
    await fetchCurrentUser()
  }

  const logout = async () => {
    const chatStore = useChatStore()
    try {
      await api.post('/auth/logout')
    } catch {
      // Ignore
    }

    chatStore.disconnectWebSocket()

    token.value = null
    refreshToken.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userId')
  }

  const refreshAccessToken = async (): Promise<string | null> => {
    if (!refreshToken.value) return null

    try {
      const response = await api.post<{ data: { token: string } }>(
        '/auth/refresh-token',
        {},
        {
          headers: {
            Authorization: `Bearer ${refreshToken.value}`
          }
        }
      )

      const newToken = response.data.data.token
      token.value = newToken
      localStorage.setItem('token', newToken)

      try {
        const payload = JSON.parse(atob(newToken.split('.')[1]))
        if (payload.userId) {
          localStorage.setItem('userId', String(payload.userId))
        }
      } catch (e) {
        console.error('Failed to parse token:', e)
      }

      return newToken
    } catch {
      logout()
      return null
    }
  }

  const fetchCurrentUser = async () => {
    if (!token.value) return null

    try {
      const response = await api.get<{ data: BackendProfile }>('/auth/profile')
      user.value = {
        id: response.data.data.userId,
        username: response.data.data.username,
        nickname: response.data.data.username,
        avatar: response.data.data.avatar || '',
        bio: response.data.data.bio || '',
        createdAt: new Date().toISOString()
      }
      if (user.value.id) {
        localStorage.setItem('userId', String(user.value.id))
      }
      return user.value
    } catch {
      return null
    }
  }

  const searchUsers = async (keyword: string, page = 1, size = 20) => {
    const response = await api.get<{ data: { list: any[]; total: number } }>('/auth/search-users', {
      params: { keyword, page, size }
    })
    return response.data.data
  }

  const sendVerifyCode = async (email: string, type: 'register' | 'login' | 'reset-password') => {
    await api.post('/auth/send-code', { email, type })
  }

  return {
    token,
    refreshToken,
    user,
    isLoggedIn,
    login,
    loginByEmail,
    register,
    logout,
    forgotPassword,
    refreshAccessToken,
    fetchCurrentUser,
    updateProfile,
    searchUsers,
    sendVerifyCode
  }
})
