import request from '@/utils/request'

// ==================== 租金缴纳 ====================
export const getRentListApi = (params: { contractId?: number; page?: number; size?: number }) => {
  return request.get('/tenant/after/rent/list', { params })
}

export const getActiveContractsApi = () => {
  return request.get('/tenant/after/contracts')
}

// ==================== 问题反馈 ====================
export const submitIssueApi = (data: {
  contractId: number
  content: string
  images?: string
  manageType?: number
}) => {
  return request.post('/tenant/after/issue/submit', data)
}

export const getIssueListApi = (params: { status?: number; page?: number; size?: number }) => {
  return request.get('/tenant/after/issue/list', { params })
}

export const appendIssueApi = (manageId: number, data: { content: string }) => {
  return request.put(`/tenant/after/issue/append/${manageId}`, data)
}

// ==================== 退租管理 ====================

// 申请退租
export const applyCheckoutApi = (data: {
  contractId: number
  expectDate: string
  reason: string
  checkoutType?: number
}) => {
  return request.post('/tenant/after/checkout/apply', data)
}

// 获取退租申请列表
export const getCheckoutListApi = () => {
  return request.get('/tenant/after/checkout/list')
}

// 获取退租申请详情
export const getCheckoutDetailApi = (manageId: number) => {
  return request.get(`/tenant/after/checkout/${manageId}`)
}

// 取消退租申请
export const cancelCheckoutApi = (manageId: number) => {
  return request.put(`/tenant/after/checkout/cancel/${manageId}`)
}

// 确认交接完成
export const confirmCheckoutApi = (manageId: number) => {
  return request.put(`/tenant/after/checkout/confirm/${manageId}`)
}
