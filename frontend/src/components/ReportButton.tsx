import { useState } from 'react'
import { Form, Modal, Select, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { reportApi } from '../api/report'
import { useAuthStore } from '../store/authStore'
import { palette } from '../styles/tokens'

/** 举报理由（KEY 即落库 reason 文本，≤255） */
const REPORT_REASONS = [
  { value: '垃圾广告或营销信息', label: '垃圾广告或营销信息' },
  { value: '引战/人身攻击', label: '引战 / 人身攻击' },
  { value: '违法违规内容', label: '违法违规内容' },
  { value: '不实或误导信息', label: '不实或误导信息' },
  { value: '其它问题', label: '其它问题' },
]

/**
 * 举报入口（产品优化 F4）：登录用户对点评等目标提交举报。
 * 重复举报后端返回 40905，拦截器统一 toast「已举报过该内容」。
 * 未登录点击跳登录（带 redirect）。
 */
export default function ReportButton({
  targetType,
  targetId,
}: {
  targetType: 'REVIEW' | 'COMMENT' | 'USER' | 'SHOP'
  targetId: number
}) {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn)
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState<string | undefined>(undefined)
  const [form] = Form.useForm()

  const mut = useMutation({
    mutationFn: (r: string) => reportApi.report({ targetType, targetId, reason: r }),
    onSuccess: () => {
      message.success('举报已提交，我们会尽快处理')
      setOpen(false)
      form.resetFields()
      setReason(undefined)
    },
    // 错误提示由 request 拦截器统一处理（含 40905「已举报过该内容」、401 跳登录）
  })

  const openModal = () => {
    if (!isLoggedIn) {
      const redirect = encodeURIComponent(location.pathname + location.search)
      navigate(`/login?redirect=${redirect}`)
      return
    }
    setOpen(true)
  }

  const submit = () => {
    if (!reason) {
      message.warning('请选择举报理由')
      return
    }
    mut.mutate(reason)
  }

  return (
    <>
      <span
        role="button"
        tabIndex={0}
        onClick={openModal}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault()
            openModal()
          }
        }}
        style={{ cursor: 'pointer', fontSize: 13, color: palette.muted }}
      >
        举报
      </span>
      <Modal
        title="举报内容"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={submit}
        okText="提交举报"
        cancelText="取消"
        confirmLoading={mut.isPending}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 8 }}>
          <Form.Item label="举报理由" required>
            <Select
              value={reason}
              options={REPORT_REASONS}
              placeholder="请选择举报理由"
              onChange={(v) => setReason(v)}
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
