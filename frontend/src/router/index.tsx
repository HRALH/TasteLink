import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import MainLayout from '../components/layout/MainLayout'
import NotFound from '../pages/common/NotFound'

// 页面（M10 为占位 stub，后续里程碑替换为真实实现）
import LoginPage from '../pages/auth/LoginPage'
import RegisterPage from '../pages/auth/RegisterPage'
import HomePage from '../pages/home/HomePage'
import ShopListPage from '../pages/shop/ShopListPage'
import ShopDetailPage from '../pages/shop/ShopDetailPage'
import ShopReviewPage from '../pages/review/ShopReviewPage'
import ReviewDetailPage from '../pages/review/ReviewDetailPage'
import UserHomePage from '../pages/user/UserHomePage'
import FollowingsPage from '../pages/user/FollowingsPage'
import FollowersPage from '../pages/user/FollowersPage'
import MePage from '../pages/user/MePage'
import { useAuthStore } from '../store/authStore'

/**
 * 路由守卫：需登录页未登录跳 /login，携带 redirect 以便登录后回跳（docs/03 §4）
 */
function RequireAuth({ children }: { children: ReactNode }) {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn)
  const location = useLocation()
  if (!isLoggedIn) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  return <>{children}</>
}

export default function AppRouter() {
  return (
    <Routes>
      {/* 认证页：无顶栏布局 */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* 主体：带顶栏布局 */}
      <Route element={<MainLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/shops" element={<ShopListPage />} />
        <Route path="/shops/:id" element={<ShopDetailPage />} />
        <Route
          path="/shops/:id/review"
          element={
            <RequireAuth>
              <ShopReviewPage />
            </RequireAuth>
          }
        />
        <Route path="/reviews/:id" element={<ReviewDetailPage />} />
        <Route path="/users/:id" element={<UserHomePage />} />
        <Route path="/users/:id/followings" element={<FollowingsPage />} />
        <Route path="/users/:id/followers" element={<FollowersPage />} />
        <Route
          path="/me"
          element={
            <RequireAuth>
              <MePage />
            </RequireAuth>
          }
        />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
