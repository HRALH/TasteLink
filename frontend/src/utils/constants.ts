/**
 * 全局常量
 */

/** 业务错误码（对齐 docs/05 §3.2） */
export const Code = {
  SUCCESS: 0,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  USERNAME_EXISTS: 40901,
  ALREADY_LIKED: 40902,
  ALREADY_FOLLOWED: 40903,
  CANNOT_FOLLOW_SELF: 40904,
  SERVER_ERROR: 500,
} as const

/** 默认分页参数 */
export const DEFAULT_PAGE = 1
export const DEFAULT_SIZE = 10

/** 单条点评图片上限（docs/01 R2） */
export const MAX_REVIEW_IMAGES = 9

/** 首页/店铺筛选城市字典（MVP 按城市字符串） */
export const CITIES = [
  '北京',
  '上海',
  '广州',
  '深圳',
  '杭州',
  '成都',
  '武汉',
  '南京',
  '重庆',
  '西安',
] as const

/** authStore 持久化到 localStorage 的 key */
export const AUTH_STORAGE_KEY = 'tastelink-auth'

/** 店铺列表排序 */
export const ShopSort = {
  /** 默认：按点评数（热度） */
  REVIEW_COUNT: 'review_count',
  /** 按评分 */
  RATING: 'rating',
} as const

/** 点评列表排序 */
export const ReviewSort = {
  /** 默认：按时间倒序 */
  TIME: 'time',
  /** 按点赞数（热度） */
  LIKE: 'like',
} as const
