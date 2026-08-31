import { useEffect, useState } from 'react'
import { Col, Empty, Input, Pagination, Row, Segmented, Select, Skeleton, Space } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import ShopCard from '../../components/ShopCard'
import { shopApi } from '../../api/shop'
import type { CategoryVO, PageResult, ShopVO } from '../../types/api'
import { CITIES, DEFAULT_PAGE, DEFAULT_SIZE, ShopSort } from '../../utils/constants'

export default function ShopListPage() {
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState<string | undefined>(undefined)
  const [categoryId, setCategoryId] = useState<number>(0)
  const [city, setCity] = useState<string>('')
  const [sortBy, setSortBy] = useState<string>(ShopSort.REVIEW_COUNT)
  const [page, setPage] = useState(DEFAULT_PAGE)

  const [categories, setCategories] = useState<CategoryVO[]>([])
  const [result, setResult] = useState<PageResult<ShopVO> | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    shopApi
      .categories()
      .then(setCategories)
      .catch(() => {})
  }, [])

  useEffect(() => {
    setLoading(true)
    shopApi
      .list({
        keyword,
        categoryId: categoryId || undefined,
        city: city || undefined,
        sortBy,
        page,
        size: DEFAULT_SIZE,
      })
      .then(setResult)
      .catch(() => setResult(null))
      .finally(() => setLoading(false))
  }, [keyword, categoryId, city, sortBy, page])

  const resetPage = () => setPage(DEFAULT_PAGE)

  const categoryOptions = [
    { label: '全部分类', value: 0 },
    ...categories.map((c) => ({ label: c.name, value: c.id })),
  ]
  const cityOptions = [{ label: '全部城市', value: '' }, ...CITIES.map((c) => ({ label: c, value: c }))]
  const sortOptions = [
    { label: '热度', value: ShopSort.REVIEW_COUNT },
    { label: '评分', value: ShopSort.RATING },
  ]

  return (
    <div>
      <Space size={12} wrap style={{ marginBottom: 16 }}>
        <Input
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
        <Segmented
          options={sortOptions}
          value={sortBy}
          onChange={(v) => {
            setSortBy(v as string)
            resetPage()
          }}
        />
      </Space>

      {loading ? (
        <Skeleton active />
      ) : !result?.records?.length ? (
        <Empty description="没有找到符合条件的店铺" />
      ) : (
        <>
          <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
            {result.records.map((s) => (
              <Col key={s.id} xs={24} sm={12} md={8}>
                <ShopCard shop={s} />
              </Col>
            ))}
          </Row>
          <Pagination
            style={{ textAlign: 'center' }}
            current={page}
            pageSize={DEFAULT_SIZE}
            total={result.total}
            onChange={setPage}
            showSizeChanger={false}
          />
        </>
      )}
    </div>
  )
}
