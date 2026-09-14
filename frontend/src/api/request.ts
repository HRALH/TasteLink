import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import { getAppMessage } from '../utils/message'
import { useAuthStore } from '../store/authStore'
import { Code } from '../utils/constants'

/**
 * 可被调用方分支处理的业务错误码：拦截器抑制自动 `message.error`，抛 `ApiError` 交调用方处理。
 * v2：`SHOP_VERSION_CONFLICT(409)` 乐观锁冲突，由管理员编辑页弹 Modal 提示「加载最新内容」。
 */
export const PROCESSABLE_CODES = new Set<number>([Code.SHOP_VERSION_CONFLICT])

/** 统一业务错误：携带 `code`（及可选 `data`），调用方可 `catch` 后按码分支（如 409 刷新）。 */
export class ApiError extends Error {
  code: number
  data?: unknown
  constructor(code: number, message: string, data?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.data = data
  }
}

/**
 * F5-1：把 axios / 浏览器层抛出的英文错误原文归一为面向用户的中文文案。
 * - timeout → 「网络超时，请重试」
 * - Network Error（无响应/无法连接）→ 「网络异常，请检查连接」
 * 其余带 HTTP 体 message 的交调用方/上游分支处理，不在此强制覆盖。
 */
function normalizeNetworkError(error: AxiosError): string | null {
  // axios 超时：error.code === 'ECONNABORTED' 且 message 含 timeout
  if (error.code === 'ECONNABORTED' || /timeout of \d+ms exceeded/i.test(error.message)) {
    return '网络超时，请重试'
  }
  // 网络层失败：无 response、且非超时
  if (!error.response && (error.message === 'Network Error' || error.code === 'ERR_NETWORK')) {
    return '网络异常，请检查连接'
  }
  return null
}

/**
 * axios 实例与拦截器封装（docs/03 §5.1）
 * - baseURL：env VITE_API_BASE_URL，默认 /api/v1（dev 经 Vite 代理转后端）
 * - 请求拦截器：自动注入 Authorization: Bearer ${token}
 * - 响应拦截器：取后端 R.data；code≠0 提示并 reject；幂等码(已点赞/已关注)按成功语义返回 data
 *   HTTP 401：清登录态并跳 /login(携带 redirect)
 * - F5-1：message 经 getAppMessage() 取 App.useApp() context 实例（吃 ConfigProvider 主题，
 *   避开 antd5 静态函数 deprecation 警告）；网络层英文错误归一为中文文案
 */
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

const instance = axios.create({
  baseURL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json; charset=utf-8' },
})

// 请求拦截：注入 JWT
instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 用作 401 跳登录的去重
let redirecting = false
function redirectToLogin() {
  if (redirecting) return
  redirecting = true
  const { pathname, search } = window.location
  if (pathname === '/login' || pathname === '/register') {
    redirecting = false
    return
  }
  const redirect = encodeURIComponent(pathname + search)
  window.location.href = `/login?redirect=${redirect}`
}

// 响应拦截：解包 R，统一错误处理
instance.interceptors.response.use(
  (response) => {
    const r = response.data
    // 非 R 结构兜底（理论上不会出现，保持原样返回）
    if (r == null || typeof r.code === 'undefined') {
      return response
    }
    if (r.code !== Code.SUCCESS) {
      // 幂等场景：已点赞/已关注，按业务成功处理，直接返回 data
      if (r.code === Code.ALREADY_LIKED || r.code === Code.ALREADY_FOLLOWED) {
        return r.data
      }
      // 可处理码：抑制自动 toast，抛 ApiError 交调用方分支处理（v2 409 乐观锁冲突）
      if (PROCESSABLE_CODES.has(r.code)) {
        return Promise.reject(new ApiError(r.code, r.message || '', r.data))
      }
      getAppMessage().error(r.message || '请求失败')
      return Promise.reject(new ApiError(r.code, r.message || `code=${r.code}`, r.data))
    }
    return r.data
  },
  (error: AxiosError) => {
    const status = error.response?.status
    const r = error.response?.data as { code?: number; message?: string } | undefined
    if (status === 401) {
      useAuthStore.getState().logout()
      getAppMessage().error(r?.message || '登录已失效，请重新登录')
      redirectToLogin()
    } else if (status === 409 && PROCESSABLE_CODES.has(r?.code ?? 0)) {
      // 乐观锁冲突走 HTTP 409：抑制 toast，抛 ApiError 交调用方弹 Modal（后端 code 409）
      return Promise.reject(new ApiError(r?.code ?? 409, r?.message || '', r))
    } else if (status && status >= 500) {
      getAppMessage().error(r?.message || '服务器异常，请稍后再试')
    } else if (r?.message) {
      getAppMessage().error(r.message)
    } else {
      // F5-1：英文网络错误归一为中文文案
      const normalized = normalizeNetworkError(error)
      getAppMessage().error(normalized ?? '网络异常，请稍后再试')
    }
    return Promise.reject(error)
  },
)

/** http 客户端：响应拦截器已解包到 R.data，此处断言为 Promise<T>。 */
export const http = {
  get<T = unknown>(url: string, params?: object): Promise<T> {
    return instance.get(url, { params }) as unknown as Promise<T>
  },
  post<T = unknown>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
    return instance.post(url, data, config) as unknown as Promise<T>
  },
  put<T = unknown>(url: string, data?: object): Promise<T> {
    return instance.put(url, data) as unknown as Promise<T>
  },
  delete<T = unknown>(url: string, params?: object): Promise<T> {
    return instance.delete(url, { params }) as unknown as Promise<T>
  },
}

export default instance
