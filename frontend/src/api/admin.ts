import { http } from './request'
import type {
  AdminReviewVO,
  PageQuery,
  PageResult,
  ReportVO,
  ShopDetailVO,
  UpdateShopRequest,
} from '../types/api'

/**
 * 管理员接口（v2 Phase B/C + 产品优化 F4 内容治理，docs/05）
 * - 路径前缀 `/admin/**` 由后端 `hasRole('ADMIN')` 强制鉴权；非管理员得 403
 * - 店铺加载/列表复用公开 `shopApi.detail` / `shopApi.list`（后端未建管理专用 GET 端点）
 * - 编辑并发冲突返回 409：拦截器抛 `ApiError`，由编辑页弹「加载最新内容」
 */
export const adminShopApi = {
  /** 编辑店铺：并发冲突（version 不符）后端返回 409 */
  update: (shopId: number, body: UpdateShopRequest) =>
    http.put<ShopDetailVO>(`/admin/shops/${shopId}`, body),
  /** 删除店铺：软删（下架 + 级联隐藏）+ 异步物理清理;二次删返回 404(幂等) */
  remove: (shopId: number) => http.delete<void>(`/admin/shops/${shopId}`),
}

/** 后台内容治理（F4）：点评下架/恢复 + 列表 + 举报分页 */
export const adminContentApi = {
  /** 分页查所有点评（含已下架），status 可空/1/0 */
  listReviews: (query: PageQuery & { status?: number | null }) =>
    http.get<PageResult<AdminReviewVO>>('/admin/reviews', query),
  /** 下架点评（status=0） */
  hideReview: (id: number) => http.put<void>(`/admin/reviews/${id}/hide`),
  /** 恢复点评（status=1） */
  restoreReview: (id: number) => http.put<void>(`/admin/reviews/${id}/restore`),
  /** 举报分页，status 可空/PENDING/RESOLVED */
  listReports: (query: PageQuery & { status?: string | null }) =>
    http.get<PageResult<ReportVO>>('/admin/reports', query),
}
