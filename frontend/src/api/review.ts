import { http } from './request'
import type { CreateReviewBody, ReviewVO } from '../types/api'

/** 点评模块（docs/05 §4.4） */
export const reviewApi = {
  /** 对店铺发布点评（登录） */
  create: (shopId: number, body: CreateReviewBody) =>
    http.post<ReviewVO>(`/shops/${shopId}/reviews`, body),
  /** 点评详情（公开，登录则含 hasLiked） */
  get: (reviewId: number) => http.get<ReviewVO>(`/reviews/${reviewId}`),
}
