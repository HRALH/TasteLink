import { Avatar, Button, Dropdown, Layout, Space, message } from 'antd'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import type { MenuProps } from 'antd'
import { useAuthStore } from '../../store/authStore'
import { palette, contentWidth } from '../../styles/tokens'

const { Header, Content, Footer } = Layout

const NAV = [
  { key: '/', label: '首页' },
  { key: '/shops', label: '店铺' },
]

/** 主体布局：编辑式顶栏（品牌小型大写 + 下划线 active 导航）+ 内容 + 暖色页脚 */
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
    <Layout style={{ minHeight: '100vh', background: palette.paper }}>
      <Header
        style={{
          position: 'sticky',
          top: 0,
          zIndex: 20,
          height: 64,
          paddingInline: 0,
          background: palette.surface,
          borderBottom: `1px solid ${palette.rule}`,
        }}
      >
        <div
          style={{
            maxWidth: contentWidth,
            margin: '0 auto',
            width: '100%',
            height: '100%',
            paddingInline: 24,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <Space size={36} align="center">
            <Link
              to="/"
              style={{
                fontFamily: 'var(--font-sc)',
                fontSize: 22,
                fontWeight: 700,
                color: palette.appetite,
                letterSpacing: '0.02em',
                whiteSpace: 'nowrap',
              }}
            >
              TasteLink
            </Link>
            <nav style={{ display: 'flex', gap: 6 }} aria-label="主导航">
              {NAV.map((item) => {
                const active = selectedKey === item.key
                return (
                  <Link
                    key={item.key}
                    to={item.key}
                    style={{
                      position: 'relative',
                      padding: '8px 6px',
                      fontSize: 15,
                      color: active ? palette.appetite : palette.ink,
                      fontWeight: active ? 600 : 500,
                    }}
                  >
                    {item.label}
                    {active && (
                      <span
                        style={{
                          position: 'absolute',
                          left: 6,
                          right: 6,
                          bottom: 0,
                          height: 2,
                          background: palette.appetite,
                          borderRadius: 2,
                        }}
                      />
                    )}
                  </Link>
                )
              })}
            </nav>
          </Space>

          <Space size={12}>
            {isLoggedIn && userInfo ? (
              <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
                <Space size={8} style={{ cursor: 'pointer' }}>
                  <Avatar src={userInfo.avatarUrl} size={32} style={{ background: palette.rule }}>
                    {userInfo.nickname?.[0]}
                  </Avatar>
                  <span style={{ fontSize: 14, color: palette.ink }}>{userInfo.nickname}</span>
                </Space>
              </Dropdown>
            ) : (
              <>
                <Link to="/login">
                  <Button shape="round">登录</Button>
                </Link>
                <Link to="/register">
                  <Button type="primary" shape="round">
                    注册
                  </Button>
                </Link>
              </>
            )}
          </Space>
        </div>
      </Header>

      <Content style={{ maxWidth: contentWidth, margin: '0 auto', width: '100%', padding: '28px 24px 48px' }}>
        <Outlet />
      </Content>

      <Footer
        style={{
          background: palette.surface,
          borderTop: `1px solid ${palette.rule}`,
          padding: '24px 24px 32px',
        }}
      >
        <div
          style={{
            maxWidth: contentWidth,
            margin: '0 auto',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: 8,
          }}
        >
          <span
            style={{ fontFamily: 'var(--font-sc)', fontWeight: 700, color: palette.appetite, fontSize: 15 }}
          >
            TasteLink
          </span>
          <span style={{ color: palette.muted, fontSize: 13 }}>
            餐饮口碑社区 · 用文字留住每一口滋味
          </span>
        </div>
      </Footer>
    </Layout>
  )
}
