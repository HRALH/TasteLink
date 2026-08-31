import { useEffect, useState } from 'react'
import { Col, Empty, Row, Segmented, Skeleton, Typography } from 'antd'
import { homeApi } from '../../api/home'
import ShopCard from '../../components/ShopCard'
import ReviewCard from '../../components/ReviewCard'
import type { HomeVO } from '../../types/api'
import { CITIES } from '../../utils/constants'

const { Title } = Typography

export default function HomePage() {
  const [city, setCity] = useState<string | undefined>(undefined)
  const [data, setData] = useState<HomeVO | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    homeApi
      .home(city)
      .then(setData)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [city])

  const cityOptions = [{ label: '全部', value: '' }, ...CITIES.map((c) => ({ label: c, value: c }))]

  return (
    <div>
      <div
        style={{
          marginBottom: 16,
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          flexWrap: 'wrap',
        }}
      >
        <Title level={4} style={{ margin: 0 }}>
          热门推荐
        </Title>
        <Segmented
          options={cityOptions}
          value={city ?? ''}
          onChange={(v) => setCity((v as string) || undefined)}
        />
      </div>

      <Title level={5}>🔥 热门店铺</Title>
      {loading ? (
        <Skeleton active />
      ) : !data?.hotShops?.length ? (
        <Empty description="暂无热门店铺" />
      ) : (
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          {data.hotShops.map((s) => (
            <Col key={s.id} xs={24} sm={12} md={8}>
              <ShopCard shop={s} />
            </Col>
          ))}
        </Row>
      )}

      <Title level={5}>👍 热门点评</Title>
      {loading ? (
        <Skeleton active />
      ) : !data?.hotReviews?.length ? (
        <Empty description="暂无热门点评" />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {data.hotReviews.map((r) => (
            <ReviewCard key={r.id} review={r} />
          ))}
        </div>
      )}
    </div>
  )
}
