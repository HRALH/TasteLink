import { describe, expect, it, beforeEach, vi } from 'vitest'
import { HttpResponse, http } from 'msw'

// antd 静态 message 在 jsdom（无 App context）下渲染不可靠；改注入 message spy，
// 断言“是否调用 message.error”，确定性更强。其它 antd 工具保持真实（spread actual）。
vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>()
  return {
    ...actual,
    message: {
      error: vi.fn(),
      success: vi.fn(),
      info: vi.fn(),
      warning: vi.fn(),
      loading: vi.fn(),
    },
  }
})

import { message } from 'antd'
import { ApiError, http as apiHttp } from '../api/request'
import { useAuthStore } from '../store/authStore'
import { Code } from '../utils/constants'
import { server } from './server'

describe('request 拦截器', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 't', userInfo: null, role: 'USER', isLoggedIn: true })
    vi.clearAllMocks()
  })

  it('成功时解包 R.data，不 toast', async () => {
    const data = await apiHttp.get<{ id: number }>('/shops/1')
    expect(data).toMatchObject({ id: 1 })
    expect(message.error).not.toHaveBeenCalled()
  })

  it('幂等码 40902 按成功返回 data 且不 toast', async () => {
    server.use(
      http.get('/api/v1/shops/special', () =>
        HttpResponse.json({ code: Code.ALREADY_LIKED, message: '已点赞', data: { likeCount: 7 } }),
      ),
    )
    const data = await apiHttp.get<{ likeCount: number }>('/shops/special')
    expect(data).toEqual({ likeCount: 7 })
    expect(message.error).not.toHaveBeenCalled()
  })

  it('409 抑制 toast 并抛 ApiError(code=409)', async () => {
    server.use(
      http.put('/api/v1/admin/shops/1', () =>
        HttpResponse.json({ code: 409, message: '店铺已被他人修改，请刷新重试', data: null }, { status: 409 }),
      ),
    )
    await expect(apiHttp.put('/admin/shops/1', {})).rejects.toSatisfy(
      (e: unknown) => e instanceof ApiError && e.code === 409,
    )
    expect(message.error).not.toHaveBeenCalled()
  })

  it('其它业务错误自动 toast', async () => {
    server.use(
      http.get('/api/v1/shops/bad', () => HttpResponse.json({ code: 500, message: '服务器异常' })),
    )
    await expect(apiHttp.get('/shops/bad')).rejects.toThrow()
    expect(message.error).toHaveBeenCalledWith('服务器异常')
  })

  it('401 清登录态并提示', async () => {
    server.use(
      http.get('/api/v1/shops/secure', () =>
        HttpResponse.json({ code: 401, message: '未登录' }, { status: 401 }),
      ),
    )
    await expect(apiHttp.get('/shops/secure')).rejects.toBeDefined()
    expect(useAuthStore.getState().isLoggedIn).toBe(false)
    expect(message.error).toHaveBeenCalled()
  })
})
