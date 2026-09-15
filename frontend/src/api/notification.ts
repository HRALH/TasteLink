import { http } from './request'
import type {
  NotificationVO,
  PageQuery,
  PageResult,
  UnreadCountResult,
} from '../types/api'

/** 站内通知（产品优化 F1）：全部需登录 */
export const notificationApi = {
  /** 我的通知分页（时间倒序） */
  page: (query: PageQuery) => http.get<PageResult<NotificationVO>>('/notifications', query),
  /** 未读数（顶栏红点轮询） */
  unreadCount: () => http.get<UnreadCountResult>('/notifications/unread-count'),
  /** 标记单条已读 */
  markRead: (id: number) => http.post<void>(`/notifications/${id}/read`),
  /** 全部已读 */
  markAllRead: () => http.post<void>('/notifications/read-all'),
}
