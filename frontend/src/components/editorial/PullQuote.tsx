import type { CSSProperties, ReactNode } from 'react'
import { palette } from '../../styles/tokens'

interface PullQuoteProps {
  children: ReactNode
  style?: CSSProperties
}

/**
 * 杂志引文：左侧留白槽里的大号食欲红开引号 + 宋体正文。
 * 用于点评详情正文，使“口味写作”如杂志摘录 —— 本设计系统的标志性元素。
 */
export default function PullQuote({ children, style }: PullQuoteProps) {
  return (
    <div className="pullquote" style={{ position: 'relative', paddingLeft: 34, ...style }}>
      <span
        aria-hidden
        style={{
          position: 'absolute',
          left: 0,
          top: -8,
          fontFamily: 'var(--font-serif)',
          color: palette.appetite,
          fontSize: 40,
          lineHeight: 1,
          fontWeight: 700,
          opacity: 0.85,
        }}
      >
        {'❝'}
      </span>
      <div style={{ position: 'relative' }}>{children}</div>
    </div>
  )
}
