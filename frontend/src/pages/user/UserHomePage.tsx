import { useEffect, useState } from 'react'
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
import ReviewCard from '../../components/ReviewCard'
import FollowButton from '../../components/FollowButton'
import SectionTitle from '../../components/editorial/SectionTitle'
import { Reveal } from '../../components/motion'
import { staggerDelay } from '../../utils/motion'
import { userApi } from '../../api/user'
import { palette } from '../../styles/tokens'
import type { PageResult, ReviewVO, UserVO } from '../../types/api'
import { DEFAULT_PAGE, DEFAULT_SIZE } from '../../utils/constants'
import { useAuthStore } from '../../store/authStore'

const { Paragraph } = Typography

export default function UserHomePage() {
  const { id } = useParams<{ id: string }>()
  const userId = Number(id)
  const me = useAuthStore((s) => s.userInfo)
  const isMe = me?.userId === userId

  const [user, setUser] = useState<UserVO | null>(null)
  const [loading, setLoading] = useState(false)
  const [reviews, setReviews] = useState<PageResult<ReviewVO> | null>(null)
  const [page, setPage] = useState(DEFAULT_PAGE)
  const [loadingReviews, setLoadingReviews] = useState(false)

  useEffect(() => {
    if (!userId) return
    setLoading(true)
    userApi
      .get(userId)
      .then(setUser)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [userId])

  useEffect(() => {
    if (!userId) return
    setLoadingReviews(true)
    userApi
      .reviews(userId, { page, size: DEFAULT_SIZE })
      .then(setReviews)
      .catch(() => setReviews(null))
      .finally(() => setLoadingReviews(false))
  }, [userId, page])

  if (loading) return <Skeleton avatar active />
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
                userId={user.id}
                hasFollowed={user.hasFollowed}
                onChange={(f) =>
                  setUser((u) =>
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
      {loadingReviews ? (
        <Skeleton active />
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
            onChange={setPage}
            showSizeChanger={false}
          />
        </>
      )}
    </div>
  )
}
