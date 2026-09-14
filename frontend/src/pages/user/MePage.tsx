import { useState } from 'react'
import { Avatar, Button, Card, Form, Input, Skeleton, message } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import UploadImage from '../../components/UploadImage'
import QueryError from '../../components/QueryError'
import SectionTitle from '../../components/editorial/SectionTitle'
import { userApi } from '../../api/user'
import { useAuthStore } from '../../store/authStore'
import { palette } from '../../styles/tokens'

const { TextArea } = Input

interface ProfileFormValues {
  nickname: string
  bio: string
}

export default function MePage() {
  const updateProfile = useAuthStore((s) => s.updateProfile)
  const queryClient = useQueryClient()
  const [form] = Form.useForm<ProfileFormValues>()
  // 仅承载「本次新上传的头像」；未改动时回落到已加载资料的头像（免 effect 回填 setState）
  const [avatarDraft, setAvatarDraft] = useState<string | null>(null)

  const userQuery = useQuery({ queryKey: ['me'], queryFn: () => userApi.me() })
  const user = userQuery.data

  const saveMutation = useMutation({
    mutationFn: (values: ProfileFormValues) =>
      userApi.updateMe({
        nickname: values.nickname,
        bio: values.bio,
        avatarUrl: (avatarDraft ?? user?.avatarUrl) || undefined,
      }),
    onSuccess: (updated) => {
      queryClient.setQueryData(['me'], updated)
      queryClient.invalidateQueries({ queryKey: ['user', updated.id] })
      setAvatarDraft(null)
      updateProfile({ nickname: updated.nickname, avatarUrl: updated.avatarUrl })
      message.success('保存成功')
    },
    // 错误提示已由 request 拦截器统一处理
  })

  if (userQuery.isPending) return <Skeleton avatar active />
  if (userQuery.isError) return <QueryError onRetry={() => userQuery.refetch()} />
  if (!user) return <Skeleton avatar active />

  // Form 在数据就绪后才挂载，initialValues 一次性填充（替代原 effect setFieldsValue）
  const displayAvatar = avatarDraft ?? user.avatarUrl

  return (
    <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap' }}>
      <Card className="tl-card" style={{ flex: '1 1 240px' }}>
        <SectionTitle size="sm" style={{ marginBottom: 12 }}>
          头像
        </SectionTitle>
        <Avatar size={96} src={displayAvatar} style={{ background: palette.rule }}>
          {user.nickname?.[0]}
        </Avatar>
        <div style={{ marginTop: 16 }}>
          <UploadImage
            onChange={(urls) => setAvatarDraft(urls[urls.length - 1] || null)}
            maxCount={1}
          />
        </div>
      </Card>
      <Card className="tl-card" style={{ flex: '1 1 320px' }}>
        <SectionTitle size="sm" style={{ marginBottom: 12 }}>
          编辑资料
        </SectionTitle>
        <Form
          form={form}
          layout="vertical"
          initialValues={{ nickname: user.nickname, bio: user.bio || '' }}
          onFinish={(values) => saveMutation.mutate(values)}
        >
          <Form.Item label="用户名">
            <Input value={user.username} disabled />
          </Form.Item>
          <Form.Item name="nickname" label="昵称" rules={[{ required: true, message: '请输入昵称' }]}>
            <Input maxLength={20} placeholder="昵称（可修改）" />
          </Form.Item>
          <Form.Item name="bio" label="简介">
            <TextArea rows={3} maxLength={100} showCount placeholder="介绍一下自己吧" />
          </Form.Item>
          <Button type="primary" shape="round" htmlType="submit" loading={saveMutation.isPending}>
            保存
          </Button>
        </Form>
      </Card>
    </div>
  )
}
