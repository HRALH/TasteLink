import { useState } from 'react'
import { Avatar, Button, Empty, Pagination, Skeleton, Space } from 'antd'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationApi } from '../../api/notification'
import type { NotificationVO } from '../../types/api'
import { palette } from '../../styles/tokens'
import { DEFAULT_PAGE, DEFAULT_SIZE } from '../../utils/constants'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import QueryError from '../../components/QueryError'
import SectionTitle from '../../components/editorial/SectionTitle'
import { Reveal } from '../../components/motion'

/** 通知文案（按 type） */
function describe(n: NotificationVO): { text: string; to: string } {
  switch (n.type) {
    case 'REVIEW_LIKED':
      return { text: '赞了你的点评', to: `/reviews/${n.targetId}` }
    case 'REVIEW_COMMENTED':
      return { text: '评论了你的点评', to: `/reviews/${n.targetId}` }
    case 'USER_FOLLOWED':
      return { text: '关注了你', to: `/users/${n.actorId}` }
    default:
      return { text: '向你发来一条互动', to: '/' }
  }
}

export default function NotificationsPage() {
  useDocumentTitle('通知中心')
  const [page, setPage] = useState(DEFAULT_PAGE)
  const qc = useQueryClient()

  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ['notifications', page],
    queryFn: () => notificationApi.page({ page, size: DEFAULT_SIZE }),
  })

  const markReadMut = useMutation({
    mutationFn: notificationApi.markRead,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  })

  const markAllMut = useMutation({
    mutationFn: notificationApi.markAllRead,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  })

  const unreadExists = data?.records.some((n) => !n.isRead)

  return (
    <div>
      <Reveal>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
          <SectionTitle eyebrow="ACTIVITY" size="md">
            通知中心
          </SectionTitle>
          {unreadExists ? (
            <Button
              shape="round"
              loading={markAllMut.isPending}
              onClick={() => markAllMut.mutate()}
            >
              全部已读
            </Button>
          ) : null}
        </div>
      </Reveal>

      {isPending ? (
        <Skeleton active />
      ) : isError ? (
        <QueryError onRetry={() => refetch()} />
      ) : !data?.records?.length ? (
        <Empty description="暂无通知，去给别人点点赞、发条评论吧" />
      ) : (
        <>
          <Reveal key={`notif-${page}`}>
            <Space direction="vertical" size={12} style={{ width: '100%', marginTop: 16 }}>
              {data.records.map((n) => {
                const { text, to } = describe(n)
                return (
                  <Link
                    key={n.id}
                    to={to}
                    onClick={() => {
                      if (!n.isRead && !markReadMut.isPending) markReadMut.mutate(n.id)
                    }}
                    className="tl-card"
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 12,
                      padding: '14px 16px',
                      background: n.isRead ? palette.surface : palette.paper,
                      border: `1px solid ${palette.rule}`,
                      borderRadius: 10,
                    }}
                  >
                    <Avatar src={n.actorAvatarUrl} size={36}>
                      {n.actorNickname?.[0]}
                    </Avatar>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 14, color: palette.ink }}>
                        <span style={{ fontWeight: 600 }}>{n.actorNickname}</span>
                        <span style={{ color: palette.muted, marginLeft: 6 }}>{text}</span>
                        {!n.isRead ? (
                          <span
                            aria-hidden
                            style={{
                              display: 'inline-block',
                              width: 8,
                              height: 8,
                              marginLeft: 8,
                              borderRadius: '50%',
                              background: palette.appetite,
                              verticalAlign: 'middle',
                            }}
                          />
                        ) : null}
                      </div>
                      {n.preview ? (
                        <div
                          style={{
                            marginTop: 4,
                            fontSize: 13,
                            color: palette.muted,
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                          }}
                        >
                          “{n.preview}”
                        </div>
                      ) : null}
                      <div style={{ marginTop: 2, fontSize: 12, color: palette.muted }}>
                        {n.createTime}
                      </div>
                    </div>
                  </Link>
                )
              })}
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
