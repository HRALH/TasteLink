import { useEffect, useRef, useState } from 'react'

interface UseInViewOptions {
  /** 仅首次进入视口触发后停止观察（默认 true：入场后保持可见，避免反复闪动） */
  once?: boolean
  /** IntersectionObserver rootMargin，默认底部留 -8% 触发稍晚一点更自然 */
  rootMargin?: string
  /** 相交比例阈值 */
  threshold?: number
}

/** prefers-reduced-motion：减少动效时直接以可见态渲染、跳过入场 */
function prefersReducedMotion(): boolean {
  if (typeof window === 'undefined' || !window.matchMedia) return false
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

/** 减少动效 或 运行环境不支持 IntersectionObserver 时，直接可见（不在 effect 里同步 setState） */
function shouldShowImmediately(): boolean {
  return typeof IntersectionObserver === 'undefined' || prefersReducedMotion()
}

/**
 * 元素进入视口检测（IntersectionObserver）。
 * - 默认 once：首次相交后 disconnect，保持可见
 * - reduced-motion / 不支持 IO 时，初始即 shown=true（内容不隐藏）
 * - setState 仅发生在异步 IO 回调中，避免 effect 内同步 setState 造成级联渲染
 */
export function useInView<T extends Element = HTMLDivElement>(options: UseInViewOptions = {}) {
  const { once = true, rootMargin = '0px 0px -8% 0px', threshold = 0.08 } = options
  const ref = useRef<T | null>(null)
  const [shown, setShown] = useState<boolean>(shouldShowImmediately)

  useEffect(() => {
    const el = ref.current
    if (!el || shouldShowImmediately()) return
    const io = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            setShown(true)
            if (once) io.disconnect()
          } else if (!once) {
            setShown(false)
          }
        }
      },
      { rootMargin, threshold },
    )
    io.observe(el)
    return () => io.disconnect()
  }, [once, rootMargin, threshold])

  return [ref, shown] as const
}
