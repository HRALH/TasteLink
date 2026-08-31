import { http } from './request'
import type { FollowResult, PageQuery, PageResult, UserVO } from '../types/api'

/** 关注模块（docs/05 §4.6） */
export const followApi = {
  /** 关注用户（幂等，不可关注自己） */
  follow: (userId: number) => http.post<FollowResult>(`/users/${userId}/follow`),
  /** 取消关注（幂等） */
  unfollow: (userId: number) => http.delete<FollowResult>(`/users/${userId}/follow`),
  /** 关注列表（公开） */
  followings: (userId: number, query: PageQuery) =>
    http.get<PageResult<UserVO>>(`/users/${userId}/followings`, query),
  /** 粉丝列表（公开） */
  followers: (userId: number, query: PageQuery) =>
    http.get<PageResult<UserVO>>(`/users/${userId}/followers`, query),
}
