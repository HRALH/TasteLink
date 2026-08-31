import { Button, Form, Input, message } from 'antd'
import { Link, useNavigate } from 'react-router-dom'
import AuthShell from '../../components/AuthShell'
import { authApi } from '../../api/auth'

interface RegisterFormValues {
  username: string
  password: string
  confirm: string
}

// 密码规则：长度≥8 且同时含字母与数字（docs/01 §3.1）
const passwordRules = [
  { required: true, message: '请输入密码' },
  { min: 8, message: '密码至少 8 位' },
  {
    validator: (_: unknown, value: string) => {
      if (!value) return Promise.resolve()
      const hasLetter = /[a-zA-Z]/.test(value)
      const hasNumber = /[0-9]/.test(value)
      return hasLetter && hasNumber
        ? Promise.resolve()
        : Promise.reject(new Error('密码需同时包含字母和数字'))
    },
  },
]

export default function RegisterPage() {
  const [form] = Form.useForm<RegisterFormValues>()
  const navigate = useNavigate()

  const onFinish = async (values: RegisterFormValues) => {
    try {
      await authApi.register({ username: values.username, password: values.password })
      message.success('注册成功，请登录')
      navigate('/login', { replace: true })
    } catch {
      // 错误提示已由 request 拦截器统一处理（如用户名已存在 40901）
    }
  }

  return (
    <AuthShell title="注册">
      <Form form={form} layout="vertical" onFinish={onFinish} requiredMark={false}>
        <Form.Item
          name="username"
          label="用户名"
          rules={[
            { required: true, message: '请输入用户名' },
            { min: 3, message: '用户名至少 3 位' },
            { pattern: /^[a-zA-Z0-9_]+$/, message: '仅支持字母、数字、下划线' },
          ]}
        >
          <Input placeholder="登录用户名（注册后不可修改）" autoComplete="username" />
        </Form.Item>
        <Form.Item name="password" label="密码" rules={passwordRules} hasFeedback>
          <Input.Password placeholder="至少 8 位，含字母和数字" autoComplete="new-password" />
        </Form.Item>
        <Form.Item
          name="confirm"
          label="确认密码"
          dependencies={['password']}
          hasFeedback
          rules={[
            { required: true, message: '请再次输入密码' },
            ({ getFieldValue }) => ({
              validator: (_: unknown, value: string) =>
                !value || getFieldValue('password') === value
                  ? Promise.resolve()
                  : Promise.reject(new Error('两次输入的密码不一致')),
            }),
          ]}
        >
          <Input.Password placeholder="再次输入密码" autoComplete="new-password" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block>
          注册
        </Button>
      </Form>
      <div style={{ textAlign: 'center', marginTop: 16 }}>
        已有账号？<Link to="/login">去登录</Link>
      </div>
    </AuthShell>
  )
}
