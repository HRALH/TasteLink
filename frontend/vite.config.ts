import react from '@vitejs/plugin-react'
import { visualizer } from 'rollup-plugin-visualizer'
import { defineConfig } from 'vite'

// https://vite.dev/config/
// dev：把 /api 代理到后端 http://localhost:8080，前端以相对 /api/v1 访问，避免 CORS
export default defineConfig(({ command }) => ({
  plugins: [
    react(),
    // F3-3 体积观测：仅 build 时生成 stats.html（已 gitignore，产物入库前目检）
    ...(command === 'build'
      ? [visualizer({ filename: 'stats.html', gzipSize: true, brotliSize: true })]
      : []),
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        // F3-2 长缓存友好拆分：框架与组件库独立 chunk，业务代码变更不抖动 vendor hash；
        // 配合 F3-1 路由懒加载，admin 代码不进入普通用户首屏。
        // Vite 8(Rolldown)的 manualChunks 仅支持函数形式，按包名精确归组；
        // 传递依赖（scheduler/rc-*/dayjs 等）跟随各自 importer 自然落位
        manualChunks(id: string) {
          const pkg = /node_modules\/((?:@[^/]+\/)?[^/]+)/.exec(id)?.[1]
          if (!pkg) return
          if (pkg === 'react' || pkg === 'react-dom' || pkg === 'react-router-dom' || pkg === 'react-router') {
            return 'vendor-react'
          }
          if (pkg === 'antd' || pkg === '@ant-design/icons') {
            return 'vendor-antd'
          }
        },
      },
    },
  },
}))
