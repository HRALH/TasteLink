import { describe, expect, it, beforeEach, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

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

import ShopListPage from '../pages/shop/ShopListPage'
import { aShop } from './handlers'
import { server } from './server'
import { Providers } from './render'

const ok = (data: unknown) => HttpResponse.json({ code: 0, message: 'success', data })
const page = (records: unknown[], total: number) => ({ records, total, current: 1, size: 10, pages: 1 })

beforeEach(() => {
  vi.clearAllMocks()
})

describe('ShopListPage 筛选/翻页交互（F6）', () => {
  it('切分类 resetPage 到第 1 页（从第 2 页切分类应回到第 1 页）', async () => {
    let capturedPage: number | undefined
    server.use(
      http.get('/api/v1/shops/categories', () =>
        ok([{ id: 2, code: 'japanese', name: '日料', iconUrl: '', sortOrder: 1 }]),
      ),
      http.get('/api/v1/shops', ({ request }) => {
        const url = new URL(request.url)
        capturedPage = Number(url.searchParams.get('page'))
        return ok(page([aShop], 30))
      }),
    )
    const user = userEvent.setup()
    render(
      <Providers initialEntries={['/shops']}>
        <Routes>
          <Route path="/shops" element={<ShopListPage />} />
        </Routes>
      </Providers>,
    )

    // 初始加载：page=1
    await waitFor(() => expect(capturedPage).toBe(1))

    // 翻到第 2 页（handlers 返回 total=30，分页器会出现）
    const page2 = screen.getByRole('listitem', { name: '2' })
    await user.click(page2)
    await waitFor(() => expect(capturedPage).toBe(2))

    // 切分类 → 应回到 page=1
    const categorySelect = screen.getAllByRole('combobox')[0]
    await user.click(categorySelect)
    const option = await screen.findByText('日料', { selector: '.ant-select-item-option-content' })
    await user.click(option)
    await waitFor(() => expect(capturedPage).toBe(1))
  })
})

describe('LoginPage redirect 回跳解码（F6）', () => {
  it('redirect query 已编码时应解码后跳转（encodeURIComponent round-trip）', async () => {
    // 动态 import 避免与上面的 vi.mock 串扰
    const [{ default: LoginPage }, { useAuthStore }] = await Promise.all([
      import('../pages/auth/LoginPage'),
      import('../store/authStore'),
    ])
    const { message } = await import('antd')

    server.use(
      http.post('/api/v1/auth/login', async ({ request }) => {
        const body = (await request.json()) as { username: string }
        return ok({
          token: `head.${btoa(JSON.stringify({ sub: body.username, userId: 1, role: 'USER' }))}.sig`,
          expiresInSec: 3600,
          userId: 1,
          username: body.username,
          nickname: body.username,
          avatarUrl: '',
        })
      }),
    )
    useAuthStore.setState({ token: null, role: null, isLoggedIn: false, userInfo: null })

    // 编码的 redirect：/shops/123 → %2Fshops%2F123
    const redirect = encodeURIComponent('/shops/123')
    const user = userEvent.setup()

    const { container } = render(
      <MemoryRouter initialEntries={[`/login?redirect=${redirect}`]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/shops/123" element={<div data-testid="target">目标页</div>} />
        </Routes>
      </MemoryRouter>,
    )
    void container

    // 默认配置文件用户名/密码不触发校验——填表
    await user.type(screen.getByPlaceholderText('用户名'), 'u')
    await user.type(screen.getByPlaceholderText('密码'), 'pw')
    await user.click(screen.getByRole('button', { name: /登\s*录/ }))

    await waitFor(() => {
      expect(screen.getByTestId('target')).toBeInTheDocument()
    })
    void message // message.success 由 mock 接住，不渲染
  })
})
