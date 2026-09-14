import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { Link, Route, Routes } from 'react-router-dom'
import FollowButton from '../components/FollowButton'
import UserHomePage from '../pages/user/UserHomePage'
import { server } from './server'
import { Providers, QueryShell } from './render'

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

describe('FollowButton 跨用户状态（F1-1 回归）', () => {
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

    // u1：hasFollowed=true → 已关注
    await screen.findByRole('button', { name: '已关注' })

    // 同路由组件实例导航到 u2（hasFollowed=false）。
    // 注：UserHomePage 拉取期间的全页骨架会卸载按钮，视觉残留窗口极短；
    // 此用例锁定期望行为，组件级契约测试（上）才钉住 key 的必要性。
    await userEvent.click(screen.getByText('go-u2'))
    await screen.findByRole('button', { name: '关注', exact: true })
    expect(screen.queryByRole('button', { name: '已关注' })).not.toBeInTheDocument()
  })
})
