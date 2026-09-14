import { useState } from 'react'
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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { reviewApi } from '../../api/review'
import { interactionApi } from '../../api/interaction'
import QueryError from '../../components/QueryError'
import type { ReviewVO } from '../../types/api'
import { DEFAULT_PAGE, DEFAULT_SIZE } from '../../utils/constants'
import { useAuthStore } from '../../store/authStore'
import PullQuote from '../../components/editorial/PullQuote'
import SectionTitle from '../../components/editorial/SectionTitle'
import { Reveal } from '../../components/motion'
import { palette } from '../../styles/tokens'
import { thumb } from '../../utils/thumb'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'

const { TextArea } = Input

export default function ReviewDetailPage() {
  const { id } = useParams<{ id: string }>()
  const reviewId = Number(id)
  const reviewIdValid = !Number.isNaN(reviewId) && reviewId > 0
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn)
  const queryClient = useQueryClient()

  const [commentPage, setCommentPage] = useState(DEFAULT_PAGE)
  const [commentText, setCommentText] = useState('')
  // 点赞心爆重放计数：每次 toggleLike 自增，驱动 icon remount 重播 .tl-heartburst
  const [likeBurst, setLikeBurst] = useState(0)

  const reviewQuery = useQuery({
    queryKey: ['review', reviewId],
    queryFn: () => reviewApi.get(reviewId),
    enabled: reviewIdValid,
  })
  const commentsQuery = useQuery({
    queryKey: ['comments', reviewId, commentPage],
    queryFn: () => interactionApi.comments(reviewId, { page: commentPage, size: DEFAULT_SIZE }),
    enabled: reviewIdValid,
  })

  const review = reviewQuery.data
  const comments = commentsQuery.data

  // F5-3：详情页带点评内容摘要进文档标题（截断 20 字）
  useDocumentTitle(review ? review.content.slice(0, 20) : undefined)

  // 点赞：onMutate 快照乐观翻 → onError 回滚 → onSuccess 以服务端 likeCount 为准
  const likeMutation = useMutation({
    mutationFn: (liked: boolean) =>
      liked ? interactionApi.unlike(reviewId) : interactionApi.like(reviewId),
    onMutate: async (liked) => {
      await queryClient.cancelQueries({ queryKey: ['review', reviewId] })
      const prev = queryClient.getQueryData<ReviewVO>(['review', reviewId])
      queryClient.setQueryData<ReviewVO>(['review', reviewId], (r) =>
        r ? { ...r, hasLiked: !liked, likeCount: Math.max(0, r.likeCount + (liked ? -1 : 1)) } : r,
      )
      setLikeBurst((b) => b + 1)
      return { prev }
    },
    onError: (_e, _liked, ctx) => {
      if (ctx?.prev) queryClient.setQueryData(['review', reviewId], ctx.prev)
    },
    onSuccess: (res, liked) => {
      queryClient.setQueryData<ReviewVO>(['review', reviewId], (r) =>
        r ? { ...r, likeCount: res.likeCount, hasLiked: !liked } : r,
      )
    },
  })

  const commentMutation = useMutation({
    mutationFn: (text: string) => interactionApi.createComment(reviewId, { content: text }),
    onSuccess: () => {
      message.success('评论成功')
      setCommentText('')
      queryClient.setQueryData<ReviewVO>(['review', reviewId], (r) =>
        r ? { ...r, replyCount: r.replyCount + 1 } : r,
      )
      setCommentPage(DEFAULT_PAGE)
      queryClient.invalidateQueries({ queryKey: ['comments', reviewId] })
    },
    // 错误提示已由 request 拦截器统一处理
  })

  const toggleLike = () => {
    if (!review) return
    if (!isLoggedIn) {
      message.warning('请先登录')
      return
    }
    likeMutation.mutate(review.hasLiked)
  }

  const submitComment = () => {
    const text = commentText.trim()
    if (!text || !isLoggedIn) {
      if (!isLoggedIn) message.warning('请先登录')
      return
    }
    commentMutation.mutate(text)
  }

  if (!reviewIdValid) return <Empty description="点评不存在" />
  if (reviewQuery.isPending) return <Skeleton active />
  if (reviewQuery.isError) return <QueryError onRetry={() => reviewQuery.refetch()} />
  if (!review) return <Empty description="点评不存在" />

  return (
    <div>
      <Reveal>
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
                  src={thumb(url, 240)}
                  alt={`${review.userNickname}的点评图片 ${i + 1}`}
                  width={120}
                  height={120}
                  preview={{ src: url }}
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
            icon={
              <span key={likeBurst} className={likeBurst ? 'tl-heartburst' : undefined}>
                {review.hasLiked ? <LikeFilled /> : <LikeOutlined />}
              </span>
            }
            loading={likeMutation.isPending}
            onClick={toggleLike}
            className="tl-press"
          >
            {review.likeCount} 赞
          </Button>
          <span>{review.replyCount} 评论</span>
          <span>{review.createTime}</span>
        </Space>
      </Card>
      </Reveal>

      <Reveal style={{ marginTop: 20 }}>
      <Card className="tl-card">
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
            loading={commentMutation.isPending}
            disabled={!isLoggedIn || !commentText.trim()}
            onClick={submitComment}
            className="tl-press"
          >
            发表评论
          </Button>
        </div>

        {commentsQuery.isPending ? (
          <Skeleton active />
        ) : commentsQuery.isError ? (
          <QueryError onRetry={() => commentsQuery.refetch()} />
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
      </Reveal>
    </div>
  )
}
