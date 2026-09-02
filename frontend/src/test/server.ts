import { setupServer } from 'msw/node'
import { handlers } from './handlers'

/** msw node 服务端（vitest）。默认装载 handlers.ts 的 happy-path，测试可用 server.use() 覆盖。 */
export const server = setupServer(...handlers)
