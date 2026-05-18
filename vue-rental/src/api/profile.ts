import request from '@/utils/request'

// 实名认证相关
export interface RealnameAuthDTO {
  realName: string
  idCard: string
  idCardFront: string
  idCardBack: string
}

export interface RealnameInfo {
  realnameStatus: number // 0-待审核 1-已通过 2-已驳回
  realnameTime?: string
  auditReason?: string
  authInfo?: {
    authId: number
    realName: string
    idCard: string
    idCardFront: string
    idCardBack: string
    authStatus: number
    auditReason?: string
    createTime: string
  }
}

// 获取实名认证信息
export const getRealnameInfoApi = () => {
  return request.get<RealnameInfo>('/tenant/profile/realname')
}

// 提交实名认证
export const submitRealnameAuthApi = (data: RealnameAuthDTO) => {
  return request.post('/tenant/profile/realname/submit', data)
}

// 个人信息相关
export interface PersonalInfo {
  userId: number
  username: string
  phone: string
  email?: string
  avatar?: string
  realName?: string
  idCard?: string
  realnameStatus: number
  createTime: string
}

export interface UpdateInfoDTO {
  avatar?: string
  username?: string
  realName?: string
  phone?: string
  email?: string
}

export interface UpdatePhoneDTO {
  phone: string
  verifyCode?: string
}

export interface UpdateEmailDTO {
  email: string
  verifyCode?: string
}

export interface UpdatePasswordDTO {
  oldPassword: string
  newPassword: string
}

// 获取个人信息
export const getPersonalInfoApi = () => {
  return request.get<PersonalInfo>('/tenant/profile/info')
}

// 更新个人信息
export const updatePersonalInfoApi = (data: UpdateInfoDTO) => {
  return request.put('/tenant/profile/info', data)
}

// 修改手机号
export const updatePhoneApi = (data: UpdatePhoneDTO) => {
  return request.put('/tenant/profile/phone', data)
}

// 修改邮箱
export const updateEmailApi = (data: UpdateEmailDTO) => {
  return request.put('/tenant/profile/email', data)
}

// 修改密码
export const updatePasswordApi = (data: UpdatePasswordDTO) => {
  return request.put('/tenant/profile/password', data)
}

// 修改身份证
export interface UpdateIdCardDTO {
  idCard: string
}

export const updateIdCardApi = (data: UpdateIdCardDTO) => {
  return request.put('/tenant/profile/idcard', data)
}

// 个人名片相关
export interface UserCard {
  userId: number
  username: string
  avatar?: string
  realName?: string
  phone?: string
  email?: string
  userType: number // 1-租客 2-房东
  realnameStatus: number
  createTime: string
}

// 获取自己的名片
export const getMyCardApi = () => {
  return request.get<UserCard>('/tenant/profile/card')
}

// 获取他人名片（通过userId）
export const getUserCardApi = (userId: number) => {
  return request.get<UserCard>(`/tenant/profile/card/${userId}`)
}
