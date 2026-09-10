import request from './request'
export const addRepair = (data) => request.post('/repair/add', data)
export const getRepairList = (params) => request.get('/repair/list', { params })
export const updateRepair = (data) => request.put('/repair/update', data)
export const deleteRepair = (id) => request.delete(`/repair/${id}`)
