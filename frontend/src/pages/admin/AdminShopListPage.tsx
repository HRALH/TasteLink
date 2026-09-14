import { useState } from 'react'
import { Card, Col, Empty, Input, Modal, Pagination, Row, Select, Skeleton, Space, message } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import { isAxiosError } from 'axios'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import AdminShopCard from '../../components/admin/AdminShopCard'
import QueryError from '../../components/QueryError'
import SectionTitle from '../../components/editorial/SectionTitle'
import { Reveal } from '../../components/motion'
import { staggerDelay } from '../../utils/motion'
import { shopApi } from '../../api/shop'
import { adminShopApi } from '../../api/admin'
import { ApiError } from '../../api/request'
import { useCategories } from '../../hooks/useCategories'
import { Code, CITIES, DEFAULT_PAGE, DEFAULT_SIZE } from '../../utils/constants'
import type { ShopVO } from '../../types/api'

/**
 * 店铺管理列表（v2 FE-B + FE-C）。
 * 复用公开 GET /shops（后端未建管理专用列表，软删店铺已被该接口按 status 过滤，故删除后从列表移除）。
 */
export default function AdminShopListPage() {
  const navigate = useNavigate()
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState<string | undefined>(undefined)
  const [categoryId, setCategoryId] = useState<number>(0)
  const [city, setCity] = useState<string>('')
  const [page, setPage] = useState(DEFAULT_PAGE)

  const { data: categories = [] } = useCategories()
  const {
    data: result,
    isPending,
    isError,
    refetch,
  } = useQuery({
    queryKey: ['shops', { keyword, categoryId, city, page }],
    queryFn: () =>
      shopApi.list({
        keyword,
        categoryId: categoryId || undefined,
        city: city || undefined,
        page,
        size: DEFAULT_SIZE,
      }),
  })

  const resetPage = () => setPage(DEFAULT_PAGE)

  const categoryOptions = [{ label: '全部分类', value: 0 }, ...categories.map((c) => ({ label: c.name, value: c.id }))]
  const cityOptions = [{ label: '全部城市', value: '' }, ...CITIES.map((c) => ({ label: c, value: c }))]
  const resultKey = `${keyword ?? 'all'}-${categoryId}-${city}-${page}`

  const handleDelete = (shop: ShopVO) => {
    Modal.confirm({
      title: `删除「${shop.name}」?`,
      content: '删除后店铺立即下架，关联点评/评论/点赞将在稍后自动清理，不可立即恢复；若需彻底回滚请联系统管理员。',
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        const removedWasLast = result ? result.records.length === 1 : false
        try {
          await adminShopApi.remove(shop.id)
          message.success('已删除，关联数据将在稍后自动清理')
          if (removedWasLast && page > 1) {
            // 删尽当前页则回上一页（key 变化触发重新拉取）
            setPage((p) => p - 1)
          } else {
            refetch()
          }
        } catch (e) {
          if (e instanceof ApiError && e.code === Code.SHOP_VERSION_CONFLICT) {
            message.warning('店铺已被他人改动，请刷新后重试')
            refetch()
          } else if (isAxiosError(e) && e.response?.status === 404) {
            // 已删除（幂等）：拦截器已 toast 后端消息，刷新列表即可
            refetch()
          }
          // 其余错误：拦截器已 toast
        }
      },
    })
  }

  return (
    <div>
      <Reveal>
        <SectionTitle eyebrow="ADMIN" size="lg">
          店铺管理
        </SectionTitle>
      </Reveal>

      <Reveal style={{ marginBottom: 20 }}>
        <Card className="tl-card" styles={{ body: { padding: 16 } }}>
          <Space size={12} wrap>
            <Input
              className="tl-field"
              placeholder="搜索店铺名称"
              prefix={<SearchOutlined />}
              value={keywordInput}
              onChange={(e) => setKeywordInput(e.target.value)}
              onPressEnter={() => {
                setKeyword(keywordInput || undefined)
                resetPage()
              }}
              style={{ width: 220 }}
              allowClear
            />
            <Select
              value={categoryId}
              options={categoryOptions}
              onChange={(v) => {
                setCategoryId(v)
                resetPage()
              }}
              style={{ width: 140 }}
            />
            <Select
              value={city}
              options={cityOptions}
              onChange={(v) => {
                setCity(v)
                resetPage()
              }}
              style={{ width: 120 }}
            />
          </Space>
        </Card>
      </Reveal>

      {isPending ? (
        <Skeleton active />
      ) : isError ? (
        <QueryError onRetry={() => refetch()} />
      ) : !result?.records?.length ? (
        <Empty description="没有可管理的店铺" />
      ) : (
        <>
          <Row gutter={[16, 16]} style={{ marginBottom: 16 }} key={resultKey}>
            {result.records.map((s, i) => (
              <Col key={s.id} xs={24} sm={12} md={8}>
                <Reveal delay={staggerDelay(i)} style={{ height: '100%' }}>
                  <AdminShopCard
                    shop={s}
                    onEdit={(id) => navigate(`/admin/shops/${id}/edit`)}
                    onDelete={handleDelete}
                  />
                </Reveal>
              </Col>
            ))}
          </Row>
          <Pagination
            style={{ textAlign: 'center' }}
            current={page}
            pageSize={DEFAULT_SIZE}
            total={result.total}
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
