import { describe, expect, it } from 'vitest'
import { thumb } from '../utils/thumb'

describe('utils/thumb', () => {
  it('OSS URL 拼接 resize 参数', () => {
    expect(thumb('https://tastelink.oss-cn-hangzhou.aliyuncs.com/2026/09/a.jpg', 160)).toBe(
      'https://tastelink.oss-cn-hangzhou.aliyuncs.com/2026/09/a.jpg?x-oss-process=image/resize,w_160',
    )
  })

  it('已有 query 的 OSS URL 用 & 续接', () => {
    expect(thumb('https://b.oss-cn-shanghai.aliyuncs.com/a.png?x-oss-process=style/s', 200)).toBe(
      'https://b.oss-cn-shanghai.aliyuncs.com/a.png?x-oss-process=style/s&x-oss-process=image/resize,w_200',
    )
  })

  it('COS URL 拼接 imageMogr2 thumbnail 参数', () => {
    expect(
      thumb('https://tastelink-1491810581.cos.ap-shanghai.myqcloud.com/uploads/2026/09/a.jpg', 160),
    ).toBe(
      'https://tastelink-1491810581.cos.ap-shanghai.myqcloud.com/uploads/2026/09/a.jpg?imageMogr2/thumbnail/160x',
    )
  })

  it('已有 query 的 COS URL 用 & 续接', () => {
    expect(thumb('https://b.cos.ap-shanghai.myqcloud.com/a.jpg?v=1', 200)).toBe(
      'https://b.cos.ap-shanghai.myqcloud.com/a.jpg?v=1&imageMogr2/thumbnail/200x',
    )
  })

  it('local 存储与 localhost 原样返回', () => {
    expect(thumb('/static/uploads/2026/09/a.jpg', 160)).toBe('/static/uploads/2026/09/a.jpg')
    expect(thumb('http://localhost:8080/static/uploads/a.jpg', 160)).toBe(
      'http://localhost:8080/static/uploads/a.jpg',
    )
  })

  it('非对象存储特征域名（如外部 CDN）原样返回', () => {
    expect(thumb('https://cdn.example.com/a.jpg', 160)).toBe('https://cdn.example.com/a.jpg')
    // 域名里出现 myqcloud.com 但不是 .myqcloud.com 后缀 → 不应误判
    expect(thumb('https://not-myqcloud.com.evil.example.com/a.jpg', 160)).toBe(
      'https://not-myqcloud.com.evil.example.com/a.jpg',
    )
  })

  it('空 url / 非法宽度原样返回', () => {
    expect(thumb('', 160)).toBe('')
    expect(thumb('https://b.oss-cn-hangzhou.aliyuncs.com/a.jpg', 0)).toBe(
      'https://b.oss-cn-hangzhou.aliyuncs.com/a.jpg',
    )
    expect(thumb('https://b.cos.ap-shanghai.myqcloud.com/a.jpg', 0)).toBe(
      'https://b.cos.ap-shanghai.myqcloud.com/a.jpg',
    )
  })

  it('宽度取整（DPR 换算产生小数）', () => {
    expect(thumb('https://b.oss-cn-hangzhou.aliyuncs.com/a.jpg', 160.6)).toContain('w_161')
    expect(thumb('https://b.cos.ap-shanghai.myqcloud.com/a.jpg', 240.4)).toContain('thumbnail/240x')
  })
})
