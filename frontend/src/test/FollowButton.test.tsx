import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { Link, Route, Routes } from 'react-router-dom'
import UserHomePage from '../pages/user/UserHomePage'
import { server } from './server'
import { Providers } from './render'

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
  it('从 /users/1 导航到 /users/2，按钮关注态跟随新用户而非残留', async () => {
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

    // 同路由组件实例导航到 u2（hasFollowed=false）：
    // 无 key={userId} 时 FollowButton 的 useState 残留 u1 的 true，按钮仍显示“已关注”
    await userEvent.click(screen.getByText('go-u2'))
    await screen.findByRole('button', { name: '关注', exact: true })
    expect(screen.queryByRole('button', { name: '已关注' })).not.toBeInTheDocument()
  })
})
