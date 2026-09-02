import { http, HttpResponse } from 'msw'

/** 后端统一响应体 R（与 request 拦截器解包契约一致） */
const ok = (data: unknown, code = 0, message = 'success') => HttpResponse.json({ code, message, data })

/** 一份可复用的店铺详情（含 ShopDetailVO 全字段） */
export const aShop = {
  id: 1,
  name: '测试小店',
  categoryId: 2,
  categoryName: '日料',
  city: '上海',
  address: '南京路1号',
  coverUrl: 'https://cdn.example/cover.jpg',
  phone: '021-1',
  description: '很好',
  avgRating: 4.5,
  reviewCount: 12,
  likeCount: 3,
  topReviews: [] as unknown[],
}

/** 构造带 role claim 的 JWT（header.payload.sig，payload 为 base64url/UTF-8 JSON） */
function tokenWithRole(role: 'USER' | 'ADMIN', userId = 1): string {
  const payload = btoa(JSON.stringify({ sub: role === 'ADMIN' ? 'admin' : 'user', userId, role }))
  return `head.${payload}.sig`
}

/** happy-path handlers；特定场景由测试用 server.use() 覆盖 */
export const handlers = [
  http.post('/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as { username: string }
    const isAdmin = body.username === 'admin'
    return ok({
      token: tokenWithRole(isAdmin ? 'ADMIN' : 'USER', isAdmin ? 1 : 2),
      expiresInSec: 3600,
      userId: isAdmin ? 1 : 2,
      username: body.username,
      nickname: body.username,
      avatarUrl: '',
    })
  }),
  http.get('/api/v1/users/me', () =>
    ok({ id: 1, username: 'admin', nickname: 'admin', avatarUrl: '', bio: '', followingCount: 0, followerCount: 0, reviewCount: 0, hasFollowed: false }),
  ),
  http.get('/api/v1/shops', () => ok({ records: [aShop], total: 1, current: 1, size: 10, pages: 1 })),
  http.get('/api/v1/shops/categories', () =>
    ok([{ id: 2, code: 'japanese', name: '日料', iconUrl: '', sortOrder: 1 }]),
  ),
  http.get('/api/v1/shops/:id', () => ok(aShop)),
  http.put('/api/v1/admin/shops/:id', () => ok({ ...aShop, name: '已更新' })),
  http.delete('/api/v1/admin/shops/:id', () => ok(null)),
]
