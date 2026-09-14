import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { authApi } from '../api/auth'
import type { UserInfo } from '../types/api'
import { AUTH_STORAGE_KEY } from '../utils/constants'
import { getRoleFromToken, isTokenExpired, type Role } from '../utils/jwt'

/**
 * 全局登录态（docs/03 §5.2）
 * - token / userInfo / isLoggedIn 持久化到 localStorage，刷新页面恢复
 * - role 由 JWT 解出（后端 LoginVO 未带 role，仅 JWT claim 携带），**不持久化**——
 *   每次加载由 rehydrateRole() 从 token 重新推导，避免冗余存储漂移。
 *   role 仅供 UI 显隐 / 路由守卫，真正鉴权仍由后端 `/admin/**` 的 hasRole('ADMIN') 强制（403）
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
      logout: () => {
        // 跨端契约接线：best-effort 通知后端清理 JWT 黑名单/会话态
        // 后端未上线或失败不阻塞前端清态（catch 吞掉，行为与未接线时一致）
        if (get().token) {
          void authApi.logout().catch(() => {})
        }
        set({ token: null, userInfo: null, role: null, isLoggedIn: false })
      },
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
        isLoggedIn: s.isLoggedIn,
      }),
      onRehydrateStorage: () => (state) => {
        if (!state) return
        // F5-2：持久化恢复时 token 已过期即清登录态，避免 UI 假登录态直到 401
        if (state.token && isTokenExpired(state.token)) {
          state.logout()
          return
        }
        if (state.token && state.role === null) state.rehydrateRole()
      },
    },
  ),
)

/** 派生选择器：当前登录人是否管理员 */
export const useIsAdmin = (): boolean => useAuthStore((s) => s.role === 'ADMIN')
