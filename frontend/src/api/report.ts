import { http } from './request'
import type { CreateReportBody } from '../types/api'

/** 内容举报（产品优化 F4）：登录用户提交举报；重复举报后端返回 40905，拦截器 toast「已举报过」 */
export const reportApi = {
  report: (body: CreateReportBody) => http.post<void>('/reports', body),
}
