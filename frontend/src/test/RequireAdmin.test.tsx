import { describe, expect, it, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { RequireAdmin } from '../router'
import { useAuthStore } from '../store/authStore'

function renderAt(path: string) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<div data-testid="login" />} />
        <Route path="/" element={<div data-testid="home" />} />
        <Route
          path="/admin/*"
          element={
            <RequireAdmin>
              <div data-testid="protected" />
            </RequireAdmin>
          }
        />
      </Routes>
    </MemoryRouter>,
  )
}

describe('RequireAdmin 守卫', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: null, userInfo: null, role: null, isLoggedIn: false })
  })

  it('未登录跳 /login', () => {
    renderAt('/admin/shops')
    expect(screen.getByTestId('login')).toBeInTheDocument()
    expect(screen.queryByTestId('protected')).not.toBeInTheDocument()
  })

  it('登录但非管理员跳首页', () => {
    useAuthStore.setState({ token: 't', userInfo: null, role: 'USER', isLoggedIn: true })
    renderAt('/admin/shops')
    expect(screen.getByTestId('home')).toBeInTheDocument()
    expect(screen.queryByTestId('protected')).not.toBeInTheDocument()
  })

  it('管理员放行', () => {
    useAuthStore.setState({ token: 't', userInfo: null, role: 'ADMIN', isLoggedIn: true })
    renderAt('/admin/shops')
    expect(screen.getByTestId('protected')).toBeInTheDocument()
  })
})
