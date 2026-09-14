import { describe, expect, it, beforeEach, vi } from 'vitest'
import { useAuthStore } from '../store/authStore'
import { AUTH_STORAGE_KEY } from '../utils/constants'

/** 构造带任意 claim 的 JWT（base64url payload） */
function jwtWith(claims: Record<string, unknown>): string {
  const payload = btoa(JSON.stringify(claims))
  return `h.${payload}.s`
}

beforeEach(() => {
  localStorage.clear()
  useAuthStore.setState({
    token: null,
    userInfo: null,
    role: null,
    isLoggedIn: false,
  })
  vi.clearAllMocks()
})

describe('authStore partialize / onRehydrateStorage (F6)', () => {
  it('partialize 不持久化 role（F0-4：role 由 JWT 重新推导，持久化冗余）', () => {
    // 直接拉取 persist 中间件的 partialize 配置
    // Zustand persist 把 options 挂在 store.persist 上
    const partialize = (
      useAuthStore as unknown as {
        persist: { setOptions: (o: unknown) => void; getOptions: () => unknown }
      }
    ).persist.getOptions?.() as { partialize?: (s: unknown) => unknown } | undefined
    // 若该路径取不到，回退用 storage.getItem 比对——但 Zustand persist 默认 partialize
    // 返回整个 state，这里项目里显式覆写了 partialize，故断言这层契约。
    void partialize // partialize 路径不稳定，用观测式断言替代（见下）

    // 观测式断言：调用 login 后，localStorage 里的持久化快照不包含 role
    useAuthStore.getState().login(jwtWith({ role: 'ADMIN' }), {
      userId: 1,
      username: 'admin',
      nickname: 'admin',
      avatarUrl: '',
    })
    const stored = JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) || '{}')
    expect(stored.state).toBeDefined()
    expect(stored.state).not.toHaveProperty('role')
    expect(stored.state.token).toBeDefined()
    expect(stored.state.userInfo).toMatchObject({ username: 'admin' })
    expect(stored.state.isLoggedIn).toBe(true)
  })

  it('login 写入 token/userInfo/role（role 从 JWT 解出）/isLoggedIn', () => {
    useAuthStore
      .getState()
      .login(jwtWith({ role: 'ADMIN', userId: 1 }), {
        userId: 1,
        username: 'admin',
        nickname: 'admin',
        avatarUrl: '',
      })
    const s = useAuthStore.getState()
    expect(s.token).toBeTruthy()
    expect(s.role).toBe('ADMIN')
    expect(s.isLoggedIn).toBe(true)
    expect(s.userInfo?.userId).toBe(1)
  })

  it('logout 清四元组 + best-effort 调 API（失败不阻塞）', async () => {
    // logout 现在会异步调 authApi.logout（POST /auth/logout），best-effort catch
    // 不需要在测试里断言 fetch——只验证本地态被清。
    useAuthStore
      .getState()
      .login(jwtWith({ role: 'USER', userId: 2 }), {
        userId: 2,
        username: 'u',
        nickname: 'u',
        avatarUrl: '',
      })
    useAuthStore.getState().logout()
    const s = useAuthStore.getState()
    expect(s.token).toBeNull()
    expect(s.userInfo).toBeNull()
    expect(s.role).toBeNull()
    expect(s.isLoggedIn).toBe(false)
    // 持久化层也同步清空（role 不持久化，但 token/userInfo/isLoggedIn 应为空）
    const stored = JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) || '{}')
    expect(stored.state.token).toBeNull()
    expect(stored.state.isLoggedIn).toBe(false)
  })

  it('updateProfile 合并到现有 userInfo', () => {
    useAuthStore
      .getState()
      .login(jwtWith({ role: 'USER', userId: 1 }), {
        userId: 1,
        username: 'u',
        nickname: '原昵称',
        avatarUrl: 'a',
      })
    useAuthStore.getState().updateProfile({ nickname: '新昵称', bio: '简介' })
    const s = useAuthStore.getState()
    expect(s.userInfo).toMatchObject({
      userId: 1,
      username: 'u',
      nickname: '新昵称',
      avatarUrl: 'a',
    })
  })

  it('rehydrateRole：token 在但 role 缺失时从 JWT 回填', () => {
    // 模拟旧持久化态迁移：token 在，role 未存（F0-4 之后 partialize 不再存 role）
    // 直接 setState 一个 role=null 的态（绕过 login）
    useAuthStore.setState({
      token: jwtWith({ role: 'ADMIN', userId: 1 }),
      userInfo: null,
      role: null,
      isLoggedIn: true,
    })
    useAuthStore.getState().rehydrateRole()
    expect(useAuthStore.getState().role).toBe('ADMIN')
  })

  it('rehydrateRole：无 token 或 role 已存在时不改', () => {
    useAuthStore.setState({ token: null, role: null, isLoggedIn: false, userInfo: null })
    useAuthStore.getState().rehydrateRole()
    expect(useAuthStore.getState().role).toBeNull()

    useAuthStore.setState({
      token: jwtWith({ role: 'USER', userId: 1 }),
      role: 'USER',
      isLoggedIn: true,
      userInfo: null,
    })
    useAuthStore.getState().rehydrateRole()
    // role 已存在，不被覆写
    expect(useAuthStore.getState().role).toBe('USER')
  })
})
