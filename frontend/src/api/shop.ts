import { http } from './request'
import type {
  CategoryVO,
  PageQuery,
  PageResult,
  ReviewVO,
  ShopDetailVO,
  ShopVO,
} from '../types/api'

/** 店铺模块（docs/05 §4.3） */
export interface ShopListQuery extends PageQuery {
  keyword?: string
  categoryId?: number
  city?: string
  /** 默认 review_count（热度），可扩展 rating */
  sortBy?: string
}

export interface ShopReviewsQuery extends PageQuery {
  /** time 默认 / like 热度 */
  sortBy?: string
}

export const shopApi = {
  /** 店铺列表（公开） */
  list: (query: ShopListQuery) => http.get<PageResult<ShopVO>>('/shops', query),
  /** 店铺详情（公开） */
  detail: (shopId: number) => http.get<ShopDetailVO>(`/shops/${shopId}`),
  /** 店铺下点评列表（公开） */
  reviews: (shopId: number, query: ShopReviewsQuery) =>
    http.get<PageResult<ReviewVO>>(`/shops/${shopId}/reviews`, query),
  /** 分类字典（公开） */
  categories: () => http.get<CategoryVO[]>('/shops/categories'),
}
