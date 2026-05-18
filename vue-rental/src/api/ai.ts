import request from '@/utils/request'

// 智能解析房源描述（AI智能发布使用）
export const parseHouseDescriptionApi = (data: {
  description: string
}) => {
  return request.post('/ai/parse-house-description', data, {
    timeout: 30000
  })
}

// AI房源对比分析（房源对比弹窗使用）
export const compareHousesApi = (data: {
  houses: any[]
}) => {
  return request.post('/ai/compare-houses', data, {
    timeout: 30000
  })
}
