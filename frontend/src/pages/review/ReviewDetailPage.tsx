import { useEffect, useState } from 'react'
import {
  Avatar,
  Button,
  Card,
  Empty,
  Image as AntImage,
  Input,
  Pagination,
  Rate,
  Skeleton,
  Space,
  message,
} from 'antd'
import { LikeFilled, LikeOutlined } from '@ant-design/icons'
import { Link, useParams } from 'react-router-dom'
import { reviewApi } from '../../api/review'
import { interactionApi } from '../../api/interaction'
import type { CommentVO, PageResult, ReviewVO } from '../../types/api'
import { DEFAULT_PAGE, DEFAULT_SIZE } from '../../utils/constants'
import { useAuthStore } from '../../store/authStore'
import PullQuote from '../../components/editorial/PullQuote'
import SectionTitle from '../../components/editorial/SectionTitle'
import { palette } from '../../styles/tokens'

const { TextArea } = Input

export default function ReviewDetailPage() {
  const { id } = useParams<{ id: string }>()
  const reviewId = Number(id)
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn)

  const [review, setReview] = useState<ReviewVO | null>(null)
  const [loading, setLoading] = useState(false)
  const [likeLoading, setLikeLoading] = useState(false)

  const [comments, setComments] = useState<PageResult<CommentVO> | null>(null)
  const [commentPage, setCommentPage] = useState(DEFAULT_PAGE)
  const [commentVersion, setCommentVersion] = useState(0)
  const [commentLoading, setCommentLoading] = useState(false)
  const [commentText, setCommentText] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // 加载点评详情
  useEffect(() => {
    if (!reviewId) return
    setLoading(true)
    reviewApi
      .get(reviewId)
      .then(setReview)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [reviewId])

  // 加载评论列表（翻页或提交后刷新）
  useEffect(() => {
    if (!reviewId) return
    setCommentLoading(true)
    interactionApi
      .comments(reviewId, { page: commentPage, size: DEFAULT_SIZE })
      .then(setComments)
      .catch(() => setComments(null))
      .finally(() => setCommentLoading(false))
  }, [reviewId, commentPage, commentVersion])

  const toggleLike = async () => {
    if (!review) return
    if (!isLoggedIn) {
      message.warning('请先登录')
      return
    }
    const liked = review.hasLiked
    setLikeLoading(true)
    // 乐观更新
    setReview({
      ...review,
      hasLiked: !liked,
      likeCount: Math.max(0, review.likeCount + (liked ? -1 : 1)),
    })
    try {
      const res = liked ? await interactionApi.unlike(reviewId) : await interactionApi.like(reviewId)
      setReview((r) => (r ? { ...r, likeCount: res.likeCount, hasLiked: !liked } : r))
    } catch {
      // 回滚
      setReview((r) => (r ? { ...r, hasLiked: liked, likeCount: review.likeCount } : r))
    } finally {
      setLikeLoading(false)
    }
  }

  const submitComment = async () => {
    const text = commentText.trim()
    if (!text || !isLoggedIn) {
      if (!isLoggedIn) message.warning('请先登录')
      return
    }
    setSubmitting(true)
    try {
      await interactionApi.createComment(reviewId, { content: text })
      message.success('评论成功')
      setCommentText('')
      if (review) setReview({ ...review, replyCount: review.replyCount + 1 })
      setCommentPage(DEFAULT_PAGE)
      setCommentVersion((v) => v + 1)
    } catch {
      // 错误提示已由 request 拦截器统一处理
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <Skeleton active />
  if (!review) return <Empty description="点评不存在" />

  return (
    <div>
      <Card className="tl-card">
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 4,
          }}
        >
          <Space size={10} align="center">
            <Link to={`/users/${review.userId}`}>
              <Avatar src={review.userAvatarUrl} size={32}>
                {review.userNickname?.[0]}
              </Avatar>
            </Link>
            <Link to={`/users/${review.userId}`} style={{ fontWeight: 600, color: palette.ink }}>
              {review.userNickname}
            </Link>
          </Space>
          <Rate disabled value={review.rating} style={{ fontSize: 14 }} />
        </div>

        <Link
          to={`/shops/${review.shopId}`}
          style={{ fontSize: 13, color: palette.gold, fontWeight: 600 }}
        >
          {review.shopName}
        </Link>

        <PullQuote style={{ margin: '14px 0 6px' }}>
          <span style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{review.content}</span>
        </PullQuote>

        {review.images?.length ? (
          <AntImage.PreviewGroup>
            <Space size={8} wrap>
              {review.images.map((url, i) => (
                <AntImage
                  key={i}
                  src={url}
                  width={120}
                  height={120}
                  style={{ objectFit: 'cover', borderRadius: 8 }}
                />
              ))}
            </Space>
          </AntImage.PreviewGroup>
        ) : null}

        <Space size={16} style={{ color: palette.muted, marginTop: 14 }}>
          <Button
            type={review.hasLiked ? 'primary' : 'default'}
            shape="round"
            icon={review.hasLiked ? <LikeFilled /> : <LikeOutlined />}
            loading={likeLoading}
            onClick={toggleLike}
          >
            {review.likeCount} 赞
          </Button>
          <span>{review.replyCount} 评论</span>
          <span>{review.createTime}</span>
        </Space>
      </Card>

      <Card className="tl-card" style={{ marginTop: 20 }}>
        <SectionTitle eyebrow="COMMENTS" size="sm">
          评论 ({review.replyCount})
        </SectionTitle>
        <div style={{ marginBottom: 16 }}>
          <TextArea
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            rows={2}
            placeholder={isLoggedIn ? '说点什么…' : '登录后可评论'}
            disabled={!isLoggedIn}
            maxLength={200}
            showCount
          />
          <Button
            type="primary"
            shape="round"
            style={{ marginTop: 8 }}
            loading={submitting}
            disabled={!isLoggedIn || !commentText.trim()}
            onClick={submitComment}
          >
            发表评论
          </Button>
        </div>

        {commentLoading ? (
          <Skeleton active />
        ) : !comments?.records?.length ? (
          <Empty description="暂无评论" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {comments.records.map((c) => (
              <Space key={c.id} align="start" size={10}>
                <Link to={`/users/${c.userId}`}>
                  <Avatar size="small" src={c.userAvatarUrl}>
                    {c.userNickname?.[0]}
                  </Avatar>
                </Link>
                <div>
                  <div>
                    <Link to={`/users/${c.userId}`} style={{ fontWeight: 500, marginRight: 8 }}>
                      {c.userNickname}
                    </Link>
                    <span style={{ color: palette.muted, fontSize: 12 }}>{c.createTime}</span>
                  </div>
                  <div style={{ color: palette.ink }}>{c.content}</div>
                </div>
              </Space>
            ))}
          </div>
        )}
        {comments && comments.total > DEFAULT_SIZE ? (
          <Pagination
            size="small"
            style={{ marginTop: 16, textAlign: 'center' }}
            current={commentPage}
            pageSize={DEFAULT_SIZE}
            total={comments.total}
            onChange={setCommentPage}
            showSizeChanger={false}
          />
        ) : null}
      </Card>
    </div>
  )
}
