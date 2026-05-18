import request from '@/utils/request'
import type { LoginForm, RegisterForm, AuthResponse, ApiResponse, UserInfo } from '@/types/user'

// 登录
export const loginApi = (data: LoginForm) => {
  return request.post<ApiResponse<AuthResponse>>('/auth/login', data)
}

// 注册
export const registerApi = (data: RegisterForm) => {
  return request.post<ApiResponse<AuthResponse>>('/auth/register', data)
}

// 获取当前用户信息
export const getCurrentUserApi = () => {
  return request.get<ApiResponse<UserInfo>>('/auth/current')
}

// 预检查用户信息（用于头像显示）
export const preCheckUserApi = (data: LoginForm) => {
  return request.post<ApiResponse<{ avatar?: string }>>('/auth/pre-check', data)
}

// 退出登录
export const logoutApi = () => {
  return request.post<ApiResponse>('/auth/logout')
}