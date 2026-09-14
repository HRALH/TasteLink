import { useEffect } from 'react'

const BASE_TITLE = 'TasteLink · 餐饮口碑社区'

/**
 * F5-3：页面级文档标题。详情页可传「店铺名 - TasteLink」等带语义的标题，
 * 卸载/切页时自动回退到 BASE_TITLE。
 * - 传 string：直接拼 `${input} · ${BASE}`，若 input 已含 BASE 则直接用 input
 * - 传 undefined/空：只显示 BASE
 */
export function useDocumentTitle(input?: string): void {
  useEffect(() => {
    const title = input && input.trim() ? `${input} · ${BASE_TITLE}` : BASE_TITLE
    document.title = title
    return () => {
      document.title = BASE_TITLE
    }
  }, [input])
}
