import request from '@/utils/request'

// ==================== 合同管理 ====================

// 获取合同统计
export const getContractStatsApi = () => {
  return request.get('/landlord/rental/contract/stats')
}

// 获取合同列表
export const getContractListApi = (params: {
  status?: number
  houseId?: number
  keyword?: string
  page?: number
  size?: number
}) => {
  return request.get('/landlord/rental/contract/list', { params })
}

// 获取合同详情
export const getContractDetailApi = (contractId: number) => {
  return request.get(`/landlord/rental/contract/${contractId}`)
}

// 创建合同（上传文件，基于申请）
export const createContractApi = (formData: FormData) => {
  return request.post('/landlord/rental/contract/create', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 直接创建合同（无需申请，房东主动发起）
export const createContractDirectApi = (formData: FormData) => {
  return request.post('/landlord/rental/contract/create-direct', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 搜索租客
export const searchTenantApi = (keyword: string) => {
  return request.get('/landlord/rental/tenant/search', { params: { keyword } })
}

// 获取与房东有聊天会话的租客列表
export const getChatTenantsApi = () => {
  return request.get('/landlord/rental/chat-tenants')
}

// 更新合同
export const updateContractApi = (contractId: number, data: {
  paymentDay?: number
  monthlyRent?: number
  depositAmount?: number
}) => {
  return request.put(`/landlord/rental/contract/update/${contractId}`, data)
}

// 下载合同文件
export const downloadContractApi = (contractId: number) => {
  const token = localStorage.getItem('token_landlord')
  window.open(`http://localhost:8080/api/landlord/rental/contract/download/${contractId}?token=${token}`, '_blank')
}

// 发送合同
export const sendContractApi = (contractId: number) => {
  return request.put(`/landlord/rental/contract/send/${contractId}`)
}

// 签署合同（房东端已不再使用，保留兼容）
export const signContractApi = (contractId: number, data?: { signature?: string }) => {
  return request.put(`/landlord/rental/contract/sign/${contractId}`, data || {})
}

// 身份验证
export const verifyIdentityApi = (data: { idCardLast6: string; phoneLast4: string }) => {
  return request.post('/landlord/rental/contract/verify-identity', data)
}

// 重新上传合同
export const reuploadContractApi = (contractId: number, formData: FormData) => {
  return request.post(`/landlord/rental/contract/reupload/${contractId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 同意续租（上传新合同）
export const approveRenewalApi = (contractId: number, formData: FormData) => {
  return request.post(`/landlord/rental/contract/renewal/approve/${contractId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 拒绝续租
export const rejectRenewalApi = (contractId: number, data?: { reason?: string }) => {
  return request.put(`/landlord/rental/contract/renewal/reject/${contractId}`, data || {})
}


