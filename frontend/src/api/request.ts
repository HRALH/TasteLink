import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import { message } from 'antd'
import { useAuthStore } from '../store/authStore'
import { Code } from '../utils/constants'

/**
 * axios 实例与拦截器封装（docs/03 §5.1）
 * - baseURL：env VITE_API_BASE_URL，默认 /api/v1（dev 经 Vite 代理转后端）
 * - 请求拦截器：自动注入 Authorization: Bearer ${token}
 * - 响应拦截器：取后端 R.data；code≠0 提示并 reject；幂等码(已点赞/已关注)按成功语义返回 data
 *   HTTP 401：清登录态并跳 /login(携带 redirect)
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
      message.error(r.message || '请求失败')
      return Promise.reject(new Error(r.message || `code=${r.code}`))
    }
    return r.data
  },
  (error: AxiosError) => {
    const status = error.response?.status
    const r = error.response?.data as { code?: number; message?: string } | undefined
    if (status === 401) {
      useAuthStore.getState().logout()
      message.error(r?.message || '登录已失效，请重新登录')
      redirectToLogin()
    } else if (status && status >= 500) {
      message.error(r?.message || '服务器异常，请稍后再试')
    } else if (r?.message) {
      message.error(r.message)
    } else if (error.message) {
      message.error(error.message)
    } else {
      message.error('网络异常，请稍后再试')
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
