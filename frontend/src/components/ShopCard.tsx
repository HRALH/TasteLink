import { Card, Rate } from 'antd'
import { EnvironmentOutlined, MessageOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import type { ShopVO } from '../types/api'
import { palette } from '../styles/tokens'
import RankBadge from './editorial/RankBadge'

interface ShopCardProps {
  shop: ShopVO
  /** 榜单排名（仅传给“排序即信息”的热门榜，如 1/2/3）；普通列表不传 */
  rank?: number
}

/** 店铺卡片：菜单卡 —— 封面 + 暖金分类 eyebrow + 宋体店名 + 发丝线 + 评分批注 */
export default function ShopCard({ shop, rank }: ShopCardProps) {
  return (
    <Link to={`/shops/${shop.id}`} className="tl-rise" style={{ display: 'block', height: '100%' }}>
      <Card
        className="tl-card"
        cover={
          shop.coverUrl ? (
            <img src={shop.coverUrl} alt={shop.name} style={{ height: 160, objectFit: 'cover' }} />
          ) : (
            <div style={{ height: 160, background: 'linear-gradient(135deg, #f3ebdb, #efe3cc)' }} />
          )
        }
        styles={{ body: { padding: 14 } }}
        style={{ height: '100%' }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
          <span className="eyebrow" style={{ fontSize: 11 }}>
            {shop.categoryName}
          </span>
          {rank ? <RankBadge rank={rank} /> : null}
        </div>
        <div className="editorial-title" style={{ fontSize: 18, marginBottom: 8, lineHeight: 1.3 }}>
          {shop.name}
        </div>
        <hr className="hairline" style={{ marginBottom: 10 }} />
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            color: palette.muted,
            fontSize: 13,
          }}
        >
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, minWidth: 0 }}>
            <EnvironmentOutlined />
            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{shop.city}</span>
          </span>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            <MessageOutlined /> {shop.reviewCount}
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 10 }}>
          <Rate disabled allowHalf value={shop.avgRating} style={{ fontSize: 14 }} />
          <span style={{ fontSize: 13, fontWeight: 600, color: palette.appetite }}>
            {shop.avgRating.toFixed(1)}
          </span>
        </div>
      </Card>
    </Link>
  )
}
