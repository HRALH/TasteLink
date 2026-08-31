import { Avatar, Space } from 'antd'
import { Link } from 'react-router-dom'
import type { UserVO } from '../types/api'
import FollowButton from './FollowButton'

/** 用户摘要卡片：头像 + 昵称 + 简介 + 关注按钮 */
export default function UserCard({ user }: { user: UserVO }) {
  return (
    <Space
      size={12}
      align="center"
      style={{ padding: '12px 0', borderBottom: '1px solid #f0f0f0' }}
    >
      <Link to={`/users/${user.id}`}>
        <Avatar src={user.avatarUrl} size={48}>
          {user.nickname?.[0]}
        </Avatar>
      </Link>
      <div style={{ flex: 1, minWidth: 0 }}>
        <Link to={`/users/${user.id}`} style={{ fontWeight: 600 }}>
          {user.nickname}
        </Link>
        <div style={{ color: '#888', fontSize: 13 }}>{user.bio || '暂无简介'}</div>
      </div>
      <FollowButton userId={user.id} hasFollowed={user.hasFollowed} />
    </Space>
  )
}
