import { http } from './request'
import type { LoginBody, LoginResult, RegisterBody, RegisterResult } from '../types/api'

/** 认证模块（docs/05 §4.1） */
export const authApi = {
  /** 注册（公开） */
  register: (body: RegisterBody) => http.post<RegisterResult>('/auth/register', body),
  /** 登录（公开） */
  login: (body: LoginBody) => http.post<LoginResult>('/auth/login', body),
  /** 登出（需登录；best-effort：后端清理 JWT 黑名单/会话态，失败不阻塞前端清态） */
  logout: () => http.post<void>('/auth/logout'),
}
