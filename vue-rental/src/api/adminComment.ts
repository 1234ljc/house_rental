import request from '@/utils/request'

// 获取举报列表
export const getReportsApi = (params?: { status?: number; page?: number; size?: number }) => {
  return request.get('/admin/comment/reports', { params })
}

// 获取举报统计
export const getReportStatsApi = () => {
  return request.get('/admin/comment/reports/stats')
}

// 审核通过（删帖）
export const approveReportApi = (reportId: number, remark?: string) => {
  return request.post(`/admin/comment/reports/approve/${reportId}`, { remark })
}

// 驳回举报
export const rejectReportApi = (reportId: number, remark?: string) => {
  return request.post(`/admin/comment/reports/reject/${reportId}`, { remark })
}
