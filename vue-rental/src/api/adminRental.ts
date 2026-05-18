import request from '@/utils/request'

// ==================== 合同监管 ====================

// 获取合同列表
export function getContractList(params: {
  status?: number
  keyword?: string
  page?: number
  size?: number
}) {
  return request.get('/admin/rental/contract/list', { params })
}

// 获取合同详情
export function getContractDetail(id: number) {
  return request.get(`/admin/rental/contract/${id}`)
}

// 获取合同统计
export function getContractStats() {
  return request.get('/admin/rental/contract/stats')
}

// 终止合同
export function terminateContract(id: number, reason: string) {
  return request.put(`/admin/rental/contract/${id}/terminate`, { reason })
}


