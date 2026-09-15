import { Card, Rate } from 'antd'
import { EnvironmentOutlined, MessageOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import type { ShopVO } from '../types/api'
import { palette } from '../styles/tokens'
import { thumb } from '../utils/thumb'
import RankBadge from './editorial/RankBadge'

interface ShopCardProps {
  shop: ShopVO
  /** 榜单排名（仅传给“排序即信息”的热门榜，如 1/2/3）；普通列表不传 */
  rank?: number
}

/** 店铺卡片：菜单卡 —— 封面 + 暖金分类 eyebrow + 宋体店名 + 发丝线 + 评分批注 */
export default function ShopCard({ shop, rank }: ShopCardProps) {
  return (
    <Link to={`/shops/${shop.id}`} style={{ display: 'block', height: '100%' }}>
      <Card
        className="tl-card tl-zoomable"
        cover={
          shop.coverUrl ? (
            <img
              src={thumb(shop.coverUrl, 640)}
              alt={shop.name}
              loading="lazy"
              decoding="async"
              style={{ width: '100%', height: 160, objectFit: 'cover' }}
            />
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
            <MessageOutlined /> {shop.reviewCount > 0 ? `${shop.reviewCount} 点评` : '暂无点评'}
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 10 }}>
          {shop.avgRating > 0 ? (
            <>
              <Rate disabled allowHalf value={shop.avgRating} style={{ fontSize: 14 }} />
              <span style={{ fontSize: 13, fontWeight: 600, color: palette.appetite }}>
                {shop.avgRating.toFixed(1)}
              </span>
            </>
          ) : (
            // 无点评店不应显示"0.0"空星（失真）——展示「暂无评分」，与评分缺失语义一致
            <span style={{ fontSize: 13, color: palette.muted }}>暂无评分</span>
          )}
        </div>
      </Card>
    </Link>
  )
}
