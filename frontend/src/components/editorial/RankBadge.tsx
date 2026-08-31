import { palette } from '../../styles/tokens'

/**
 * 排行金标：仅用于“排序即信息”的榜单（如热门 No.1/2/3）。
 * 普通列表不加无意义序号（避免装饰性编号反模式）。
 */
export default function RankBadge({ rank }: { rank: number }) {
  const top3 = rank <= 3
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        minWidth: 26,
        height: 26,
        padding: '0 8px',
        borderRadius: 999,
        fontFamily: "'Playfair Display', serif",
        fontSize: 14,
        fontWeight: 700,
        color: top3 ? '#ffffff' : palette.ink,
        background: top3 ? palette.gold : 'rgba(36, 26, 20, 0.06)',
        letterSpacing: '0.04em',
      }}
    >
      {rank}
    </span>
  )
}
