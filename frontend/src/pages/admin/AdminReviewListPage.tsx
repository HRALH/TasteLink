import { useState } from 'react'
import {
  Button,
  Card,
  Empty,
  Pagination,
  Rate,
  Select,
  Skeleton,
  Space,
  Tag,
  message,
} from 'antd'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminContentApi } from '../../api/admin'
import type { AdminReviewVO } from '../../types/api'
import { DEFAULT_PAGE, DEFAULT_SIZE } from '../../utils/constants'
import { palette } from '../../styles/tokens'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import QueryError from '../../components/QueryError'
import SectionTitle from '../../components/editorial/SectionTitle'
import { Reveal } from '../../components/motion'

/**
 * 内容治理（产品优化 F4）：后台点评下架/恢复 + 举报查阅。
 * 复用编辑式卡片风格；下架/恢复镜像后端计数 + 热度 zrem（Service 内完成）。
 */
export default function AdminReviewListPage() {
  useDocumentTitle('内容治理')
  const qc = useQueryClient()
  const [filterStatus, setFilterStatus] = useState<number | null>(null) // null=全部
  const [page, setPage] = useState(DEFAULT_PAGE)

  const {
    data,
    isPending,
    isError,
    refetch,
  } = useQuery({
    queryKey: ['admin', 'reviews', filterStatus, page],
    queryFn: () =>
      adminContentApi.listReviews({ page, size: DEFAULT_SIZE, status: filterStatus ?? undefined }),
  })

  const hideMut = useMutation({
    mutationFn: adminContentApi.hideReview,
    onSuccess: () => {
      message.success('已下架，计数与热度同步回扣')
      qc.invalidateQueries({ queryKey: ['admin', 'reviews'] })
    },
  })
  const restoreMut = useMutation({
    mutationFn: adminContentApi.restoreReview,
    onSuccess: () => {
      message.success('已恢复可见')
      qc.invalidateQueries({ queryKey: ['admin', 'reviews'] })
    },
  })

  const validStatusOptions = [
    { label: '全部状态', value: -1 },
    { label: '正常', value: 1 },
    { label: '已下架', value: 0 },
  ]

  return (
    <div>
      <Reveal>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
          <SectionTitle eyebrow="ADMIN" size="lg">
            内容治理
          </SectionTitle>
          <Select
            value={filterStatus ?? -1}
            options={validStatusOptions}
            onChange={(v) => {
              setFilterStatus(v === -1 ? null : v)
              setPage(DEFAULT_PAGE)
            }}
            style={{ width: 140 }}
          />
        </div>
      </Reveal>

      {isPending ? (
        <Skeleton active />
      ) : isError ? (
        <QueryError onRetry={() => refetch()} />
      ) : !data?.records?.length ? (
        <Empty description="没有符合条件的点评" />
      ) : (
        <>
          <Space direction="vertical" size={12} style={{ width: '100%', marginTop: 16 }}>
            {data.records.map((r) => (
              <ReviewRow
                key={r.id}
                review={r}
                onToggle={
                  r.status === 1
                    ? () => hideMut.mutate(r.id)
                    : () => restoreMut.mutate(r.id)
                }
                loading={
                  (r.status === 1 ? hideMut.isPending : restoreMut.isPending)
                }
              />
            ))}
          </Space>
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

      <Reveal style={{ marginTop: 36 }}>
        <SectionTitle eyebrow="REPORTS" size="md">
          举报记录
        </SectionTitle>
      </Reveal>
      <ReportsList />
    </div>
  )
}

function ReviewRow({
  review,
  onToggle,
  loading,
}: {
  review: AdminReviewVO
  onToggle: () => void
  loading: boolean
}) {
  const hidden = review.status === 0
  return (
    <Card className="tl-card" styles={{ body: { padding: 16 } }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
        <Space size={10} align="center">
          <Link to={`/users/${review.userId}`} style={{ fontWeight: 600, color: palette.ink }}>
            {review.userNickname}
          </Link>
          <Link to={`/shops/${review.shopId}`} style={{ fontSize: 13, color: palette.gold, fontWeight: 600 }}>
            {review.shopName}
          </Link>
          <Tag color={hidden ? 'default' : 'green'} style={{ marginInlineEnd: 0 }}>
            {hidden ? '已下架' : '正常'}
          </Tag>
        </Space>
        <Rate disabled value={review.rating} style={{ fontSize: 14 }} />
      </div>
      <div
        style={{
          fontSize: 14,
          color: palette.ink,
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
          display: '-webkit-box',
          WebkitLineClamp: 3,
          WebkitBoxOrient: 'vertical',
          overflow: 'hidden',
        }}
      >
        {review.content}
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
        <span style={{ fontSize: 12, color: palette.muted }}>
          {review.createTime} · {review.likeCount} 赞 · {review.replyCount} 评
        </span>
        <Button
          danger={review.status === 1}
          shape="round"
          size="small"
          loading={loading}
          onClick={onToggle}
        >
          {onToggleLabel(review.status)}
        </Button>
      </div>
    </Card>
  )
}

function onToggleLabel(status: number): string {
  return status === 1 ? '下架' : '恢复'
}

/** 举报记录（精简列表，只读查阅；处理决策为后续工作流，本次仅展示线索） */
function ReportsList() {
  const [status, setStatus] = useState<string | null>(null)
  const [page, setPage] = useState(DEFAULT_PAGE)
  const { data, isPending } = useQuery({
    queryKey: ['admin', 'reports', status, page],
    queryFn: () =>
      adminContentApi.listReports({ page, size: DEFAULT_SIZE, status: status ?? undefined }),
  })

  const statusOptions = [
    { label: '全部', value: '' },
    { label: '待处理', value: 'PENDING' },
    { label: '已处理', value: 'RESOLVED' },
  ]

  return (
    <div style={{ marginTop: 16 }}>
      <div style={{ marginBottom: 12 }}>
        <Select
          value={status ?? ''}
          options={statusOptions}
          onChange={(v) => {
            setStatus(v || null)
            setPage(DEFAULT_PAGE)
          }}
          style={{ width: 120 }}
        />
      </div>
      {isPending ? (
        <Skeleton active />
      ) : !data?.records?.length ? (
        <Empty description="暂无举报记录" />
      ) : (
        <>
          <Space direction="vertical" size={10} style={{ width: '100%' }}>
            {data.records.map((rp) => (
              <Card key={rp.id} className="tl-card" styles={{ body: { padding: 12 } }}>
                <div style={{ fontSize: 13, color: palette.ink }}>
                  <span style={{ fontWeight: 600 }}>{rp.reporterNickname || `用户 ${rp.reporterId}`}</span>
                  <span style={{ color: palette.muted, marginInline: 6 }}>
                    举报 {rp.targetType} #{rp.targetId}
                  </span>
                  <Tag color={rp.status === 'RESOLVED' ? 'default' : 'orange'} style={{ marginInlineEnd: 0 }}>
                    {rp.status === 'RESOLVED' ? '已处理' : '待处理'}
                  </Tag>
                </div>
                <div style={{ marginTop: 4, fontSize: 13, color: palette.muted }}>
                  {rp.reason}
                  <span style={{ marginLeft: 8 }}>{rp.createTime}</span>
                </div>
              </Card>
            ))}
          </Space>
          <Pagination
            style={{ marginTop: 16, textAlign: 'center' }}
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
