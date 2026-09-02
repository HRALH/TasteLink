import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './server'

// —— jsdom 缺失的浏览器 API 补桩（antd responsiveObserver / Upload 滚动等依赖）——
if (!window.matchMedia) {
  window.matchMedia = (query: string) =>
    ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    }) as unknown as MediaQueryList
}

class MockResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
if (!window.ResizeObserver) window.ResizeObserver = MockResizeObserver as unknown as typeof ResizeObserver
if (!window.scrollTo) window.scrollTo = () => {}
if (!Element.prototype.scrollIntoView) Element.prototype.scrollIntoView = () => {}

// 启动 msw：未处理请求直接报错，逼出漏配 handler
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

afterEach(() => {
  cleanup()
  // 清理 antd 静态 message / Modal 残留节点，避免用例间串扰
  document.body
    .querySelectorAll('.ant-message, .ant-modal-root, .ant-notification, .ant-image-preview-root')
    .forEach((el) => el.remove())
  server.resetHandlers()
  localStorage.clear()
})

afterAll(() => server.close())
