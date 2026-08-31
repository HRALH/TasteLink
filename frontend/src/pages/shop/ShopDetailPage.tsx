import { useEffect, useState } from 'react'
import {
  Button,
  Card,
  Descriptions,
  Empty,
  Pagination,
  Rate,
  Segmented,
  Skeleton,
  Space,
  Tag,
  Typography,
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
import { shopApi } from '../../api/shop'
import type { PageResult, ReviewVO, ShopDetailVO } from '../../types/api'
import { DEFAULT_PAGE, DEFAULT_SIZE, ReviewSort } from '../../utils/constants'

const { Title, Paragraph } = Typography

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
      <Card style={{ marginBottom: 16 }}>
        {shop.coverUrl && (
          <img
            src={shop.coverUrl}
            alt={shop.name}
            style={{ width: '100%', height: 240, objectFit: 'cover', borderRadius: 8, marginBottom: 16 }}
          />
        )}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 8 }}>
          <div>
            <Title level={4} style={{ marginBottom: 4 }}>
              {shop.name}
            </Title>
            <Space size={8} wrap>
              <Tag color="orange">{shop.categoryName}</Tag>
              <span style={{ color: '#888' }}>
                <EnvironmentOutlined /> {shop.city} · {shop.address}
              </span>
            </Space>
          </div>
          <Space direction="vertical" align="end">
            <Space>
              <Rate disabled allowHalf value={shop.avgRating} />
              <span>{shop.avgRating.toFixed(1)}分</span>
            </Space>
            <Space size={16} style={{ color: '#888' }}>
              <span>
                <MessageOutlined /> {shop.reviewCount}点评
              </span>
              <span>
                <LikeOutlined /> {shop.likeCount}赞
              </span>
            </Space>
            <Link to={`/shops/${shopId}/review`}>
              <Button type="primary" icon={<EditOutlined />}>
                写点评
              </Button>
            </Link>
          </Space>
        </div>
        {shop.description ? (
          <Paragraph style={{ marginTop: 12, color: '#555' }}>{shop.description}</Paragraph>
        ) : null}
        {shop.phone ? (
          <Descriptions size="small" column={1} style={{ marginTop: 8 }}>
            <Descriptions.Item label="电话">
              <PhoneOutlined /> {shop.phone}
            </Descriptions.Item>
          </Descriptions>
        ) : null}
      </Card>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <Title level={5} style={{ margin: 0 }}>
          点评
        </Title>
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
            style={{ marginTop: 16, textAlign: 'center' }}
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
