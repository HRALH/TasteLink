import { describe, expect, it, beforeEach, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { Route, Routes } from 'react-router-dom'

// antd 静态 Modal/message 在 jsdom 无 App context 下渲染不可靠；注入 Modal.warning / message spy，
// 断言“409 时是否唤起 Modal.warning”并直接调用其 onOk 验证“加载最新内容”行为。其余组件保持真实。
vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>()
  return {
    ...actual,
    Modal: {
      ...actual.Modal,
      warning: vi.fn(),
      confirm: vi.fn(),
      info: vi.fn(),
      error: vi.fn(),
      success: vi.fn(),
    },
    message: {
      error: vi.fn(),
      success: vi.fn(),
      info: vi.fn(),
      warning: vi.fn(),
      loading: vi.fn(),
    },
  }
})

import { Modal } from 'antd'
import AdminShopEditPage from '../pages/admin/AdminShopEditPage'
import { aShop } from './handlers'
import { server } from './server'
import { Providers } from './render'
import { useAuthStore } from '../store/authStore'

function renderAt(path: string) {
  render(
    <Providers initialEntries={[path]}>
      <Routes>
        <Route path="/admin/shops/:id/edit" element={<AdminShopEditPage />} />
        <Route path="/admin/shops" element={<div data-testid="admin-list">列表</div>} />
      </Routes>
    </Providers>,
  )
}

describe('AdminShopEditPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 't', userInfo: null, role: 'ADMIN', isLoggedIn: true })
    vi.clearAllMocks()
  })

  it('载入店铺并保存成功 → 跳回列表', async () => {
    const user = userEvent.setup()
    renderAt('/admin/shops/1/edit')
    await screen.findByDisplayValue('测试小店')
    await user.click(screen.getByRole('button', { name: /保\s*存/ }))
    await waitFor(() => expect(screen.getByTestId('admin-list')).toBeInTheDocument())
  })

  it('409 冲突 → Modal.warning 提示，调用 onOk 重新拉取并回填', async () => {
    let detailCalls = 0
    server.use(
      // 显式覆盖 categories，避免被 /api/v1/shops/:id（:id 可匹配 'categories'）抢先吞掉
      http.get('/api/v1/shops/categories', () =>
        HttpResponse.json({
          code: 0,
          message: 'success',
          data: [{ id: 2, code: 'japanese', name: '日料', iconUrl: '', sortOrder: 1 }],
        }),
      ),
      http.get('/api/v1/shops/:id', () => {
        detailCalls += 1
        return HttpResponse.json({
          code: 0,
          message: 'success',
          data: { ...aShop, name: detailCalls === 1 ? '原始名' : '已刷新' },
        })
      }),
      http.put('/api/v1/admin/shops/:id', () =>
        HttpResponse.json({ code: 409, message: '店铺已被他人修改，请刷新重试' }, { status: 409 }),
      ),
    )
    const user = userEvent.setup()
    renderAt('/admin/shops/1/edit')
    await screen.findByDisplayValue('原始名')
    await user.click(screen.getByRole('button', { name: /保\s*存/ }))
    await waitFor(() => expect(Modal.warning).toHaveBeenCalledTimes(1))
    const arg = Modal.warning.mock.calls[0][0] as { title: string; onOk: () => Promise<void> }
    expect(arg.title).toContain('该店铺已被其它管理员修改并保存')
    await arg.onOk()
    await waitFor(() => expect(screen.getByDisplayValue('已刷新')).toBeInTheDocument())
    expect(detailCalls).toBe(2)
  })
})
