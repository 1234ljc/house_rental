import request from '@/utils/request'
import type { Permission, DashboardStats, MonitorData, TodoItem, TrendData, HotCity } from '@/types/admin'

// 获取用户菜单权限
export const getMenusApi = () => {
  return request.get<Permission[]>('/admin/permission/menus')
}

// 获取统计卡片数据
export const getDashboardStatsApi = () => {
  return request.get<DashboardStats>('/admin/dashboard/stats')
}

// 获取实时监控数据
export const getMonitorDataApi = () => {
  return request.get<MonitorData>('/admin/dashboard/monitor')
}

// 获取趋势图数据
export const getTrendsApi = () => {
  return request.get<TrendData>('/admin/dashboard/trends')
}

// 获取热门城市排行
export const getHotCitiesApi = () => {
  return request.get<HotCity[]>('/admin/dashboard/hot-cities')
}

// 获取待办事项
export const getTodosApi = () => {
  return request.get<TodoItem[]>('/admin/dashboard/todos')
}
