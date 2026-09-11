import { Avatar, Space } from 'antd'
import { Link } from 'react-router-dom'
import type { UserVO } from '../types/api'
import FollowButton from './FollowButton'
import { palette } from '../styles/tokens'

/** 用户摘要卡片：头像 + 昵称 + 简介 + 关注按钮 */
export default function UserCard({ user }: { user: UserVO }) {
  return (
    <Space
      size={12}
      align="center"
      style={{ padding: '14px 0', borderBottom: `1px solid ${palette.rule}` }}
    >
      <Link to={`/users/${user.id}`}>
        <Avatar src={user.avatarUrl} size={48} style={{ background: palette.rule }}>
          {user.nickname?.[0]}
        </Avatar>
      </Link>
      <div style={{ flex: 1, minWidth: 0 }}>
        <Link to={`/users/${user.id}`} style={{ fontWeight: 600, color: palette.ink }}>
          {user.nickname}
        </Link>
        <div style={{ color: palette.muted, fontSize: 13, marginTop: 2 }}>
          {user.bio || '暂无简介'}
        </div>
      </div>
      <FollowButton key={user.id} userId={user.id} hasFollowed={user.hasFollowed} />
    </Space>
  )
}
