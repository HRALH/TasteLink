import { App, message as staticMessage } from 'antd'
import { useEffect, useRef } from 'react'

/**
 * App.useApp() 的 message 引用（F5-1）。
 * antd 5 静态 `message.error` 不吃 ConfigProvider 主题且有 deprecation 警告，
 * 必须经 `<App>` 包裹 + App.useApp() 拿 context 内实例。
 */
type MessageApi = ReturnType<typeof App.useApp>['message']

let messageApi: MessageApi | null = null

/** 非组件代码（如 request 拦截器）读 message 实例；未绑定时回退到 antd 静态方法。 */
export function getAppMessage(): MessageApi {
  return messageApi ?? staticMessage
}

/** App 内组件挂载时写入；模块级 ref 保证拦截器能读到。 */
export function setAppMessage(m: MessageApi): void {
  messageApi = m
}

/**
 * 必须渲染在 `<AntApp>` 内部。空渲染输出——作用仅在副作用里把 useApp() 的
 * message 实例 ref 绑给 getAppMessage()。
 */
export function MessageBinder() {
  const { message } = App.useApp()
  const ready = useRef(false)
  useEffect(() => {
    if (ready.current) return
    setAppMessage(message)
    ready.current = true
  }, [message])
  return null
}
