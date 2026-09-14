import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ConfigProvider } from 'antd'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import zhCN from 'antd/locale/zh_CN'
import 'antd/dist/reset.css'
import './index.css'
import App from './App.tsx'
import { palette, font } from './styles/tokens'

/**
 * React Query 全局客户端（F2 数据获取层）：
 * - retry 1：瞬时网络抖动给一次重试，避免直接落错误态
 * - refetchOnWindowFocus false：社区浏览场景，切窗不打扰
 */
const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ConfigProvider
        locale={zhCN}
      theme={{
        token: {
          colorPrimary: palette.appetite,
          colorBgLayout: palette.paper,
          colorText: palette.ink,
          colorTextSecondary: palette.muted,
          colorTextDescription: palette.muted,
          colorLink: palette.appetite,
          colorLinkHover: palette.ember,
          colorLinkActive: palette.ember,
          colorSuccess: '#3f8e5c',
          colorWarning: palette.gold,
          colorError: palette.appetite,
          borderRadius: 10,
          fontFamily: font.sans,
          fontFamilyCode: font.sans,
        },
        components: {
          Card: {
            colorBgContainer: palette.surface,
            boxShadow: '0 1px 2px rgba(36, 26, 20, 0.04)',
            boxShadowTertiary: '0 1px 2px rgba(36, 26, 20, 0.04)',
            borderRadiusLG: 10,
            paddingLG: 20,
          },
          Menu: {
            itemSelectedColor: palette.appetite,
            itemSelectedBg: 'transparent',
            horizontalItemSelectedColor: palette.appetite,
            itemColor: palette.ink,
            colorItemBg: 'transparent',
          },
          Button: {
            fontWeight: 600,
            controlHeight: 38,
            primaryShadow: 'none',
            defaultBorderColor: palette.rule,
            defaultColor: palette.ink,
          },
          Segmented: {
            itemSelectedBg: palette.appetite,
            itemSelectedColor: '#ffffff',
            itemColor: palette.muted,
            itemHoverColor: palette.ink,
            trackBg: 'rgba(36, 26, 20, 0.05)',
            borderRadius: 999,
            borderRadiusSM: 999,
          },
          Tag: {
            defaultBg: 'rgba(183, 121, 31, 0.12)',
            defaultColor: palette.gold,
            borderRadiusSM: 4,
          },
          Divider: {
            colorSplit: palette.rule,
          },
          Layout: {
            headerBg: palette.surface,
            headerHeight: 64,
            footerBg: palette.surface,
          },
          Typography: {
            colorTextSecondary: palette.muted,
          },
          Pagination: {
            itemActiveBg: palette.appetite,
          },
          Rate: {
            starColor: palette.appetite,
          },
          Avatar: {
            colorBgContainer: palette.rule,
          },
          Empty: {
            colorText: palette.muted,
          },
          Input: {
            activeBorderColor: palette.appetite,
            hoverBorderColor: '#d4c39e',
          },
          Select: {
            activeBorderColor: palette.appetite,
            hoverBorderColor: '#d4c39e',
          },
          Form: {
            labelColor: palette.ink,
          },
        },
      }}
    >
        <App />
      </ConfigProvider>
    </QueryClientProvider>
  </StrictMode>,
)
