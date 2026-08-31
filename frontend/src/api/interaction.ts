import { http } from './request'
import type {
  CommentVO,
  CreateCommentBody,
  LikeResult,
  PageQuery,
  PageResult,
} from '../types/api'

/** 互动模块（docs/05 §4.5）：点赞/评论 */
export const interactionApi = {
  /** 点赞点评（幂等） */
  like: (reviewId: number) => http.post<LikeResult>(`/reviews/${reviewId}/likes`),
  /** 取消点赞（幂等） */
  unlike: (reviewId: number) => http.delete<LikeResult>(`/reviews/${reviewId}/likes`),
  /** 评论列表（公开，单层） */
  comments: (reviewId: number, query: PageQuery) =>
    http.get<PageResult<CommentVO>>(`/reviews/${reviewId}/comments`, query),
  /** 发表评论（登录） */
  createComment: (reviewId: number, body: CreateCommentBody) =>
    http.post<CommentVO>(`/reviews/${reviewId}/comments`, body),
}
