import request from '@/utils/request'
import type { UserInfo } from '@/types/user'

// 用户列表查询参数
export interface UserQueryParams {
  userType: number
  status?: number
  startDate?: string
  endDate?: string
  keyword?: string
  page?: number
  size?: number
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 添加管理员参数
export interface AddAdminParams {
  username: string
  password: string
  realName?: string
  phone?: string
  email?: string
}

// 获取用户列表
export const getUserListApi = (params: UserQueryParams) => {
  return request.get<PageResult<UserInfo>>('/admin/user/list', { params })
}

// 获取用户详情
export const getUserDetailApi = (userId: number) => {
  return request.get<UserInfo>(`/admin/user/${userId}`)
}

// 禁用/启用用户
export const updateUserStatusApi = (userId: number, status: number) => {
  return request.put(`/admin/user/${userId}/status`, { status })
}

// 重置密码
export const resetPasswordApi = (userId: number) => {
  return request.put(`/admin/user/${userId}/reset-password`)
}

// 添加管理员
export const addAdminApi = (data: AddAdminParams) => {
  return request.post('/admin/user/admin', data)
}
