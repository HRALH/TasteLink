import { useEffect, useState } from 'react'
import {
  Button,
  Card,
  Empty,
  Pagination,
  Rate,
  Segmented,
  Skeleton,
  Space,
} from 'antd'
import {
  EditOutlined,
  EnvironmentOutlined,
  LikeOutlined,
  MessageOutlined,
  PhoneOutlined,
} from '@ant-design/icons'
import { Link, useParams } from 'react-router-dom'
import ReviewCard from '../../components/ReviewCard'
import SectionTitle from '../../components/editorial/SectionTitle'
import Eyebrow from '../../components/editorial/Eyebrow'
import { shopApi } from '../../api/shop'
import { palette } from '../../styles/tokens'
import type { PageResult, ReviewVO, ShopDetailVO } from '../../types/api'
import { DEFAULT_PAGE, DEFAULT_SIZE, ReviewSort } from '../../utils/constants'

export default function ShopDetailPage() {
  const { id } = useParams<{ id: string }>()
  const shopId = Number(id)

  const [shop, setShop] = useState<ShopDetailVO | null>(null)
  const [loadingShop, setLoadingShop] = useState(false)

  const [sortBy, setSortBy] = useState<string>(ReviewSort.TIME)
  const [page, setPage] = useState(DEFAULT_PAGE)
  const [reviews, setReviews] = useState<PageResult<ReviewVO> | null>(null)
  const [loadingReviews, setLoadingReviews] = useState(false)

  useEffect(() => {
    if (!shopId) return
    setLoadingShop(true)
    shopApi
      .detail(shopId)
      .then(setShop)
      .catch(() => {})
      .finally(() => setLoadingShop(false))
  }, [shopId])

  useEffect(() => {
    if (!shopId) return
    setLoadingReviews(true)
    shopApi
      .reviews(shopId, { sortBy, page, size: DEFAULT_SIZE })
      .then(setReviews)
      .catch(() => setReviews(null))
      .finally(() => setLoadingReviews(false))
  }, [shopId, sortBy, page])

  if (loadingShop) return <Skeleton active />
  if (!shop) return <Empty description="店铺不存在或已下架" />

  return (
    <div>
      <Card className="tl-card" style={{ marginBottom: 24, overflow: 'hidden' }} styles={{ body: { padding: 0 } }}>
        {shop.coverUrl && (
          <div style={{ position: 'relative' }}>
            <img
              src={shop.coverUrl}
              alt={shop.name}
              style={{ width: '100%', height: 260, objectFit: 'cover', display: 'block' }}
            />
            <div
              style={{
                position: 'absolute',
                inset: 0,
                background: 'linear-gradient(180deg, rgba(36,26,20,0) 40%, rgba(36,26,20,0.55))',
              }}
            />
          </div>
        )}
        <div style={{ padding: 20 }}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex-start',
              flexWrap: 'wrap',
              gap: 12,
            }}
          >
            <div>
              <Eyebrow>{shop.categoryName}</Eyebrow>
              <h1 className="editorial-title" style={{ fontSize: 28, margin: '6px 0 8px' }}>
                {shop.name}
              </h1>
              <div
                style={{
                  color: palette.muted,
                  fontSize: 14,
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                }}
              >
                <EnvironmentOutlined /> {shop.city} · {shop.address}
              </div>
            </div>
            <Space direction="vertical" align="end" size={12}>
              <Space align="center">
                <Rate disabled allowHalf value={shop.avgRating} style={{ fontSize: 18 }} />
                <span style={{ fontSize: 18, fontWeight: 700, color: palette.appetite }}>
                  {shop.avgRating.toFixed(1)}
                </span>
              </Space>
              <Space size={16} style={{ color: palette.muted, fontSize: 13 }}>
                <span>
                  <MessageOutlined /> {shop.reviewCount} 点评
                </span>
                <span>
                  <LikeOutlined /> {shop.likeCount} 赞
                </span>
              </Space>
              <Link to={`/shops/${shopId}/review`}>
                <Button type="primary" shape="round" icon={<EditOutlined />}>
                  写点评
                </Button>
              </Link>
            </Space>
          </div>
          {shop.description ? (
            <p style={{ marginTop: 14, color: palette.ink, lineHeight: 1.7, marginBottom: 0 }}>
              {shop.description}
            </p>
          ) : null}
          {shop.phone ? (
            <div
              style={{
                marginTop: 10,
                color: palette.muted,
                fontSize: 13,
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
              }}
            >
              <PhoneOutlined /> {shop.phone}
            </div>
          ) : null}
        </div>
      </Card>

      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 14,
        }}
      >
        <SectionTitle eyebrow="REVIEWS" size="md" style={{ marginBottom: 0 }}>
          点评
        </SectionTitle>
        <Segmented
          options={[
            { label: '最新', value: ReviewSort.TIME },
            { label: '最热', value: ReviewSort.LIKE },
          ]}
          value={sortBy}
          onChange={(v) => {
            setSortBy(v as string)
            setPage(DEFAULT_PAGE)
          }}
        />
      </div>

      {loadingReviews ? (
        <Skeleton active />
      ) : !reviews?.records?.length ? (
        <Empty description="暂无点评，快来抢沙发" />
      ) : (
        <>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {reviews.records.map((r) => (
              <ReviewCard key={r.id} review={r} showShop={false} />
            ))}
          </div>
          <Pagination
            style={{ marginTop: 24, textAlign: 'center' }}
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
