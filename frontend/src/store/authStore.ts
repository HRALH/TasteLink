import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserInfo } from '../types/api'
import { AUTH_STORAGE_KEY } from '../utils/constants'

/**
 * 全局登录态（docs/03 §5.2）
 * - token/userInfo 持久化到 localStorage，刷新页面恢复
 * - 动作：login(token, userInfo)、logout()、updateProfile(partial)
 */
interface AuthState {
  token: string | null
  userInfo: UserInfo | null
  isLoggedIn: boolean
  login: (token: string, userInfo: UserInfo) => void
  logout: () => void
  updateProfile: (partial: Partial<UserInfo>) => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      userInfo: null,
      isLoggedIn: false,
      login: (token, userInfo) => set({ token, userInfo, isLoggedIn: true }),
      logout: () => set({ token: null, userInfo: null, isLoggedIn: false }),
      updateProfile: (partial) =>
        set((s) => (s.userInfo ? { userInfo: { ...s.userInfo, ...partial } } : s)),
    }),
    {
      name: AUTH_STORAGE_KEY,
      partialize: (s) => ({ token: s.token, userInfo: s.userInfo, isLoggedIn: s.isLoggedIn }),
    },
  ),
)
