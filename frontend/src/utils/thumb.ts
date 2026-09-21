/**
 * 对象存储缩略图（F4-2）：列表/卡片/缩略位不加载原图（手机原图可达 5MB）。
 * 按域名特征识别是哪家对象存储，各自拼该家的原生图片处理参数：
 *   - 阿里云 OSS（{bucket}.oss-{region}.aliyuncs.com）→ `?x-oss-process=image/resize,w_{w}`
 *   - 腾讯云 COS（{bucket}.cos.{region}.myqcloud.com）→ `?imageMogr2/thumbnail/{w}x`
 * local 存储（/static/**）、localhost、其它来源一律原样返回。
 *
 * COS 侧：公有读桶 + 默认域名可直接拼参数、无需签名；首次使用会自动开通并绑定
 * 数据万象，按处理请求数计费（单价低，省下的外网下行流量费通常更多）。
 *
 * 注意：若后端启用了自定义访问域名（storage.oss.domain / storage.cos.domain，如挂
 * CDN），域名特征需同步扩展——否则这里会静默退回加载原图，功能不报错但白耗流量。
 */

/** 阿里云 OSS 默认域名特征：{bucket}.oss-{region}.aliyuncs.com */
const OSS_HOST_RE = /^https?:\/\/[^/]*\.aliyuncs\.com(?::\d+)?\//i
/** 腾讯云 COS 默认域名特征：{bucket}.cos.{region}.myqcloud.com */
const COS_HOST_RE = /^https?:\/\/[^/]*\.myqcloud\.com(?::\d+)?\//i

/**
 * 返回按宽度 w（px）等比缩小的图床 URL；非对象存储 URL 原样返回。
 * w 取展示尺寸 × DPR（如 80px 缩略图传 160）。
 */
export function thumb(url: string, w: number): string {
  if (!url || !Number.isFinite(w) || w <= 0) return url
  const px = Math.round(w)
  const sep = url.includes('?') ? '&' : '?'
  if (OSS_HOST_RE.test(url)) {
    return `${url}${sep}x-oss-process=image/resize,w_${px}`
  }
  if (COS_HOST_RE.test(url)) {
    return `${url}${sep}imageMogr2/thumbnail/${px}x`
  }
  return url
}
