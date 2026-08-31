import { Button } from 'antd'
import { Link } from 'react-router-dom'
import Eyebrow from '../../components/editorial/Eyebrow'
import { palette } from '../../styles/tokens'

/** 编辑式 404：暖纸居中，大号宋体数字 + 餐饮用语提示 */
export default function NotFound() {
  return (
    <div
      style={{
        minHeight: '60vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: 24,
      }}
    >
      <Eyebrow>404 · NOT FOUND</Eyebrow>
      <h1
        className="editorial-title"
        style={{ fontSize: 96, margin: '8px 0', color: palette.appetite, lineHeight: 1 }}
      >
        404
      </h1>
      <p style={{ color: palette.muted, fontSize: 15, margin: '0 0 20px' }}>
        这块内容不在菜单上 —— 页面不存在或已被移除。
      </p>
      <Link to="/">
        <Button type="primary" shape="round">
          返回首页
        </Button>
      </Link>
    </div>
  )
}
