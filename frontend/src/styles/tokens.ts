/**
 * 设计 Token：赤金暖纸 · 美食杂志风
 * 全站 inline 样式统一从此取色/取值，避免散落 hex。与 index.css 的 :root 变量一一对应。
 */

/** 色板 */
export const palette = {
  /** 页面底色：暖纸白 */
  paper: '#FBF6EE',
  /** 卡片底：纯白（杂志剪贴感） */
  surface: '#FFFFFF',
  /** 正文/标题：暖墨黑 */
  ink: '#241A14',
  /** 主色：食欲红 */
  appetite: '#C8102E',
  /** 主色 hover/按压：深红 */
  ember: '#A30D24',
  /** 精选/排行金标 */
  gold: '#B7791F',
  /** 发丝线：纸色 */
  rule: '#E7DDC9',
  /** 次级文字：暖灰棕 */
  muted: '#8A7B6B',
} as const

/** 圆角 */
export const radius = {
  card: 10,
  thumb: 8,
  pill: 999,
} as const

/** 节奏间距 */
export const space = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
} as const

/** 内容宽度 */
export const contentWidth = 1120

/** 阴影 */
export const shadow = {
  card: '0 1px 2px rgba(36, 26, 20, 0.04)',
  hover: '0 1px 2px rgba(36, 26, 20, 0.04), 0 10px 28px rgba(36, 26, 20, 0.07)',
} as const

/** 过渡 */
export const motion = {
  fast: '150ms ease',
  base: '200ms ease',
} as const

/**
 * 字体栈
 * - 标题 Latin：Playfair Display（品牌 wordmark 用 SC 小型大写）
 * - 标题 CJK：Noto Serif SC（宋体；菜单/印刷质感，中文产品必备）
 * - 正文 Latin：Karla
 * - 正文 CJK：Noto Sans SC，回退 PingFang/微软雅黑
 */
export const font = {
  serif: "'Playfair Display', 'Noto Serif SC', 'Songti SC', 'STSong', serif",
  /** 品牌字标：Playfair Display SC 小型大写 */
  sc: "'Playfair Display SC', 'Playfair Display', 'Noto Serif SC', serif",
  sans: "'Karla', 'Noto Sans SC', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif",
} as const
