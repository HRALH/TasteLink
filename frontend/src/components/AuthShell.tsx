import { Card } from 'antd'
import { Link } from 'react-router-dom'
import type { ReactNode } from 'react'
import { palette } from '../styles/tokens'

/** 认证页通用外壳：暖纸居中卡片 + 品牌字标（Playfair SC）+ kicker */
export default function AuthShell({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: palette.paper,
        padding: 16,
      }}
    >
      <Card
        className="tl-card"
        style={{
          width: 400,
          boxShadow: '0 1px 2px rgba(36, 26, 20, 0.04), 0 12px 36px rgba(36, 26, 20, 0.08)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Link
            to="/"
            style={{
              fontFamily: 'var(--font-sc)',
              fontSize: 30,
              fontWeight: 700,
              color: palette.appetite,
              letterSpacing: '0.02em',
            }}
          >
            TasteLink
          </Link>
          <div className="eyebrow" style={{ marginTop: 6 }}>
            {title}
          </div>
        </div>
        {children}
      </Card>
    </div>
  )
}
