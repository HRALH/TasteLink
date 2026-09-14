import instance from './request'
import type { ImageUploadResult } from '../types/api'

/**
 * 文件上传模块（docs/05 §4.8）
 * 图片统一走 POST /files/image 拿可访问 URL 后再随表单/点评提交（docs/03 §5.3）。
 * 参数可为压缩后的 Blob(F4-3);Blob 缺文件名,append 时补一个,后端按扩展名取延展名。
 */
export function uploadImage(file: File | Blob): Promise<ImageUploadResult> {
  const form = new FormData()
  form.append('file', file, (file as File).name ?? 'upload.jpg')
  return instance.post('/files/image', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }) as unknown as Promise<ImageUploadResult>
}
