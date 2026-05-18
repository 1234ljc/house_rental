import request from '@/utils/request'

// ==================== 租客管理 ====================
export const getCurrentTenantsApi = () => {
  return request.get('/landlord/after/tenant/current')
}

export const getHistoryTenantsApi = (params: { page?: number; size?: number }) => {
  return request.get('/landlord/after/tenant/history', { params })
}

export const getTenantDetailApi = (contractId: number) => {
  return request.get(`/landlord/after/tenant/${contractId}`)
}

// ==================== 问题处理 ====================
export const getIssueListApi = (params: {
  status?: number
  houseId?: number
  page?: number
  size?: number
}) => {
  return request.get('/landlord/after/issue/list', { params })
}

export const getIssueStatsApi = () => {
  return request.get('/landlord/after/issue/stats')
}

export const processIssueApi = (manageId: number, data: { status: number; response?: string }) => {
  return request.put(`/landlord/after/issue/process/${manageId}`, data)
}

// ==================== 退租管理 ====================

// 获取退租申请列表
export const getCheckoutListApi = (params?: { status?: number; page?: number; size?: number }) => {
  return request.get('/landlord/after/checkout/list', { params })
}

// 获取退租申请统计
export const getCheckoutStatsApi = () => {
  return request.get('/landlord/after/checkout/stats')
}

// 审核退租申请
export const auditCheckoutApi = (manageId: number, data: { action: number; response?: string }) => {
  return request.put(`/landlord/after/checkout/audit/${manageId}`, data)
}

// 安排房屋交接
export const arrangeHandoverApi = (manageId: number, data: { handoverTime?: string; handoverNote?: string }) => {
  return request.put(`/landlord/after/checkout/handover/${manageId}`, data)
}

// 完成退租
export const completeCheckoutApi = (manageId: number, data: { damageDesc?: string; deductReason?: string }) => {
  return request.put(`/landlord/after/checkout/complete/${manageId}`, data)
}
