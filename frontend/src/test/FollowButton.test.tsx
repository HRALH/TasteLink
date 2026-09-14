import { describe, expect, it, beforeEach, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { Link, Route, Routes } from 'react-router-dom'
import FollowButton from '../components/FollowButton'
import UserHomePage from '../pages/user/UserHomePage'
import { server } from './server'
import { Providers, QueryShell } from './render'
import { useAuthStore } from '../store/authStore'

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

/** UserVO 最小构造 */
const userVO = (id: number, hasFollowed: boolean) => ({
  id,
  nickname: `用户${id}`,
  avatarUrl: '',
  bio: '',
  followingCount: 0,
  followerCount: 5,
  reviewCount: 0,
  hasFollowed,
})

const emptyPage = { records: [], total: 0, current: 1, size: 10, pages: 0 }
const ok = (data: unknown) => HttpResponse.json({ code: 0, message: 'success', data })

beforeEach(() => {
  useAuthStore.setState({ token: 't', userInfo: null, role: 'USER', isLoggedIn: true })
  vi.clearAllMocks()
})

describe('FollowButton key 契约与乐观更新回滚（F1-1 / F6）', () => {
  it('组件级契约：useState 只初始化一次，调用方必须以 key={userId} 切用户重置', () => {
    // 无 key：同一实例换 userId/hasFollowed，内部态残留（这正是 F1-1 的根因）
    const { rerender } = render(
      <QueryShell>
        <FollowButton userId={1} hasFollowed />
      </QueryShell>,
    )
    expect(screen.getByRole('button', { name: '已关注' })).toBeInTheDocument()
    rerender(
      <QueryShell>
        <FollowButton userId={2} hasFollowed={false} />
      </QueryShell>,
    )
    expect(screen.getByRole('button', { name: '已关注' })).toBeInTheDocument()

    // 有 key（UserHomePage/UserCard 的用法）：key 变化 remount，态重置为新用户
    rerender(
      <QueryShell>
        <FollowButton key={2} userId={2} hasFollowed={false} />
      </QueryShell>,
    )
    expect(screen.getByRole('button', { name: '关注' })).toBeInTheDocument()
  })

  it('关注翻转失败回滚态（乐观更新 onMutate 翻 → onError 回滚）', async () => {
    server.use(
      // follow API 返回 500（拦截器会 toast + reject）
      http.post('/api/v1/users/2/follow', () =>
        HttpResponse.json({ code: 500, message: '服务器异常' }),
      ),
    )
    const user = userEvent.setup()
    render(
      <QueryShell>
        <FollowButton userId={2} hasFollowed={false} />
      </QueryShell>,
    )

    const btn = screen.getByRole('button', { name: '关注' })
    await user.click(btn)
    // 乐观瞬时翻「已关注」→ 失败回滚「关注」
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '关注' })).toBeInTheDocument()
    })
    expect(message.error).toHaveBeenCalled()
  })

  it('页面级：从 /users/1 导航到 /users/2，按钮关注态跟随新用户而非残留', async () => {
    server.use(
      http.get('/api/v1/users/:id', ({ params }) =>
        ok(userVO(Number(params.id), Number(params.id) === 1)),
      ),
      http.get('/api/v1/users/:id/reviews', () => ok(emptyPage)),
    )
    render(
      <Providers initialEntries={['/users/1']}>
        <Link to="/users/2">go-u2</Link>
        <Routes>
          <Route path="/users/:id" element={<UserHomePage />} />
        </Routes>
      </Providers>,
    )
    await screen.findByRole('button', { name: '已关注' })
    await userEvent.click(screen.getByText('go-u2'))
    await screen.findByRole('button', { name: '关注', exact: true })
    expect(screen.queryByRole('button', { name: '已关注' })).not.toBeInTheDocument()
  })

  it('未登录点击 → message.warning 提示，不调 API', async () => {
    useAuthStore.setState({ token: null, role: null, isLoggedIn: false, userInfo: null })
    server.use(
      http.post('/api/v1/users/2/follow', () => {
        throw new Error('should not be called')
      }),
    )
    const user = userEvent.setup()
    render(
      <QueryShell>
        <FollowButton userId={2} hasFollowed={false} />
      </QueryShell>,
    )
    await user.click(screen.getByRole('button', { name: '关注' }))
    expect(message.warning).toHaveBeenCalledWith('请先登录')
  })
})
