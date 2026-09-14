import { useState } from 'react'
import { Empty, Pagination, Skeleton } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { followApi } from '../api/follow'
import { DEFAULT_PAGE, DEFAULT_SIZE } from '../utils/constants'
import UserCard from './UserCard'
import QueryError from './QueryError'
import SectionTitle from './editorial/SectionTitle'
import { Reveal } from './motion'

/** 关注/粉丝列表复用组件 */
export default function FollowList({
  userId,
  mode,
}: {
  userId: number
  mode: 'followings' | 'followers'
}) {
  const [page, setPage] = useState(DEFAULT_PAGE)

  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ['follow', userId, mode, page],
    queryFn: () =>
      (mode === 'followings' ? followApi.followings : followApi.followers)(userId, {
        page,
        size: DEFAULT_SIZE,
      }),
    enabled: !Number.isNaN(userId) && userId > 0,
  })

  return (
    <div>
      <Reveal>
        <SectionTitle eyebrow={mode === 'followings' ? 'FOLLOWING' : 'FOLLOWERS'} size="sm">
          {mode === 'followings' ? '关注列表' : '粉丝列表'}
        </SectionTitle>
      </Reveal>
      {isPending ? (
        <Skeleton active />
      ) : isError ? (
        <QueryError onRetry={() => refetch()} />
      ) : !data?.records?.length ? (
        <Empty description={mode === 'followings' ? '暂无关注' : '暂无粉丝'} />
      ) : (
        <>
          <Reveal key={`list-${mode}-${page}`}>
            {data.records.map((u) => (
              <UserCard key={u.id} user={u} />
            ))}
          </Reveal>
          <Pagination
            style={{ marginTop: 20, textAlign: 'center' }}
            current={page}
            pageSize={DEFAULT_SIZE}
            total={data.total}
            onChange={(p) => {
              setPage(p)
              window.scrollTo({ top: 0 })
            }}
            showSizeChanger={false}
          />
        </>
      )}
    </div>
  )
}
