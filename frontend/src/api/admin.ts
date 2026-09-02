import { http } from './request'
import type { ShopDetailVO, UpdateShopRequest } from '../types/api'

/**
 * 管理员店铺接口（v2 Phase B/C，docs/05）
 * - 路径前缀 `/admin/shops` 由后端 `hasRole('ADMIN')` 强制鉴权；非管理员得 403
 * - 店铺加载/列表复用公开 `shopApi.detail` / `shopApi.list`（后端未建管理专用 GET 端点）
 * - 编辑并发冲突返回 409：拦截器抛 `ApiError`，由编辑页弹「加载最新内容」
 */
export const adminShopApi = {
  /** 编辑店铺：并发冲突（version 不符）后端返回 409 */
  update: (shopId: number, body: UpdateShopRequest) =>
    http.put<ShopDetailVO>(`/admin/shops/${shopId}`, body),
  /** 删除店铺：软删（下架 + 级联隐藏）+ 异步物理清理；二次删返回 404（幂等） */
  remove: (shopId: number) => http.delete<void>(`/admin/shops/${shopId}`),
}
