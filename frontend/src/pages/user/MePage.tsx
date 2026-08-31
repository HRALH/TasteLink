import { useEffect, useState } from 'react'
import { Avatar, Button, Card, Form, Input, Skeleton, message } from 'antd'
import UploadImage from '../../components/UploadImage'
import SectionTitle from '../../components/editorial/SectionTitle'
import { userApi } from '../../api/user'
import { useAuthStore } from '../../store/authStore'
import { palette } from '../../styles/tokens'
import type { UserVO } from '../../types/api'

const { TextArea } = Input

interface ProfileFormValues {
  nickname: string
  bio: string
}

export default function MePage() {
  const updateProfile = useAuthStore((s) => s.updateProfile)
  const [user, setUser] = useState<UserVO | null>(null)
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm<ProfileFormValues>()
  const [avatarUrl, setAvatarUrl] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    setLoading(true)
    userApi
      .me()
      .then((u) => {
        setUser(u)
        setAvatarUrl(u.avatarUrl || '')
        form.setFieldsValue({ nickname: u.nickname, bio: u.bio || '' })
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [form])

  const onSave = async (values: ProfileFormValues) => {
    setSubmitting(true)
    try {
      const updated = await userApi.updateMe({
        nickname: values.nickname,
        bio: values.bio,
        avatarUrl: avatarUrl || undefined,
      })
      setUser(updated)
      setAvatarUrl(updated.avatarUrl || '')
      updateProfile({ nickname: updated.nickname, avatarUrl: updated.avatarUrl })
      message.success('保存成功')
    } catch {
      // 错误提示已由 request 拦截器统一处理
    } finally {
      setSubmitting(false)
    }
  }

  if (loading || !user) return <Skeleton avatar active />

  return (
    <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap' }}>
      <Card className="tl-card" style={{ flex: '1 1 240px' }}>
        <SectionTitle size="sm" style={{ marginBottom: 12 }}>
          头像
        </SectionTitle>
        <Avatar size={96} src={avatarUrl || user.avatarUrl} style={{ background: palette.rule }}>
          {user.nickname?.[0]}
        </Avatar>
        <div style={{ marginTop: 16 }}>
          <UploadImage onChange={(urls) => setAvatarUrl(urls[urls.length - 1] || '')} maxCount={1} />
        </div>
      </Card>
      <Card className="tl-card" style={{ flex: '1 1 320px' }}>
        <SectionTitle size="sm" style={{ marginBottom: 12 }}>
          编辑资料
        </SectionTitle>
        <Form form={form} layout="vertical" onFinish={onSave}>
          <Form.Item label="用户名">
            <Input value={user.username} disabled />
          </Form.Item>
          <Form.Item name="nickname" label="昵称" rules={[{ required: true, message: '请输入昵称' }]}>
            <Input maxLength={20} placeholder="昵称（可修改）" />
          </Form.Item>
          <Form.Item name="bio" label="简介">
            <TextArea rows={3} maxLength={100} showCount placeholder="介绍一下自己吧" />
          </Form.Item>
          <Button type="primary" shape="round" htmlType="submit" loading={submitting}>
            保存
          </Button>
        </Form>
      </Card>
    </div>
  )
}
