import request from '@/utils/request'

// 获取订单统计
export const getOrderStatsApi = () => {
  return request.get('/tenant/order/stats')
}

// 获取订单列表
export const getOrderListApi = (params: {
  orderType?: number
  paymentStatus?: number
  page?: number
  size?: number
}) => {
  return request.get('/tenant/order/list', { params })
}

// 获取订单详情
export const getOrderDetailApi = (orderId: number) => {
  return request.get(`/tenant/order/${orderId}`)
}

// 支付订单
export const payOrderApi = (orderId: number, data: { paymentMethod: number; useBeans?: number }) => {
  return request.post(`/tenant/order/pay/${orderId}`, data)
}

// 申请押金退还
export const applyDepositRefundApi = (contractId: number) => {
  return request.post(`/tenant/order/deposit-refund/${contractId}`)
}

// 获取待支付订单
export const getPendingOrdersApi = () => {
  return request.get('/tenant/order/pending')
}

// 为已生效合同初始化订单（补充历史数据）
export const initOrdersApi = () => {
  return request.post('/tenant/order/init-orders')
}

// 获取支付豆信息
export const getBeansInfoApi = () => {
  return request.get('/tenant/order/beans')
}

// 计算订单可使用的支付豆
export const calcBeansForOrderApi = (orderId: number) => {
  return request.get(`/tenant/order/calc-beans/${orderId}`)
}
