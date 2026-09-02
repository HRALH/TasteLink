/**
 * 后端契约类型定义（严格对齐 docs/05-接口API设计.md §5 数据模型 VO）
 */

/** 统一返回体 R */
export interface R<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/** 分页请求参数 */
export interface PageQuery {
  page?: number
  size?: number
}

/** 登录用户摘要（authStore 持久化用） */
export interface UserInfo {
  userId: number
  username: string
  nickname: string
  avatarUrl: string
  /** 角色（v2 由 JWT claim 解出，后端 LoginVO 未带）；'ADMIN' 管理员 / 'USER' 普通 */
  role?: 'USER' | 'ADMIN'
}

// ===== VO =====

export interface UserVO {
  id: number
  username?: string
  nickname: string
  avatarUrl: string
  bio: string
  followingCount: number
  followerCount: number
  reviewCount: number
  /** 当前登录人是否已关注；未登录为 false */
  hasFollowed: boolean
}

export interface CategoryVO {
  id: number
  code: string
  name: string
  iconUrl?: string
  sortOrder: number
}

export interface ShopVO {
  id: number
  name: string
  categoryId: number
  categoryName: string
  city: string
  address: string
  coverUrl: string
  avgRating: number
  reviewCount: number
}

/** ShopDetailVO 继承 ShopVO 字段 */
export interface ShopDetailVO extends ShopVO {
  phone: string
  description: string
  likeCount: number
  topReviews: ReviewVO[]
}

export interface ReviewVO {
  id: number
  shopId: number
  shopName: string
  userId: number
  userNickname: string
  userAvatarUrl: string
  content: string
  rating: number
  likeCount: number
  replyCount: number
  images: string[]
  hasLiked: boolean
  createTime: string
}

export interface CommentVO {
  id: number
  reviewId: number
  userId: number
  userNickname: string
  userAvatarUrl: string
  content: string
  createTime: string
}

export interface HomeVO {
  hotShops: ShopVO[]
  hotReviews: ReviewVO[]
}

// ===== 操作结果 =====

export interface LoginResult {
  token: string
  expiresInSec: number
  userId: number
  username: string
  nickname: string
  avatarUrl: string
}

export interface RegisterResult {
  userId: number
  username: string
}

export interface LikeResult {
  likeCount: number
}

export interface FollowResult {
  followingCount: number
}

export interface ImageUploadResult {
  url: string
  ossKey: string
}

// ===== 请求体 =====

export interface RegisterBody {
  username: string
  password: string
}

export interface LoginBody {
  username: string
  password: string
}

export interface UpdateProfileBody {
  nickname?: string
  avatarUrl?: string
  bio?: string
}

export interface CreateReviewBody {
  content: string
  rating: number
  imageUrls: string[]
}

export interface CreateCommentBody {
  content: string
}

/** 管理员编辑店铺请求（v2 Phase B）：字段均可选，非空才更新（对齐后端 UpdateShopRequest @Size） */
export interface UpdateShopRequest {
  name?: string
  categoryId?: number
  city?: string
  address?: string
  phone?: string
  coverUrl?: string
  description?: string
}
