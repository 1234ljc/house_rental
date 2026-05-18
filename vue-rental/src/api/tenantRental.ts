import request from '@/utils/request'

// ==================== 合同相关 ====================

// 获取合同列表
export const getContractListApi = (params: { status?: number; page?: number; size?: number }) => {
  return request.get('/tenant/rental/contract/list', { params })
}

// 获取合同详情
export const getContractDetailApi = (contractId: number) => {
  return request.get(`/tenant/rental/contract/${contractId}`)
}

// 获取合同统计
export const getContractStatsApi = () => {
  return request.get('/tenant/rental/contract/stats')
}

// 确认合同
export const signContractApi = (contractId: number) => {
  return request.put(`/tenant/rental/contract/sign/${contractId}`)
}



// 下载合同文件
export const downloadContractFileApi = (contractId: number) => {
  const token = localStorage.getItem('token_tenant')
  window.open(`http://localhost:8080/api/tenant/rental/contract/download/${contractId}?token=${token}`, '_blank')
}



// ==================== 身份验证与签名 ====================

// 验证身份信息
export const verifyIdentityApi = (data: { idCardLast6: string; phoneLast4: string }) => {
  return request.post('/tenant/rental/contract/verify-identity', data)
}

// 确认合同（带签名）
export const signContractWithSignatureApi = (contractId: number, data: { signature: string }) => {
  return request.put(`/tenant/rental/contract/sign/${contractId}`, data)
}

// 申请续租
export const applyRenewalApi = (contractId: number) => {
  return request.post(`/tenant/rental/contract/renewal/${contractId}`)
}


