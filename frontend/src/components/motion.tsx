import type { CSSProperties, ReactNode } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { useInView } from '../hooks/useInView'

/**
 * 动效原语组件（配 src/index.css 的 .tl-* 类）。
 * 全部尊重 prefers-reduced-motion：useInView 在减少动效/无 IO 时直接以可见态返回。
 * 交错延迟由 utils/motion 的 staggerDelay 提供（保持本文件仅导出组件，fast-refresh 友好）。
 */

interface RevealProps {
  children: ReactNode
  /** 入场延迟（ms），配合 staggerDelay 做列交错 */
  delay?: number
  className?: string
  style?: CSSProperties
}

/**
 * 单元素滚动入场：进入视口时由 .tl-reveal → .tl-reveal--shown 触发 expo-out 上升淡入。
 * 用于章节标题、卡片等。列表交错由调用方传 delay={staggerDelay(i)}。
 */
export function Reveal({ children, delay = 0, className, style }: RevealProps) {
  const [ref, shown] = useInView<HTMLDivElement>()
  return (
    <div
      ref={ref}
      className={`tl-reveal${shown ? ' tl-reveal--shown' : ''}${className ? ` ${className}` : ''}`}
      style={{ ...style, transitionDelay: shown ? `${delay}ms` : undefined }}
    >
      {children}
    </div>
  )
}

/**
 * 路由切换过渡：以 location.pathname 为 key，导航时整体 remount 重放 .tl-page-in。
 * 包裹在 MainLayout 的 <Outlet/> 外，所有受布局页面获得统一淡入上浮。
 */
export function PageTransition() {
  const location = useLocation()
  return (
    <div key={location.pathname} className="tl-page-in">
      <Outlet />
    </div>
  )
}
