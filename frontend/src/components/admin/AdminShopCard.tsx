import { Button, Card, Rate } from 'antd'
import { DeleteOutlined, EditOutlined, EnvironmentOutlined, MessageOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import type { ShopVO } from '../../types/api'
import { palette } from '../../styles/tokens'
import { thumb } from '../../utils/thumb'

interface AdminShopCardProps {
  shop: ShopVO
  onEdit: (shopId: number) => void
  onDelete: (shop: ShopVO) => void
}

/**
 * 管理员列表用的店铺卡（v2 FE-B）。
 * 不复用 ShopCard（其整卡是 Link、无操作位）；此处标题仍链到公开详情预览，
 * 底部操作区提供「编辑 / 删除」（删除用 appetite 语义红，与品牌色一致，非 AntD 默认红）。
 */
export default function AdminShopCard({ shop, onEdit, onDelete }: AdminShopCardProps) {
  return (
    <Card
      className="tl-card"
      cover={
        shop.coverUrl ? (
          <img
            src={thumb(shop.coverUrl, 640)}
            alt={shop.name}
            loading="lazy"
            decoding="async"
            style={{ width: '100%', height: 140, objectFit: 'cover' }}
          />
        ) : (
          <div style={{ height: 140, background: 'linear-gradient(135deg, #f3ebdb, #efe3cc)' }} />
        )
      }
      styles={{ body: { padding: 14 } }}
      style={{ height: '100%' }}
    >
      <span className="eyebrow" style={{ fontSize: 11, display: 'block', marginBottom: 4 }}>
        {shop.categoryName}
      </span>
      <Link
        to={`/shops/${shop.id}`}
        className="editorial-title"
        style={{ fontSize: 17, lineHeight: 1.3, display: 'block', color: palette.ink }}
      >
        {shop.name}
      </Link>
      <hr className="hairline" style={{ marginTop: 10, marginBottom: 8 }} />
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
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 8 }}>
        <Rate disabled allowHalf value={shop.avgRating} style={{ fontSize: 13 }} />
        <span style={{ fontSize: 13, fontWeight: 600, color: palette.appetite }}>{shop.avgRating.toFixed(1)}</span>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 14 }}>
        <Button
          type="primary"
          shape="round"
          icon={<EditOutlined />}
          onClick={() => onEdit(shop.id)}
          className="tl-press"
          style={{ flex: 1 }}
        >
          编辑
        </Button>
        <Button
          shape="round"
          icon={<DeleteOutlined />}
          onClick={() => onDelete(shop)}
          className="tl-press"
          style={{ color: palette.appetite, borderColor: palette.appetite }}
        >
          删除
        </Button>
      </div>
    </Card>
  )
}
