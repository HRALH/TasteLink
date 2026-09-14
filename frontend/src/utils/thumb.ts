/**
 * OSS 缩略图（F4-2）：列表/卡片/缩略位不加载原图（手机原图可达 5MB）。
 * 仅当 URL 指向阿里云 OSS（host 形如 {bucket}.{endpoint}.aliyuncs.com，对齐后端
 * OssFileStorageServiceImpl 的拼装）时拼接 `x-oss-process=image/resize,w_{w}`；
 * local 存储（/static/**）、localhost、其它来源一律原样返回。
 * 注意：若后端启用自定义 OSS domain（oss.domain 配置），此处的域名特征需同步扩展。
 */

/** OSS 默认域名特征：{bucket}.oss-{region}.aliyuncs.com */
const OSS_HOST_RE = /^https?:\/\/[^/]*\.aliyuncs\.com(?::\d+)?\//i

/**
 * 返回按宽度 w（px）等比缩小的图床 URL；非 OSS URL 原样返回。
 * w 取展示尺寸 × DPR（如 80px 缩略图传 160）。
 */
export function thumb(url: string, w: number): string {
  if (!url || !Number.isFinite(w) || w <= 0 || !OSS_HOST_RE.test(url)) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}x-oss-process=image/resize,w_${Math.round(w)}`
}
