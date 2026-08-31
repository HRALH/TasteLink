import { Alert } from 'antd'

/**
 * 工程脚手架阶段的页面占位（M10）。
 * 后续里程碑会以真实实现替换各 stub 页面。
 */
export default function PagePlaceholder({ title, milestone }: { title: string; milestone: string }) {
  return (
    <Alert
      type="info"
      showIcon
      message={`${title}页面`}
      description={`待开发（里程碑 ${milestone}），当前为脚手架占位。`}
    />
  )
}
