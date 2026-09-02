import { useEffect, useState } from 'react'
import { Button, Card, Form, Input, Select, Skeleton, Modal, message } from 'antd'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import UploadImage from '../../components/UploadImage'
import SectionTitle from '../../components/editorial/SectionTitle'
import { Reveal } from '../../components/motion'
import { shopApi } from '../../api/shop'
import { adminShopApi } from '../../api/admin'
import { ApiError } from '../../api/request'
import { Code, CITIES } from '../../utils/constants'
import { palette } from '../../styles/tokens'
import type { CategoryVO, ShopDetailVO, UpdateShopRequest } from '../../types/api'

const { TextArea } = Input

interface ShopFormValues {
  name: string
  categoryId: number
  city: string
  address: string
  phone: string
  description: string
}

/**
 * 店铺编辑页（v2 FE-B）。按 MePage 的 load → setFieldsValue → save 模式：
 * 公开 `GET /shops/{id}` 载入 → `PUT /admin/shops/{id}` 保存。
 * 后端乐观锁由 `@Version` 处理，客户端不携 version；冲突(409)弹「加载最新内容」。
 */
export default function AdminShopEditPage() {
  const { id } = useParams<{ id: string }>()
  const shopId = Number(id)
  const navigate = useNavigate()
  const [form] = Form.useForm<ShopFormValues>()
  const [shop, setShop] = useState<ShopDetailVO | null>(null)
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [coverUrl, setCoverUrl] = useState('')
  const [categories, setCategories] = useState<CategoryVO[]>([])

  useEffect(() => {
    shopApi.categories().then(setCategories).catch(() => {})
  }, [])

  useEffect(() => {
    if (Number.isNaN(shopId)) return
    setLoading(true)
    shopApi
      .detail(shopId)
      .then((s) => {
        setShop(s)
        setCoverUrl(s.coverUrl || '')
        form.setFieldsValue({
          name: s.name,
          categoryId: s.categoryId,
          city: s.city,
          address: s.address || '',
          phone: s.phone || '',
          description: s.description || '',
        })
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [shopId, form])

  if (Number.isNaN(shopId)) return <Navigate to="/admin/shops" replace />
  if (loading || !shop) return <Skeleton active />

  const categoryOptions = categories.map((c) => ({ label: c.name, value: c.id }))
  const cityOptions = CITIES.map((c) => ({ label: c, value: c }))

  const fillForm = (s: ShopDetailVO) => {
    form.setFieldsValue({
      name: s.name,
      categoryId: s.categoryId,
      city: s.city,
      address: s.address || '',
      phone: s.phone || '',
      description: s.description || '',
    })
    setCoverUrl(s.coverUrl || '')
  }

  const onSave = async (values: ShopFormValues) => {
    setSubmitting(true)
    try {
      const body: UpdateShopRequest = {
        name: values.name,
        categoryId: values.categoryId,
        city: values.city,
        address: values.address,
        // 空值不下发（后端“非空才更新”，留空即保留原值）
        phone: values.phone || undefined,
        coverUrl: coverUrl || undefined,
        description: values.description || undefined,
      }
      await adminShopApi.update(shopId, body)
      message.success('已保存')
      navigate('/admin/shops')
    } catch (e) {
      if (e instanceof ApiError && e.code === Code.SHOP_VERSION_CONFLICT) {
        Modal.warning({
          title: '该店铺已被其它管理员修改并保存',
          content: '请先加载最新内容再编辑。加载会丢弃你本地未保存的改动。',
          okText: '加载最新内容',
          onOk: async () => {
            try {
              const latest = await shopApi.detail(shopId)
              setShop(latest)
              fillForm(latest)
              message.success('已加载最新内容')
            } catch {
              // 错误提示已由 request 拦截器统一处理
            }
          },
        })
      }
      // 其余错误：拦截器已 toast
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ maxWidth: 720, margin: '0 auto' }}>
      <Reveal>
        <Card className="tl-card" styles={{ body: { padding: 24 } }}>
          <SectionTitle eyebrow="ADMIN" size="md">
            编辑店铺
          </SectionTitle>
          <Form form={form} layout="vertical" onFinish={onSave}>
            <Form.Item
              name="name"
              label="店铺名称"
              rules={[{ required: true, message: '请输入店铺名称' }, { max: 128, message: '名称不超过 128 字' }]}
            >
              <Input maxLength={128} placeholder="店铺名称" />
            </Form.Item>
            <Form.Item name="categoryId" label="分类" rules={[{ required: true, message: '请选择分类' }]}>
              <Select options={categoryOptions} placeholder="选择分类" notFoundContent="加载中…" />
            </Form.Item>
            <Form.Item name="city" label="城市" rules={[{ required: true, message: '请选择城市' }]}>
              <Select options={cityOptions} placeholder="选择城市" />
            </Form.Item>
            <Form.Item
              name="address"
              label="地址"
              rules={[{ required: true, message: '请输入地址' }, { max: 255, message: '地址不超过 255 字' }]}
            >
              <Input maxLength={255} placeholder="详细地址" />
            </Form.Item>
            <Form.Item name="phone" label="电话" rules={[{ max: 32, message: '电话不超过 32 字' }]}>
              <Input maxLength={32} placeholder="联系电话（可选，留空保留原值）" />
            </Form.Item>
            <Form.Item label="封面图">
              {coverUrl ? (
                <div style={{ marginBottom: 12 }}>
                  <img
                    src={coverUrl}
                    alt="当前封面"
                    style={{ width: '100%', maxWidth: 320, height: 160, objectFit: 'cover', borderRadius: 8 }}
                  />
                  <div style={{ color: palette.muted, fontSize: 12, marginTop: 4 }}>当前封面。上传新图将替换。</div>
                </div>
              ) : (
                <div style={{ color: palette.muted, fontSize: 12, marginBottom: 12 }}>暂无封面图。</div>
              )}
              <UploadImage maxCount={1} onChange={(urls) => setCoverUrl(urls[0] || '')} />
            </Form.Item>
            <Form.Item name="description" label="简介" rules={[{ max: 1000, message: '简介不超过 1000 字' }]}>
              <TextArea rows={4} maxLength={1000} showCount placeholder="店铺简介（可选）" />
            </Form.Item>
            <div style={{ display: 'flex', gap: 12 }}>
              <Button type="primary" shape="round" htmlType="submit" loading={submitting} className="tl-press">
                保存
              </Button>
              <Button shape="round" onClick={() => navigate('/admin/shops')}>
                取消
              </Button>
            </div>
          </Form>
        </Card>
      </Reveal>
    </div>
  )
}
