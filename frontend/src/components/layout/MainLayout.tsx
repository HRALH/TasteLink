import { Avatar, Button, Dropdown, Layout, Menu, Space, message } from 'antd'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import type { MenuProps } from 'antd'
import { useAuthStore } from '../../store/authStore'

const { Header, Content } = Layout

const navItems: MenuProps['items'] = [
  { key: '/', label: <Link to="/">首页</Link> },
  { key: '/shops', label: <Link to="/shops">店铺</Link> },
]

/** 主体布局：顶栏导航 + 登录态 + Outlet */
export default function MainLayout() {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn)
  const userInfo = useAuthStore((s) => s.userInfo)
  const logout = useAuthStore((s) => s.logout)
  const navigate = useNavigate()
  const location = useLocation()

  // 选中态：按一级路径匹配
  const selectedKey =
    location.pathname === '/'
      ? '/'
      : location.pathname.startsWith('/shops')
        ? '/shops'
        : location.pathname

  const handleLogout = () => {
    logout()
    message.success('已退出登录')
    navigate('/')
  }

  const userMenuItems: MenuProps['items'] = [
    { key: 'me', label: <Link to="/me">我的主页</Link> },
    { type: 'divider' as const },
    { key: 'logout', label: '退出登录', onClick: handleLogout },
  ]

  return (
    <Layout style={{ minHeight: '100vh', background: '#f5f5f5' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: '#fff',
          borderBottom: '1px solid #f0f0f0',
          paddingInline: 24,
          position: 'sticky',
          top: 0,
          zIndex: 10,
        }}
      >
        <Space size={40}>
          <Link
            to="/"
            style={{ fontSize: 20, fontWeight: 700, color: '#ff6b35', whiteSpace: 'nowrap' }}
          >
            TasteLink
          </Link>
          <Menu
            mode="horizontal"
            selectedKeys={[selectedKey]}
            items={navItems}
            style={{ flex: 1, minWidth: 0, borderBottom: 'none' }}
          />
        </Space>
        <Space>
          {isLoggedIn && userInfo ? (
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <Space size={8} style={{ cursor: 'pointer' }}>
                <Avatar src={userInfo.avatarUrl} size="small">
                  {userInfo.nickname?.[0]}
                </Avatar>
                <span>{userInfo.nickname}</span>
              </Space>
            </Dropdown>
          ) : (
            <>
              <Link to="/login">
                <Button>登录</Button>
              </Link>
              <Link to="/register">
                <Button type="primary">注册</Button>
              </Link>
            </>
          )}
        </Space>
      </Header>
      <Content style={{ maxWidth: 1100, margin: '0 auto', width: '100%', padding: '24px 16px' }}>
        <Outlet />
      </Content>
    </Layout>
  )
}
