import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { Skeleton } from 'antd'
import MainLayout from '../components/layout/MainLayout'
import NotFound from '../pages/common/NotFound'

// 首屏页：静态引入
import LoginPage from '../pages/auth/LoginPage'
import RegisterPage from '../pages/auth/RegisterPage'
import HomePage from '../pages/home/HomePage'
import ShopListPage from '../pages/shop/ShopListPage'
import ShopDetailPage from '../pages/shop/ShopDetailPage'
import UserHomePage from '../pages/user/UserHomePage'
import MePage from '../pages/user/MePage'
import { useAuthStore } from '../store/authStore'

// 低频/后台页（F3-1）：路由级代码分割，不进普通用户首屏
const ShopReviewPage = lazy(() => import('../pages/review/ShopReviewPage'))
const ReviewDetailPage = lazy(() => import('../pages/review/ReviewDetailPage'))
const FollowingsPage = lazy(() => import('../pages/user/FollowingsPage'))
const FollowersPage = lazy(() => import('../pages/user/FollowersPage'))
const AdminShopListPage = lazy(() => import('../pages/admin/AdminShopListPage'))
const AdminShopEditPage = lazy(() => import('../pages/admin/AdminShopEditPage'))

/** 懒加载页 fallback：沿用全站骨架态（与页内加载态一致） */
const pageFallback = (
  <div style={{ padding: 24 }}>
    <Skeleton active />
  </div>
)

function lazyPage(el: ReactNode) {
  return <Suspense fallback={pageFallback}>{el}</Suspense>
}

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

/**
 * 路由守卫：管理员页（v2 FE-0）—— 未登录跳 /login（带 redirect），非管理员跳首页。
 * 真正鉴权仍由后端 `/admin/**` hasRole('ADMIN') 强制（403）；此守卫仅做 UI 层前置拦截。
 */
export function RequireAdmin({ children }: { children: ReactNode }) {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn)
  const isAdmin = useAuthStore((s) => s.role === 'ADMIN')
  const location = useLocation()
  if (!isLoggedIn) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  if (!isAdmin) {
    return <Navigate to="/" replace />
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
          element={lazyPage(
            <RequireAuth>
              <ShopReviewPage />
            </RequireAuth>,
          )}
        />
        <Route path="/reviews/:id" element={lazyPage(<ReviewDetailPage />)} />
        <Route path="/users/:id" element={<UserHomePage />} />
        <Route path="/users/:id/followings" element={lazyPage(<FollowingsPage />)} />
        <Route path="/users/:id/followers" element={lazyPage(<FollowersPage />)} />
        <Route
          path="/me"
          element={
            <RequireAuth>
              <MePage />
            </RequireAuth>
          }
        />
        <Route
          path="/admin/shops"
          element={lazyPage(
            <RequireAdmin>
              <AdminShopListPage />
            </RequireAdmin>,
          )}
        />
        <Route
          path="/admin/shops/:id/edit"
          element={lazyPage(
            <RequireAdmin>
              <AdminShopEditPage />
            </RequireAdmin>,
          )}
        />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
