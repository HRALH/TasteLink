import { http } from './request'
import type { HomeVO } from '../types/api'

/** 首页模块（docs/05 §4.7） */
export const homeApi = {
  /** 首页热门内容，city 可选 */
  home: (city?: string) => http.get<HomeVO>('/home', city ? { city } : undefined),
}
