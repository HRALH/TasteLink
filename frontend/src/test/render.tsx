import type { ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'

/**
 * 页面级测试的公共 Provider 壳：MemoryRouter（initialEntries 指定初始路由）。
 * F2 起在此叠加 QueryClientProvider（retry 关闭，失败场景即时暴露）。
 */
export function Providers({
  children,
  initialEntries = ['/'],
}: {
  children: ReactNode
  initialEntries?: string[]
}) {
  return <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>
}
