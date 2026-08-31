import { Button, Form, Input, message } from 'antd'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import AuthShell from '../../components/AuthShell'
import { authApi } from '../../api/auth'
import { useAuthStore } from '../../store/authStore'
import type { LoginBody } from '../../types/api'

export default function LoginPage() {
  const [form] = Form.useForm<LoginBody>()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const login = useAuthStore((s) => s.login)

  const onFinish = async (values: LoginBody) => {
    try {
      const res = await authApi.login(values)
      login(res.token, {
        userId: res.userId,
        username: res.username,
        nickname: res.nickname,
        avatarUrl: res.avatarUrl,
      })
      message.success('登录成功')
      const redirect = params.get('redirect')
      navigate(redirect ? decodeURIComponent(redirect) : '/', { replace: true })
    } catch {
      // 错误提示已由 request 拦截器统一处理
    }
  }

  return (
    <AuthShell title="登录">
      <Form form={form} layout="vertical" onFinish={onFinish} requiredMark={false}>
        <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
          <Input placeholder="用户名" autoComplete="username" />
        </Form.Item>
        <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
          <Input.Password placeholder="密码" autoComplete="current-password" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block>
          登录
        </Button>
      </Form>
      <div style={{ textAlign: 'center', marginTop: 16 }}>
        还没有账号？<Link to="/register">去注册</Link>
      </div>
    </AuthShell>
  )
}
