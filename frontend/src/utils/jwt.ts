/**
 * JWT payload 解码（v2 FE-0）
 *
 * 仅用于读取 UI 建议性 claims（如 role），**不作鉴权决策**：
 * 真正的管理员鉴权仍由后端 `/api/v1/admin/**` 的 `hasRole('ADMIN')` 强制（403），
 * 客户端解出的 role 只用于显隐管理入口与路由守卫。
 *
 * 后端 `LoginVO`/`UserVO` 未在响应体返回 role，role 仅存在于 JWT claim 中，
 * 故前端从 token 解出。不引入 jwt-decode 依赖，手解 base64url payload。
 */

export type Role = 'USER' | 'ADMIN'

interface JwtPayload {
  sub?: string
  userId?: number
  /** 用户角色，v2 Phase B 起写入 */
  role?: string
  [key: string]: unknown
}

function base64urlDecode(segment: string): string {
  // base64url → base64
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/')
  const binary = atob(base64)
  // 转回 UTF-8（payload 是 JSON，可能含中文）
  try {
    return decodeURIComponent(encodeURIComponent(binary))
  } catch {
    return binary
  }
}

export function decodeJwtPayload(token: string | null | undefined): JwtPayload | null {
  if (!token || typeof token !== 'string' || typeof atob === 'undefined') return null
  const parts = token.split('.')
  if (parts.length < 2) return null
  try {
    return JSON.parse(base64urlDecode(parts[1])) as JwtPayload
  } catch {
    return null
  }
}

export function getRoleFromToken(token: string | null | undefined): Role | undefined {
  const role = decodeJwtPayload(token)?.role
  return role === 'ADMIN' || role === 'USER' ? role : undefined
}
