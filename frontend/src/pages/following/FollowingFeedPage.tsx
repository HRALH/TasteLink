import { useState } from 'react'
import { Button, Empty, Pagination, Skeleton, Space } from 'antd'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { feedApi } from '../../api/feed'
import { DEFAULT_PAGE, DEFAULT_SIZE } from '../../utils/constants'
import { palette } from '../../styles/tokens'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import QueryError from '../../components/QueryError'
import ReviewCard from '../../components/ReviewCard'
import SectionTitle from '../../components/editorial/SectionTitle'
import { Reveal } from '../../components/motion'

/** 关注的人的点评（产品优化 F3）：拉取式 feed，按时间倒序。 */
export default function FollowingFeedPage() {
  useDocumentTitle('关注的人')
  const [page, setPage] = useState(DEFAULT_PAGE)

  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ['feed', 'following', page],
    queryFn: () => feedApi.followingReviews({ page, size: DEFAULT_SIZE }),
  })

  return (
    <div>
      <Reveal>
        <SectionTitle eyebrow="FROM PEOPLE YOU FOLLOW" size="md">
          关注的人的点评
        </SectionTitle>
      </Reveal>

      {isPending ? (
        <Skeleton active />
      ) : isError ? (
        <QueryError onRetry={() => refetch()} />
      ) : !data?.records?.length ? (
        <Empty
          description="还没有关注的人的点评"
          style={{ marginTop: 40 }}
        >
          <Link to="/shops">
            <Button type="primary" shape="round" style={{ marginTop: 12, background: palette.appetite }}>
              去逛店铺找同好
            </Button>
          </Link>
        </Empty>
      ) : (
        <>
          <Reveal key={`feed-${page}`}>
            <Space direction="vertical" size={14} style={{ width: '100%', marginTop: 16 }}>
              {data.records.map((r) => (
                <ReviewCard key={r.id} review={r} />
              ))}
            </Space>
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
