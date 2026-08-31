import type { ReactNode } from 'react'

/** 章节小标题：Playfair SC 小型大写 + 字距，暖金色。用于分类/章节引导。 */
export default function Eyebrow({ children }: { children: ReactNode }) {
  return <span className="eyebrow">{children}</span>
}
