import request from '@/utils/request'

// ==================== 角色管理 ====================

export function getRoleList(params: { keyword?: string; page?: number; size?: number }) {
  return request.get('/admin/permission/role/list', { params })
}

export function getAllRoles() {
  return request.get('/admin/permission/role/all')
}

export function getRoleDetail(id: number) {
  return request.get(`/admin/permission/role/${id}`)
}

export function addRole(data: { roleCode: string; roleName: string; roleDesc?: string; permIds?: number[] }) {
  return request.post('/admin/permission/role', data)
}

export function updateRole(id: number, data: { roleName?: string; roleDesc?: string; permIds?: number[] }) {
  return request.put(`/admin/permission/role/${id}`, data)
}

export function deleteRole(id: number) {
  return request.delete(`/admin/permission/role/${id}`)
}

export function copyRole(id: number, data: { roleCode: string; roleName: string }) {
  return request.post(`/admin/permission/role/${id}/copy`, data)
}

// ==================== 权限菜单 ====================

export function getPermissionTree() {
  return request.get('/admin/permission/menu/tree')
}

// ==================== 管理员管理 ====================

export function getAdminList(params: { keyword?: string; status?: number; page?: number; size?: number }) {
  return request.get('/admin/permission/admin/list', { params })
}

export function addAdmin(data: { username: string; password: string; realName?: string; phone?: string; email?: string; roleIds?: number[] }) {
  return request.post('/admin/permission/admin', data)
}

export function updateAdmin(id: number, data: { realName?: string; phone?: string; email?: string; password?: string; roleIds?: number[] }) {
  return request.put(`/admin/permission/admin/${id}`, data)
}

export function toggleAdminStatus(id: number, status: number) {
  return request.put(`/admin/permission/admin/${id}/status`, { status })
}

export function deleteAdmin(id: number) {
  return request.delete(`/admin/permission/admin/${id}`)
}
