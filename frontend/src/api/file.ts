import instance from './request'
import type { ImageUploadResult } from '../types/api'

/**
 * 文件上传模块（docs/05 §4.8）
 * 图片统一走 POST /files/image 拿可访问 URL 后再随表单/点评提交（docs/03 §5.3）。
 */
export function uploadImage(file: File): Promise<ImageUploadResult> {
  const form = new FormData()
  form.append('file', file)
  return instance.post('/files/image', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }) as unknown as Promise<ImageUploadResult>
}
