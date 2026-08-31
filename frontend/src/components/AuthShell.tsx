import { Card, Typography } from 'antd'
import { Link } from 'react-router-dom'
import type { ReactNode } from 'react'

const { Title } = Typography

/** 认证页通用外壳：居中卡片 + 品牌标题 */
export default function AuthShell({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f5f5f5',
        padding: 16,
      }}
    >
      <Card style={{ width: 380 }}>
        <Title level={3} style={{ textAlign: 'center', marginBottom: 24 }}>
          <Link to="/" style={{ color: '#ff6b35' }}>
            TasteLink
          </Link>{' '}
          {title}
        </Title>
        {children}
      </Card>
    </div>
  )
}
