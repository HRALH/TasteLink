import { Card } from 'antd'
import { Link } from 'react-router-dom'
import type { ReactNode } from 'react'
import { palette } from '../styles/tokens'

/**
 * 认证页通用外壳：暖纸居中卡片 + 品牌字标 + kicker。
 * 编辑式入场序列：左上暖光氛围 + 印章水印「味」→ 品牌字标 draw-in → eyebrow 滑入
 * → 卡片呼吸入场 → 表单字段级联（由 .tl-auth-form nth-child 驱动）。
 */
export default function AuthShell({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
        overflow: 'hidden',
        background: palette.paper,
        padding: 16,
      }}
    >
      {/* 暖光氛围（左上食欲红极淡） */}
      <div className="tl-aura" style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }} aria-hidden />

      {/* 印章水印：宋体大号「味」，极低透明度作杂志质感，裁切于卡片之后 */}
      <span
        aria-hidden
        style={{
          position: 'absolute',
          right: '-3vw',
          bottom: '-12vh',
          fontFamily: 'var(--font-serif)',
          fontSize: '38vw',
          lineHeight: 1,
          fontWeight: 700,
          color: palette.appetite,
          opacity: 0.045,
          userSelect: 'none',
          pointerEvents: 'none',
          zIndex: 0,
        }}
      >
        味
      </span>

      <Card
        className="tl-auth-card"
        style={{
          position: 'relative',
          zIndex: 1,
          width: 400,
          boxShadow: '0 1px 2px rgba(36, 26, 20, 0.04), 0 12px 36px rgba(36, 26, 20, 0.08)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Link
            to="/"
            className="tl-brand-in"
            style={{
              fontFamily: 'var(--font-sc)',
              fontSize: 30,
              fontWeight: 700,
              color: palette.appetite,
              letterSpacing: '0.02em',
              display: 'inline-block',
            }}
          >
            TasteLink
          </Link>
          <div className="eyebrow tl-eyebrow-in" style={{ marginTop: 6 }}>
            {title}
          </div>
        </div>
        {children}
      </Card>
    </div>
  )
}
