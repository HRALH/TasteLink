import { useState } from 'react'
import { Button, Card, Form, Input, Rate, message } from 'antd'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import UploadImage from '../../components/UploadImage'
import SectionTitle from '../../components/editorial/SectionTitle'
import { Reveal } from '../../components/motion'
import { reviewApi } from '../../api/review'
import { shopApi } from '../../api/shop'
import { MAX_REVIEW_IMAGES } from '../../utils/constants'

const { TextArea } = Input

interface ReviewFormValues {
  content: string
  rating: number
}

export default function ShopReviewPage() {
  const { id } = useParams<{ id: string }>()
  const shopId = Number(id)
  const navigate = useNavigate()
  const [form] = Form.useForm<ReviewFormValues>()
  const [imageUrls, setImageUrls] = useState<string[]>([])

  // 店名仅作标题展示（「给「x」写点评」），失败不阻断表单，无需错误态
  const { data: shop } = useQuery({
    queryKey: ['shop', shopId],
    queryFn: () => shopApi.detail(shopId),
    enabled: !Number.isNaN(shopId) && shopId > 0,
  })

  const createMutation = useMutation({
    mutationFn: (values: ReviewFormValues) =>
      reviewApi.create(shopId, {
        content: values.content,
        rating: values.rating,
        imageUrls,
      }),
    onSuccess: (review) => {
      message.success('发布成功')
      navigate(`/reviews/${review.id}`, { replace: true })
    },
    // 错误提示已由 request 拦截器统一处理
  })

  // 非法 shopId（如 /shops/abc/review）兜底跳列表，避免表单提交到 POST /shops/NaN/reviews
  if (Number.isNaN(shopId)) return <Navigate to="/shops" replace />

  return (
    <Reveal>
      <Card className="tl-card" style={{ maxWidth: 680 }}>
        <SectionTitle eyebrow="WRITE A REVIEW" size="md">
          {shop ? `给「${shop.name}」写点评` : '写点评'}
        </SectionTitle>
        <Form
          form={form}
          layout="vertical"
          initialValues={{ rating: 5 }}
          onFinish={(values) => createMutation.mutate(values)}
        >
          <Form.Item name="rating" label="评分" rules={[{ required: true, message: '请选择评分' }]}>
            <Rate />
          </Form.Item>
          <Form.Item
            name="content"
            label="点评内容"
            rules={[{ required: true, message: '请填写点评内容' }]}
          >
            <TextArea rows={5} maxLength={500} showCount placeholder="分享你的用餐体验…" />
          </Form.Item>
          <Form.Item label={`图片（最多 ${MAX_REVIEW_IMAGES} 张）`}>
            <UploadImage onChange={setImageUrls} />
          </Form.Item>
          <Button
            type="primary"
            shape="round"
            htmlType="submit"
            loading={createMutation.isPending}
            className="tl-press"
          >
            发布点评
          </Button>
        </Form>
      </Card>
    </Reveal>
  )
}
