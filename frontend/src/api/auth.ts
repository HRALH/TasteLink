import { http } from './request'
import type { LoginBody, LoginResult, RegisterBody, RegisterResult } from '../types/api'

/** 认证模块（docs/05 §4.1） */
export const authApi = {
  /** 注册（公开） */
  register: (body: RegisterBody) => http.post<RegisterResult>('/auth/register', body),
  /** 登录（公开） */
  login: (body: LoginBody) => http.post<LoginResult>('/auth/login', body),
}
