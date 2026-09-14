import { useState } from 'react'
import { Upload, message } from 'antd'
import type { UploadFile, UploadProps } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { uploadImage } from '../api/file'
import { MAX_REVIEW_IMAGES } from '../utils/constants'
import { palette } from '../styles/tokens'
import { compressImage } from '../utils/compressImage'

const ACCEPTED = ['image/jpeg', 'image/png', 'image/webp']

/**
 * 图片上传组件：封装 POST /files/image，先拿可访问 URL 再随表单提交（docs/03 §5.3）。
 * 通过 onChange 回传已上传的 url 列表（有序）。
 */
export default function UploadImage({
  onChange,
  maxCount = MAX_REVIEW_IMAGES,
}: {
  onChange: (urls: string[]) => void
  maxCount?: number
}) {
  const [fileList, setFileList] = useState<UploadFile[]>([])

  const emitUrls = (next: UploadFile[]) => {
    onChange(next.filter((f) => f.status === 'done' && f.url).map((f) => f.url as string))
  }

  const props: UploadProps = {
    listType: 'picture-card',
    fileList,
    multiple: true,
    maxCount,
    accept: 'image/jpeg,image/png,image/webp',
    beforeUpload: (file) => {
      if (!ACCEPTED.includes(file.type)) {
        message.error('仅支持 jpg/png/webp 图片')
        return Upload.LIST_IGNORE
      }
      if (file.size / 1024 / 1024 >= 5) {
        message.error('图片大小不能超过 5MB')
        return Upload.LIST_IGNORE
      }
      return true
    },
    customRequest: async (options) => {
      const { onSuccess, onError } = options
      const raw = options.file as File
      const uid = (options.file as UploadFile).uid
      try {
        // 上传前压缩(canvas 缩长边+重编码);失败/无需压缩时回退原图
        const compressed = await compressImage(raw)
        const res = await uploadImage(compressed ?? raw)
        onSuccess?.(res, undefined)
        setFileList((prev) => {
          const next = prev.map((item) =>
            item.uid === uid ? { ...item, status: 'done' as const, url: res.url } : item,
          )
          emitUrls(next)
          return next
        })
      } catch (e) {
        onError?.(e as Error)
        message.error('上传失败')
        setFileList((prev) => prev.filter((item) => item.uid !== uid))
      }
    },
    onRemove: (file) => {
      setFileList((prev) => {
        const next = prev.filter((item) => item.uid !== file.uid)
        emitUrls(next)
        return next
      })
      return false
    },
  }

  return (
    <Upload {...props}>
      {fileList.length >= maxCount ? null : (
        <div>
          <PlusOutlined />
          <div style={{ marginTop: 8, color: palette.muted }}>上传图片</div>
        </div>
      )}
    </Upload>
  )
}
