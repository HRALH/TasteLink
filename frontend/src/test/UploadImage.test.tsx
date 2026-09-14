import { describe, expect, it, beforeEach, vi } from 'vitest'
import { render, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'

vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>()
  return {
    ...actual,
    message: {
      error: vi.fn(),
      success: vi.fn(),
      info: vi.fn(),
      warning: vi.fn(),
      loading: vi.fn(),
    },
  }
})

import { message } from 'antd'
import UploadImage from '../components/UploadImage'
import { server } from './server'

function mkFile(name: string, type: string, sizeMB: number): File {
  const size = Math.floor(sizeMB * 1024 * 1024)
  // jsdom Blob 支持指定 size，内容不真实但 antd 仅读 .size/.type/.name
  return new File([new Uint8Array(size)], name, { type }) as unknown as File
}

/** 找 Upload 组件内部的 file input（antd 不渲染关联 label） */
function fileInput(container: HTMLElement): HTMLInputElement {
  return container.querySelector('input[type=file]') as HTMLInputElement
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('UploadImage 大小校验（F6）', () => {
  // 注：类型校验（ACCEPTED.includes(file.type)）被 antd 的 accept 属性在
  // rc-upload attr-accept 层先过滤掉，jsdom user.upload 无法触发到 beforeUpload
  // 的类型分支——该分支是 accept 属性的 defense-in-depth 备份，UI 层不可达。
  // 故只测 5MB 上限（accept 不拦尺寸）与 customRequest 失败清理。
  it('≥ 5MB → message.error「图片大小不能超过 5MB」+ LIST_IGNORE', async () => {
    const onChange = vi.fn()
    const user = userEvent.setup()
    render(<UploadImage onChange={onChange} />)

    const input = fileInput(document.body)
    const big = mkFile('big.jpg', 'image/jpeg', 5.1)
    await user.upload(input, big)

    expect(message.error).toHaveBeenCalledWith('图片大小不能超过 5MB')
    expect(onChange).not.toHaveBeenCalled()
  })
})

describe('UploadImage customRequest 失败清理（F6）', () => {
  it('上传 API 500 → fileList 清理 + message.error「上传失败」', async () => {
    server.use(
      http.post('/api/v1/files/image', () =>
        HttpResponse.json({ code: 500, message: '服务器异常' }),
      ),
    )
    const onChange = vi.fn()
    const user = userEvent.setup()
    render(<UploadImage onChange={onChange} />)

    const input = fileInput(document.body)
    // 用 <300KB 的小图避开 compressImage 的 canvas 路径（jsdom canvas 不可用，会回退原图直传）
    const small = mkFile('ok.jpg', 'image/jpeg', 0.1)
    await user.upload(input, small)

    await waitFor(() => {
      expect(message.error).toHaveBeenCalledWith('上传失败')
    })
    // 失败后 fileList 清空，不回传 url
    expect(onChange).not.toHaveBeenCalled()
  })
})
