import { useState } from 'react'
import {
  Avatar,
  Button,
  Card,
  Col,
  Empty,
  Pagination,
  Row,
  Skeleton,
  Space,
  Statistic,
  Typography,
} from 'antd'
import { Link, useParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import ReviewCard from '../../components/ReviewCard'
import FollowButton from '../../components/FollowButton'
import QueryError from '../../components/QueryError'
import SectionTitle from '../../components/editorial/SectionTitle'
import { Reveal } from '../../components/motion'
import { staggerDelay } from '../../utils/motion'
import { userApi } from '../../api/user'
import { palette } from '../../styles/tokens'
import type { UserVO } from '../../types/api'
import { DEFAULT_PAGE, DEFAULT_SIZE } from '../../utils/constants'
import { useAuthStore } from '../../store/authStore'

const { Paragraph } = Typography

export default function UserHomePage() {
  const { id } = useParams<{ id: string }>()
  const userId = Number(id)
  const userIdValid = !Number.isNaN(userId) && userId > 0
  const me = useAuthStore((s) => s.userInfo)
  const isMe = me?.userId === userId
  const queryClient = useQueryClient()

  const [page, setPage] = useState(DEFAULT_PAGE)

  const userQuery = useQuery({
    queryKey: ['user', userId],
    queryFn: () => userApi.get(userId),
    enabled: userIdValid,
  })
  const reviewsQuery = useQuery({
    queryKey: ['userReviews', userId, page],
    queryFn: () => userApi.reviews(userId, { page, size: DEFAULT_SIZE }),
    enabled: userIdValid,
  })

  const user = userQuery.data
  const reviews = reviewsQuery.data

  if (!userIdValid) return <Empty description="用户不存在" />
  if (userQuery.isPending) return <Skeleton avatar active />
  if (userQuery.isError) return <QueryError onRetry={() => userQuery.refetch()} />
  if (!user) return <Empty description="用户不存在" />

  return (
    <div>
      <Reveal style={{ marginBottom: 24 }}>
        <Card className="tl-card">
          <Space size={20} align="start" wrap>
            <Avatar size={80} src={user.avatarUrl} style={{ background: palette.rule }}>
              {user.nickname?.[0]}
            </Avatar>
            <div style={{ flex: 1, minWidth: 200 }}>
              <h1 className="editorial-title" style={{ fontSize: 24, margin: '0 0 4px' }}>
                {user.nickname}
              </h1>
              <Paragraph style={{ color: palette.muted, margin: 0 }}>
                {user.bio || '这个人很神秘，什么都没留下'}
              </Paragraph>
              <Row gutter={36} style={{ marginTop: 16 }}>
                <Col>
                  <Link to={`/users/${userId}/followings`}>
                    <Statistic title="关注" value={user.followingCount} />
                  </Link>
                </Col>
                <Col>
                  <Link to={`/users/${userId}/followers`}>
                    <Statistic title="粉丝" value={user.followerCount} />
                  </Link>
                </Col>
                <Col>
                  <Statistic title="点评" value={user.reviewCount} />
                </Col>
              </Row>
            </div>
            {isMe ? (
              <Link to="/me">
                <Button shape="round">编辑资料</Button>
              </Link>
            ) : (
              <FollowButton
                key={user.id}
                userId={user.id}
                hasFollowed={user.hasFollowed}
                onChange={(f) =>
                  queryClient.setQueryData<UserVO>(['user', userId], (u) =>
                    u
                      ? { ...u, hasFollowed: f, followerCount: u.followerCount + (f ? 1 : -1) }
                      : u,
                  )
                }
              />
            )}
          </Space>
        </Card>
      </Reveal>

      <Reveal>
        <SectionTitle eyebrow="REVIEWS">Ta的点评</SectionTitle>
      </Reveal>
      {reviewsQuery.isPending ? (
        <Skeleton active />
      ) : reviewsQuery.isError ? (
        <QueryError onRetry={() => reviewsQuery.refetch()} />
      ) : !reviews?.records?.length ? (
        <Empty description="暂无点评" />
      ) : (
        <>
          <div
            style={{ display: 'flex', flexDirection: 'column', gap: 16 }}
            key={`reviews-${page}`}
          >
            {reviews.records.map((r, i) => (
              <Reveal key={r.id} delay={staggerDelay(i)}>
                <ReviewCard review={r} />
              </Reveal>
            ))}
          </div>
          <Pagination
            style={{ marginTop: 20, textAlign: 'center' }}
            current={page}
            pageSize={DEFAULT_SIZE}
            total={reviews.total}
            onChange={(p) => {
              setPage(p)
              window.scrollTo({ top: 0 })
            }}
            showSizeChanger={false}
          />
        </>
      )}
    </div>
  )
}
