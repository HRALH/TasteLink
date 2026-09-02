import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// 测试基线（v2 FE-0）：vitest + jsdom + @testing-library + msw。
// 显式 import { describe, it, expect } from 'vitest'（globals:false，避免动 tsconfig types）。
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    environmentOptions: {
      jsdom: { url: 'http://localhost/' },
    },
    globals: false,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/test/**/*.{test,spec}.{ts,tsx}'],
  },
})
