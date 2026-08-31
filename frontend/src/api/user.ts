import { http } from './request'
import type { PageQuery, PageResult, ReviewVO, UpdateProfileBody, UserVO } from '../types/api'

/** 用户模块（docs/05 §4.2） */
export const userApi = {
  /** 当前登录用户完整资料（登录） */
  me: () => http.get<UserVO>('/users/me'),
  /** 修改当前用户资料（登录） */
  updateMe: (body: UpdateProfileBody) => http.put<UserVO>('/users/me', body),
  /** 查看某用户主页资料（公开） */
  get: (userId: number) => http.get<UserVO>(`/users/${userId}`),
  /** 某用户发布的点评列表（公开） */
  reviews: (userId: number, query: PageQuery) =>
    http.get<PageResult<ReviewVO>>(`/users/${userId}/reviews`, query),
}
