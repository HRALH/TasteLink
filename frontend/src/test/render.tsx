import { useState, type ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'

/**
 * 页面级测试的公共 Provider 壳：QueryClientProvider（F2 起页面数据层）+ MemoryRouter。
 * QueryClient 每个壳实例一份（retry 关闭，失败即时暴露），用例间互不串缓存。
 */
export function Providers({
  children,
  initialEntries = ['/'],
}: {
  children: ReactNode
  initialEntries?: string[]
}) {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } },
      }),
  )
  return (
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>
    </QueryClientProvider>
  )
}

/** 仅需数据层（无需路由）的组件测试壳，如 FollowButton */
export function QueryShell({ children }: { children: ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } },
      }),
  )
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}
