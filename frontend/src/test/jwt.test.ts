import { describe, expect, it } from 'vitest'
import { decodeJwtPayload, getRoleFromToken, isTokenExpired } from '../utils/jwt'

function jwt(role: string): string {
  const payload = btoa(JSON.stringify({ sub: 'x', userId: 1, role }))
  return `h.${payload}.s`
}

describe('utils/jwt', () => {
  it('getRoleFromToken reads role claim', () => {
    expect(getRoleFromToken(jwt('USER'))).toBe('USER')
    expect(getRoleFromToken(jwt('ADMIN'))).toBe('ADMIN')
  })

  it('returns undefined for unknown/empty token', () => {
    expect(getRoleFromToken(jwt('OTHER'))).toBeUndefined()
    expect(getRoleFromToken(null)).toBeUndefined()
    expect(getRoleFromToken(undefined)).toBeUndefined()
    expect(getRoleFromToken('not.a.jwt')).toBeUndefined()
    expect(getRoleFromToken('onlyonepart')).toBeUndefined()
  })

  it('decodeJwtPayload parses payload', () => {
    expect(decodeJwtPayload(jwt('ADMIN'))?.role).toBe('ADMIN')
    expect(decodeJwtPayload('bad')).toBeNull()
  })

  // F5-2 回归：base64url padding 修复 — 构造长度非 4 倍数的 payload（标准 JWT 字段长度）
  it('decodeJwtPayload 补 base64url padding（payload 长度非 4 倍数）', () => {
    // {"role":"USER"} → eyJyb2xlIjoiVVNFUiJ9（长度 24，4 倍数）——删一个字符模拟非 4 倍数
    // 这里用一个真实的非 4 倍数长度 payload：{"sub":"abc"} → eyJzdWIiOiJhYmMifQ（长度 20，非 4 倍数）
    // 用 btoa 生成标准长度，手动拼接成「缺 padding」的 base64url 形态（去掉 `=`）
    const payloadObj = { sub: 'abc', userId: 1, role: 'USER', exp: 9999999999 }
    const json = JSON.stringify(payloadObj)
    const base64 = btoa(json)
    // 转 base64url：换字符表 + 去 padding
    const base64url = base64.replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_')
    // 确认 base64url 长度非 4 倍数（若恰好是 4 倍数这段测试就没意义）
    expect(base64url.length % 4).not.toBe(0)
    const token = `head.${base64url}.sig`
    const decoded = decodeJwtPayload(token)
    expect(decoded).toMatchObject({ sub: 'abc', userId: 1, role: 'USER', exp: 9999999999 })
  })
})

describe('utils/jwt isTokenExpired (F5-2)', () => {
  it('无 token / 解析失败 / 无 exp → true（保守视为过期）', () => {
    expect(isTokenExpired(null)).toBe(true)
    expect(isTokenExpired(undefined)).toBe(true)
    expect(isTokenExpired('')).toBe(true)
    expect(isTokenExpired('not.a.jwt')).toBe(true)
    // 有 payload 但无 exp claim
    expect(isTokenExpired(jwt('USER'))).toBe(true)
  })

  it('exp > now → false（有效）', () => {
    const future = Math.floor(Date.now() / 1000) + 3600
    const payload = btoa(JSON.stringify({ sub: 'x', role: 'USER', exp: future }))
    expect(isTokenExpired(`h.${payload}.s`)).toBe(false)
  })

  it('exp ≤ now → true（已过期）', () => {
    const past = Math.floor(Date.now() / 1000) - 1
    const payload = btoa(JSON.stringify({ sub: 'x', role: 'USER', exp: past }))
    expect(isTokenExpired(`h.${payload}.s`)).toBe(true)
    // 精确等于 now 也算过期（≤）
    const now = Math.floor(Date.now() / 1000)
    const payloadNow = btoa(JSON.stringify({ sub: 'x', role: 'USER', exp: now }))
    expect(isTokenExpired(`h.${payloadNow}.s`)).toBe(true)
  })

  it('非数字 exp → true', () => {
    const payload = btoa(JSON.stringify({ sub: 'x', role: 'USER', exp: 'oops' }))
    expect(isTokenExpired(`h.${payload}.s`)).toBe(true)
  })
})
