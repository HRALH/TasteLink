import { useEffect, useState } from 'react'
import { Empty, Pagination, Skeleton } from 'antd'
import { followApi } from '../api/follow'
import type { PageResult, UserVO } from '../types/api'
import { DEFAULT_PAGE, DEFAULT_SIZE } from '../utils/constants'
import UserCard from './UserCard'
import SectionTitle from './editorial/SectionTitle'

/** 关注/粉丝列表复用组件 */
export default function FollowList({
  userId,
  mode,
}: {
  userId: number
  mode: 'followings' | 'followers'
}) {
  const [data, setData] = useState<PageResult<UserVO> | null>(null)
  const [page, setPage] = useState(DEFAULT_PAGE)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!userId) return
    setLoading(true)
    const fetcher = mode === 'followings' ? followApi.followings : followApi.followers
    fetcher(userId, { page, size: DEFAULT_SIZE })
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false))
  }, [userId, page, mode])

  return (
    <div>
      <SectionTitle eyebrow={mode === 'followings' ? 'FOLLOWING' : 'FOLLOWERS'} size="sm">
        {mode === 'followings' ? '关注列表' : '粉丝列表'}
      </SectionTitle>
      {loading ? (
        <Skeleton active />
      ) : !data?.records?.length ? (
        <Empty description={mode === 'followings' ? '暂无关注' : '暂无粉丝'} />
      ) : (
        <>
          {data.records.map((u) => (
            <UserCard key={u.id} user={u} />
          ))}
          <Pagination
            style={{ marginTop: 20, textAlign: 'center' }}
            current={page}
            pageSize={DEFAULT_SIZE}
            total={data.total}
            onChange={setPage}
            showSizeChanger={false}
          />
        </>
      )}
    </div>
  )
}
