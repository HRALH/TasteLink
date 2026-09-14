/**
 * 上传前压缩（F4-3）：手机原图可达 5–10MB,直传慢且易超 5MB 上限。
 * 用 canvas 等比缩小:长边 > MAX_EDGE 才压;输出 jpeg(quality 0.85)。
 * webp/png 透明图压成 jpeg 会丢透明通道,故 png/webp 不压缩尺寸仅重编码(quality 0.85)。
 * 压缩失败(或 Image/canvas 不可用)时回退原图,不阻塞上传。
 */

/** 触发缩放的长边阈值(px);不超过此值不动尺寸。 */
export const MAX_EDGE = 2000
/** JPEG 重编码质量。 */
export const QUALITY = 0.85

/**
 * 读图 → 画到 canvas → toDataURL。返回压缩后的 Blob;原图无需压缩或压缩失败时返回 null(调用方用原文件)。
 */
export function compressImage(
  file: File,
  maxEdge = MAX_EDGE,
  quality = QUALITY,
): Promise<Blob | null> {
  // 仅处理位图;gif 动图不压(SVG 等不在上传白名单内,忽略)
  if (!file.type.startsWith('image/') || file.type === 'image/gif') {
    return Promise.resolve(null)
  }
  // 太小不值得压,直接原图
  if (file.size < 300 * 1024) {
    return Promise.resolve(null)
  }

  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onerror = () => resolve(null)
    reader.onload = () => {
      const src = reader.result
      if (typeof src !== 'string') return resolve(null)
      const img = new Image()
      img.onerror = () => resolve(null)
      img.onload = () => {
        let { width, height } = img
        const longEdge = Math.max(width, height)
        if (longEdge > maxEdge) {
          const scale = maxEdge / longEdge
          width = Math.round(width * scale)
          height = Math.round(height * scale)
        }
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        if (!ctx) return resolve(null)
        ctx.drawImage(img, 0, 0, width, height)
        // png/webp 保持原格式(jpeg 无透明);其余一律 jpeg 省体积
        const outType = file.type === 'image/png' || file.type === 'image/webp' ? file.type : 'image/jpeg'
        canvas.toBlob(
          (blob) => resolve(blob && blob.size < file.size ? blob : null),
          outType,
          quality,
        )
      }
      img.src = src
    }
    reader.readAsDataURL(file)
  })
}
