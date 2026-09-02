import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserInfo } from '../types/api'
import { AUTH_STORAGE_KEY } from '../utils/constants'
import { getRoleFromToken, type Role } from '../utils/jwt'

/**
 * 全局登录态（docs/03 §5.2）
 * - token / userInfo / role 持久化到 localStorage，刷新页面恢复
 * - role 由 JWT 解出（后端 LoginVO 未带 role，仅 JWT claim 携带）——仅供 UI 显隐 / 路由守卫，
 *   真正鉴权仍由后端 `/admin/**` 的 hasRole('ADMIN') 强制（非管理员得到 403）
 * - 动作：login(token, userInfo)、logout()、updateProfile(partial)、rehydrateRole()
 */
interface AuthState {
  token: string | null
  userInfo: UserInfo | null
  role: Role | null
  isLoggedIn: boolean
  login: (token: string, userInfo: UserInfo) => void
  logout: () => void
  updateProfile: (partial: Partial<UserInfo>) => void
  /** token 存在但 role 缺失时（如旧持久化态迁移）从 JWT 回填 */
  rehydrateRole: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      userInfo: null,
      role: null,
      isLoggedIn: false,
      login: (token, userInfo) =>
        set({ token, userInfo, role: getRoleFromToken(token) ?? null, isLoggedIn: true }),
      logout: () => set({ token: null, userInfo: null, role: null, isLoggedIn: false }),
      updateProfile: (partial) =>
        set((s) => (s.userInfo ? { userInfo: { ...s.userInfo, ...partial } } : s)),
      rehydrateRole: () => {
        const s = get()
        if (s.token && s.role === null) {
          set({ role: getRoleFromToken(s.token) ?? null })
        }
      },
    }),
    {
      name: AUTH_STORAGE_KEY,
      partialize: (s) => ({
        token: s.token,
        userInfo: s.userInfo,
        role: s.role,
        isLoggedIn: s.isLoggedIn,
      }),
      onRehydrateStorage: () => (state) => {
        if (state && state.token && state.role === null) state.rehydrateRole()
      },
    },
  ),
)

/** 派生选择器：当前登录人是否管理员 */
export const useIsAdmin = (): boolean => useAuthStore((s) => s.role === 'ADMIN')
