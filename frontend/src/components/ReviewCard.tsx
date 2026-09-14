import { Avatar, Card, Image as AntImage, Rate, Space } from 'antd'
import { LikeOutlined, MessageOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import type { ReviewVO } from '../types/api'
import { palette } from '../styles/tokens'
import { thumb } from '../utils/thumb'

/** 点评卡片：杂志引文 —— 署名 + 评分 + 宋体引文正文 + 图片 + 点赞/评论/详情 */
export default function ReviewCard({
  review,
  showShop = true,
}: {
  review: ReviewVO
  showShop?: boolean
}) {
  return (
    <Card className="tl-card" styles={{ body: { padding: 18 } }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 4,
        }}
      >
        <Space size={10} align="center">
          <Link to={`/users/${review.userId}`}>
            <Avatar src={review.userAvatarUrl} size={32}>
              {review.userNickname?.[0]}
            </Avatar>
          </Link>
          <Link to={`/users/${review.userId}`} style={{ fontWeight: 600, color: palette.ink }}>
            {review.userNickname}
          </Link>
        </Space>
        <Rate disabled value={review.rating} style={{ fontSize: 14 }} />
      </div>

      {showShop && review.shopName ? (
        <Link to={`/shops/${review.shopId}`} style={{ fontSize: 13, color: palette.gold, fontWeight: 600 }}>
          {review.shopName}
        </Link>
      ) : null}

      <div className="pullquote" style={{ margin: '10px 0 4px' }}>
        <span aria-hidden style={{ color: palette.appetite, fontWeight: 700, marginRight: 2 }}>
          {'❝'}
        </span>
        <span style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{review.content}</span>
      </div>

      {review.images?.length ? (
        <AntImage.PreviewGroup>
          <Space size={6} wrap style={{ marginTop: 10 }}>
            {review.images.slice(0, 4).map((url, i) => (
              <AntImage
                key={i}
                src={thumb(url, 160)}
                width={80}
                height={80}
                preview={{ src: url }}
                style={{ objectFit: 'cover', borderRadius: 8 }}
              />
            ))}
          </Space>
        </AntImage.PreviewGroup>
      ) : null}

      <Space size={18} style={{ color: palette.muted, fontSize: 13, marginTop: 12 }}>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
          <LikeOutlined /> {review.likeCount}
        </span>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
          <MessageOutlined /> {review.replyCount}
        </span>
        <Link to={`/reviews/${review.id}`} style={{ fontWeight: 600 }}>
          查看详情
        </Link>
      </Space>
    </Card>
  )
}
