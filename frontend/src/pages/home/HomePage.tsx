import { useEffect, useState } from 'react'
import { Button, Col, Empty, Row, Segmented, Skeleton } from 'antd'
import { Link } from 'react-router-dom'
import { homeApi } from '../../api/home'
import ShopCard from '../../components/ShopCard'
import ReviewCard from '../../components/ReviewCard'
import SectionTitle from '../../components/editorial/SectionTitle'
import Eyebrow from '../../components/editorial/Eyebrow'
import { Reveal } from '../../components/motion'
import { staggerDelay } from '../../utils/motion'
import { palette } from '../../styles/tokens'
import type { HomeVO } from '../../types/api'
import { CITIES } from '../../utils/constants'

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
  const cityKey = city ?? 'all'

  return (
    <div>
      {/* 编辑式 Hero：入场逐行浮起 */}
      <section style={{ marginBottom: 32 }}>
        <Reveal>
          <Eyebrow>TASTELINK · 餐饮口碑社区</Eyebrow>
        </Reveal>
        <Reveal delay={staggerDelay(1, 80, 6)}>
          <h1
            className="editorial-title"
            style={{ fontSize: 34, margin: '8px 0 6px', maxWidth: 720, lineHeight: 1.2 }}
          >
            用文字，留住每一口滋味。
          </h1>
        </Reveal>
        <Reveal delay={staggerDelay(2, 80, 6)}>
          <p style={{ color: palette.muted, fontSize: 15, margin: 0, maxWidth: 620, lineHeight: 1.6 }}>
            记录每一次值得回味的用餐，遇见同好，分享真实口碑。
          </p>
        </Reveal>
        <Reveal delay={staggerDelay(3, 80, 6)} style={{ marginTop: 18 }}>
          <Link to="/shops">
            <Button type="primary" shape="round" className="tl-press">
              探索店铺
            </Button>
          </Link>
        </Reveal>
      </section>

      {/* 城市筛选 + 章节分隔 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 12,
          flexWrap: 'wrap',
          marginBottom: 20,
          paddingBottom: 12,
          borderBottom: `1px solid ${palette.rule}`,
        }}
      >
        <SectionTitle eyebrow="TRENDING" size="md" style={{ marginBottom: 0 }}>
          热门推荐
        </SectionTitle>
        <Segmented
          options={cityOptions}
          value={city ?? ''}
          onChange={(v) => setCity((v as string) || undefined)}
        />
      </div>

      <Reveal>
        <SectionTitle eyebrow="HOT SHOPS">热门店铺</SectionTitle>
      </Reveal>
      {loading ? (
        <Skeleton active />
      ) : !data?.hotShops?.length ? (
        <Empty description="暂无热门店铺" />
      ) : (
        <Row gutter={[16, 16]} style={{ marginBottom: 32 }} key={`shops-${cityKey}`}>
          {data.hotShops.map((s, i) => (
            <Col key={s.id} xs={24} sm={12} md={8}>
              <Reveal delay={staggerDelay(i)} style={{ height: '100%' }}>
                <ShopCard shop={s} rank={i + 1} />
              </Reveal>
            </Col>
          ))}
        </Row>
      )}

      <Reveal>
        <SectionTitle eyebrow="HOT REVIEWS">热门点评</SectionTitle>
      </Reveal>
      {loading ? (
        <Skeleton active />
      ) : !data?.hotReviews?.length ? (
        <Empty description="暂无热门点评" />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }} key={`reviews-${cityKey}`}>
          {data.hotReviews.map((r, i) => (
            <Reveal key={r.id} delay={staggerDelay(i)}>
              <ReviewCard review={r} />
            </Reveal>
          ))}
        </div>
      )}
    </div>
  )
}
