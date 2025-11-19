import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { User, LoginRequest, AuthResponse } from '@/types'
import apiService from '@/services/api'

interface AuthState {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null

  login: (credentials: LoginRequest) => Promise<void>
  logout: () => void
  refreshToken: () => Promise<void>
  clearError: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,

      login: async (credentials: LoginRequest) => {
        set({ isLoading: true, error: null })
        try {
          const response = await apiService.post<AuthResponse>('/v1/auth/login', credentials)

          localStorage.setItem('token', response.token)
          localStorage.setItem('refreshToken', response.refreshToken)

          set({
            user: response.user,
            token: response.token,
            isAuthenticated: true,
            isLoading: false,
          })
        } catch (error: any) {
          set({
            error: error.response?.data?.message || 'Login failed',
            isLoading: false,
          })
          throw error
        }
      },

      logout: () => {
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        set({
          user: null,
          token: null,
          isAuthenticated: false,
        })
      },

      refreshToken: async () => {
        try {
          const refreshToken = localStorage.getItem('refreshToken')
          if (!refreshToken) {
            throw new Error('No refresh token available')
          }

          const response = await apiService.post<AuthResponse>('/v1/auth/refresh', {
            refreshToken,
          })

          localStorage.setItem('token', response.token)

          set({
            token: response.token,
          })
        } catch (error) {
          // Refresh failed, logout
          set({
            user: null,
            token: null,
            isAuthenticated: false,
          })
          localStorage.removeItem('token')
          localStorage.removeItem('refreshToken')
        }
      },

      clearError: () => set({ error: null }),
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)

export default useAuthStore
