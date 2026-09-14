import { Button, Empty } from 'antd'

/**
 * 数据加载失败态（F2 三态之一）：与「空数据」明确区分，给重试入口。
 * 替代原先 `.catch(() => {})` 吞错后呈现「暂无内容」的误导（spec F2 现状问题 2）。
 */
export default function QueryError({
  onRetry,
  description = '加载失败，请检查网络后重试',
}: {
  onRetry: () => void
  description?: string
}) {
  return (
    <Empty description={description}>
      <Button shape="round" onClick={() => onRetry()}>
        重试
      </Button>
    </Empty>
  )
}
