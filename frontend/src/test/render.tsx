import type { ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'

/** 测试用 QueryClient：retry 关闭，失败场景即时暴露（不被重试拖慢/掩盖） */
export function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } },
  })
}

/**
 * 页面级测试的公共 Provider 壳：QueryClientProvider（F2 起页面数据层）+ MemoryRouter。
 */
export function Providers({
  children,
  initialEntries = ['/'],
}: {
  children: ReactNode
  initialEntries?: string[]
}) {
  return (
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>
    </QueryClientProvider>
  )
}
