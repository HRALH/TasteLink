import { http } from './request'
import type { PageQuery, PageResult, ReviewVO } from '../types/api'

/** 关注 feed（产品优化 F3）：我关注的人的点评，按时间倒序 */
export const feedApi = {
  followingReviews: (query: PageQuery) =>
    http.get<PageResult<ReviewVO>>('/feed/following', query),
}
