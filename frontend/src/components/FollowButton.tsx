import { useState } from 'react'
import { Button, message } from 'antd'
import { CheckOutlined, PlusOutlined } from '@ant-design/icons'
import { useMutation } from '@tanstack/react-query'
import { followApi } from '../api/follow'
import { useAuthStore } from '../store/authStore'

/**
 * 关注/取关按钮：不可关注自己（R6）；乐观更新（useMutation onMutate 快照 + onError 回滚）；
 * 幂等由后端保证；切换时 icon 心爆反馈。
 * 注意：内部态只在挂载时初始化——使用方必须以 key={userId} 切用户（F1-1 契约）。
 */
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
  // 心爆重放计数：每次 toggle 自增，驱动 icon remount 重播 .tl-heartburst（首屏 0 不播）
  const [burst, setBurst] = useState(0)

  const mutation = useMutation({
    mutationFn: (next: boolean) => (next ? followApi.follow(userId) : followApi.unfollow(userId)),
    onMutate: (next) => {
      const prev = followed
      setFollowed(next)
      setBurst((b) => b + 1)
      return { prev }
    },
    onError: (_e, _next, ctx) => setFollowed(ctx?.prev ?? followed),
    onSuccess: (_data, next) => onChange?.(next),
  })

  // 不可关注自己：不渲染
  if (me?.userId === userId) return null

  const toggle = () => {
    if (!isLoggedIn) {
      message.warning('请先登录')
      return
    }
    mutation.mutate(!followed)
  }

  return (
    <Button
      type={followed ? 'default' : 'primary'}
      shape="round"
      loading={mutation.isPending}
      onClick={toggle}
      className="tl-press"
      icon={
        <span key={burst} aria-hidden className={burst ? 'tl-heartburst' : undefined}>
          {followed ? <CheckOutlined /> : <PlusOutlined />}
        </span>
      }
    >
      {followed ? '已关注' : '关注'}
    </Button>
  )
}
