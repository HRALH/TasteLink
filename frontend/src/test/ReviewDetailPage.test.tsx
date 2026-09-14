import { describe, expect, it, beforeEach, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { Route, Routes } from 'react-router-dom'

// antd 静态 message 在 jsdom（无 App context）下渲染不可靠；注入 message spy
// 断言「登录前点击 → 提示请先登录」
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
import ReviewDetailPage from '../pages/review/ReviewDetailPage'
import { server } from './server'
import { Providers } from './render'
import { useAuthStore } from '../store/authStore'

/** 一份带 hasLiked=false 的点评（含 ReviewVO 全字段） */
const aReviewLiked = {
  id: 1,
  shopId: 1,
  shopName: '测试小店',
  userId: 2,
  userNickname: '食客',
  userAvatarUrl: '',
  content: '好吃',
  rating: 5,
  likeCount: 3,
  replyCount: 0,
  images: [],
  hasLiked: false,
  createTime: '2026-09-11',
}

const ok = (data: unknown) => HttpResponse.json({ code: 0, message: 'success', data })
const emptyPage = { records: [], total: 0, current: 1, size: 10, pages: 0 }

function renderAt(path: string) {
  render(
    <Providers initialEntries={[path]}>
      <Routes>
        <Route path="/reviews/:id" element={<ReviewDetailPage />} />
      </Routes>
    </Providers>,
  )
}

describe('ReviewDetailPage 点赞乐观更新（F6）', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 't', userInfo: null, role: 'USER', isLoggedIn: true })
    vi.clearAllMocks()
  })

  it('toggleLike 失败回滚 hasLiked 与 likeCount', async () => {
    server.use(
      http.get('/api/v1/reviews/1', () => ok(aReviewLiked)),
      http.get('/api/v1/reviews/1/comments', () => ok(emptyPage)),
      // 点赞 API 返回 500（拦截器会 toast + reject）
      http.post('/api/v1/reviews/1/likes', () =>
        HttpResponse.json({ code: 500, message: '服务器异常' }),
      ),
    )
    const user = userEvent.setup()
    renderAt('/reviews/1')

    // 初始：3 赞，未点赞
    const btn = await screen.findByRole('button', { name: /3\s*赞/ })
    expect(btn).toBeInTheDocument()

    // 点击点赞：乐观翻 → 等待回滚
    await user.click(btn)
    // 乐观瞬时显示 4 赞；失败回滚后恢复 3 赞
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /3\s*赞/ })).toBeInTheDocument()
    })
    expect(message.error).toHaveBeenCalled()
  })

  it('未登录点击点赞 → message.warning 提示，不调 API', async () => {
    useAuthStore.setState({ token: null, role: null, isLoggedIn: false, userInfo: null })
    server.use(
      http.get('/api/v1/reviews/1', () => ok(aReviewLiked)),
      http.get('/api/v1/reviews/1/comments', () => ok(emptyPage)),
      http.post('/api/v1/reviews/1/likes', () => {
        throw new Error('should not be called')
      }),
    )
    const user = userEvent.setup()
    renderAt('/reviews/1')

    const btn = await screen.findByRole('button', { name: /3\s*赞/ })
    await user.click(btn)
    expect(message.warning).toHaveBeenCalledWith('请先登录')
  })
})
