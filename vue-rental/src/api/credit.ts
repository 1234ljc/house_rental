import request from '@/utils/request'

// 获取信用报告
export const getCreditReportApi = () => {
  return request.get('/credit/report')
}

// 获取指定用户的信用分
export const getUserCreditScoreApi = (userId: number) => {
  return request.get(`/credit/score/${userId}`)
}

// 更新信用分
export const updateCreditScoreApi = () => {
  return request.post('/credit/update')
}
