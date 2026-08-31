import { Card, Rate, Typography } from 'antd'
import { EnvironmentOutlined, MessageOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import type { ShopVO } from '../types/api'

const { Text } = Typography

/** 店铺卡片：封面 + 名称 + 城市/分类 + 平均评分 + 点评数 */
export default function ShopCard({ shop }: { shop: ShopVO }) {
  return (
    <Link to={`/shops/${shop.id}`}>
      <Card
        hoverable
        cover={
          shop.coverUrl ? (
            <img src={shop.coverUrl} alt={shop.name} style={{ height: 160, objectFit: 'cover' }} />
          ) : undefined
        }
        styles={{ body: { padding: 12 } }}
      >
        <div style={{ fontWeight: 600, fontSize: 16, marginBottom: 4 }}>{shop.name}</div>
        <div style={{ color: '#888', fontSize: 13, marginBottom: 8 }}>
          <EnvironmentOutlined /> {shop.city} · {shop.categoryName}
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Rate disabled allowHalf value={shop.avgRating} style={{ fontSize: 14 }} />
          <Text type="secondary">
            <MessageOutlined /> {shop.reviewCount}点评
          </Text>
        </div>
      </Card>
    </Link>
  )
}
