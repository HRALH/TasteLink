import { Avatar, Card, Image as AntImage, Rate, Space, Typography } from 'antd'
import { LikeOutlined, MessageOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import type { ReviewVO } from '../types/api'

const { Paragraph } = Typography

/** 点评卡片：作者 + 评分 + 文字 + 图片预览 + 点赞/评论数 + 详情入口 */
export default function ReviewCard({
  review,
  showShop = true,
}: {
  review: ReviewVO
  showShop?: boolean
}) {
  return (
    <Card styles={{ body: { padding: 16 } }}>
      <Space align="start" size={12}>
        <Link to={`/users/${review.userId}`}>
          <Avatar src={review.userAvatarUrl}>{review.userNickname?.[0]}</Avatar>
        </Link>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Link to={`/users/${review.userId}`} style={{ fontWeight: 600 }}>
              {review.userNickname}
            </Link>
            <Rate disabled value={review.rating} style={{ fontSize: 14 }} />
          </div>
          {showShop && review.shopName ? (
            <Link to={`/shops/${review.shopId}`} style={{ fontSize: 13, color: '#ff6b35' }}>
              {review.shopName}
            </Link>
          ) : null}
          <Paragraph ellipsis={{ rows: 3 }} style={{ margin: '8px 0', color: '#333' }}>
            {review.content}
          </Paragraph>
          {review.images?.length ? (
            <AntImage.PreviewGroup>
              <Space size={6} wrap style={{ marginBottom: 4 }}>
                {review.images.slice(0, 4).map((url, i) => (
                  <AntImage
                    key={i}
                    src={url}
                    width={80}
                    height={80}
                    style={{ objectFit: 'cover', borderRadius: 4 }}
                  />
                ))}
              </Space>
            </AntImage.PreviewGroup>
          ) : null}
          <Space size={16} style={{ color: '#999', fontSize: 13, marginTop: 8 }}>
            <span>
              <LikeOutlined /> {review.likeCount}
            </span>
            <span>
              <MessageOutlined /> {review.replyCount}
            </span>
            <Link to={`/reviews/${review.id}`}>查看详情</Link>
          </Space>
        </div>
      </Space>
    </Card>
  )
}
