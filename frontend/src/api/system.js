import request from '@/utils/request'

// ---------- 用户 ----------
export const pageUser = params => request.get('/system/users', { params })
export const simpleUsers = params => request.get('/system/users/simple', { params })
export const getUser = id => request.get(`/system/users/${id}`)
export const addUser = data => request.post('/system/users', data)
export const updateUser = (id, data) => request.put(`/system/users/${id}`, data)
export const deleteUser = id => request.delete(`/system/users/${id}`)
export const resetUserPassword = id => request.put(`/system/users/${id}/resetPassword`)
export const changeUserStatus = (id, status) =>
  request.put(`/system/users/${id}/status`, null, { params: { status } })

// ---------- 部门 ----------
export const deptTree = () => request.get('/system/depts/tree')
export const addDept = data => request.post('/system/depts', data)
export const updateDept = (id, data) => request.put(`/system/depts/${id}`, data)
export const deleteDept = id => request.delete(`/system/depts/${id}`)

// ---------- 角色 ----------
export const pageRole = params => request.get('/system/roles', { params })
export const allRoles = () => request.get('/system/roles/all')
export const roleMenuIds = id => request.get(`/system/roles/${id}/menuIds`)
export const addRole = data => request.post('/system/roles', data)
export const updateRole = (id, data) => request.put(`/system/roles/${id}`, data)
export const deleteRole = id => request.delete(`/system/roles/${id}`)
export const assignRoleMenus = (id, menuIds) =>
  request.put(`/system/roles/${id}/menus`, { menuIds })

// ---------- 菜单 ----------
export const menuTree = () => request.get('/system/menus/tree')
export const addMenu = data => request.post('/system/menus', data)
export const updateMenu = (id, data) => request.put(`/system/menus/${id}`, data)
export const deleteMenu = id => request.delete(`/system/menus/${id}`)

// ---------- 参数 ----------
export const pageConfig = params => request.get('/system/configs', { params })
export const addConfig = data => request.post('/system/configs', data)
export const updateConfig = (id, data) => request.put(`/system/configs/${id}`, data)
export const deleteConfig = id => request.delete(`/system/configs/${id}`)

// ---------- 日志 ----------
export const pageLog = params => request.get('/system/logs', { params })
