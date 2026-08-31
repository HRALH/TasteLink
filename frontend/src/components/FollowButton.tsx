import { useState } from 'react'
import { Button, message } from 'antd'
import { followApi } from '../api/follow'
import { useAuthStore } from '../store/authStore'

/** 关注/取关按钮：不可关注自己（R6）；乐观更新；幂等由后端保证 */
export default function FollowButton({
  userId,
  hasFollowed,
  onChange,
}: {
  userId: number
  hasFollowed: boolean
  onChange?: (followed: boolean) => void
}) {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn)
  const me = useAuthStore((s) => s.userInfo)
  const [followed, setFollowed] = useState(hasFollowed)
  const [loading, setLoading] = useState(false)

  // 不可关注自己：不渲染
  if (me?.userId === userId) return null

  const toggle = async () => {
    if (!isLoggedIn) {
      message.warning('请先登录')
      return
    }
    const next = !followed
    setLoading(true)
    setFollowed(next)
    try {
      if (next) await followApi.follow(userId)
      else await followApi.unfollow(userId)
      onChange?.(next)
    } catch {
      setFollowed(!next)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Button type={followed ? 'default' : 'primary'} loading={loading} onClick={toggle}>
      {followed ? '已关注' : '关注'}
    </Button>
  )
}
