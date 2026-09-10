import request from './request'

// 用户管理
export const getAdminUsers = (params) => request.get('/admin/users', { params })
export const updateUserStatus = (id, status) => request.put(`/admin/users/${id}/status`, { status })
export const updateUserRole = (id, role) => request.put(`/admin/users/${id}/role`, { role })
export const resetUserPassword = (id, password) => request.put(`/admin/users/${id}/password`, { password })

// 全局数据查看
export const getAdminContracts = (params) => request.get('/admin/contracts', { params })
export const getAdminHouses = (params) => request.get('/admin/houses', { params })
export const getAdminExpenses = (params) => request.get('/admin/expenses', { params })
export const getAdminRepairs = (params) => request.get('/admin/repairs', { params })
export const getAdminMoveOuts = (params) => request.get('/admin/move-outs', { params })

// 数据统计
export const getAdminStats = () => request.get('/admin/stats')
