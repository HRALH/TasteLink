import type { CSSProperties, ReactNode } from 'react'
import Eyebrow from './Eyebrow'

const sizes: Record<'lg' | 'md' | 'sm', { fontSize: number; marginTop: number; marginBottom: number }> = {
  lg: { fontSize: 26, marginTop: 6, marginBottom: 20 },
  md: { fontSize: 20, marginTop: 4, marginBottom: 16 },
  sm: { fontSize: 17, marginTop: 4, marginBottom: 12 },
}

interface SectionTitleProps {
  children: ReactNode
  /** 可选 eyebrow（小型大写引导词） */
  eyebrow?: ReactNode
  size?: 'lg' | 'md' | 'sm'
  style?: CSSProperties
}

/** 章节标题：eyebrow（可选）+ 宋体标题。替代散落的 AntD Typography.Title 章节头。 */
export default function SectionTitle({ children, eyebrow, size = 'md', style }: SectionTitleProps) {
  const s = sizes[size]
  return (
    <div style={{ marginBottom: s.marginBottom, ...style }}>
      {eyebrow ? <Eyebrow>{eyebrow}</Eyebrow> : null}
      <h2
        className="editorial-title"
        style={{ margin: 0, marginTop: s.marginTop, fontSize: s.fontSize }}
      >
        {children}
      </h2>
    </div>
  )
}
