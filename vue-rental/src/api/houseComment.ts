import request from '@/utils/request'

export const getHouseCommentsApi = (houseId: number, params?: { page?: number; size?: number }) =>
  request.get(`/house-comment/${houseId}`, { params })

export const postCommentApi = (houseId: number, data: { content: string; parentId?: number; replyToUserId?: number }) =>
  request.post(`/house-comment/${houseId}`, data)

export const deleteCommentApi = (commentId: number) =>
  request.delete(`/house-comment/${commentId}`)

export const reportCommentApi = (commentId: number, reason: string) =>
  request.post(`/house-comment/report/${commentId}`, { reason })

export const getRepliesApi = (commentId: number) =>
  request.get(`/house-comment/replies/${commentId}`)
