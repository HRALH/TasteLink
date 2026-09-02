import { describe, expect, it } from 'vitest'
import { decodeJwtPayload, getRoleFromToken } from '../utils/jwt'

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
})
