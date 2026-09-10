import request from './request'
export const createMoveOut = (data) => request.post('/move-out/create', data)
export const getMoveOutList = (params) => request.get('/move-out/list', { params })
export const updateMoveOut = (data) => request.put('/move-out/update', data)
export const deleteMoveOut = (id) => request.delete(`/move-out/${id}`)
