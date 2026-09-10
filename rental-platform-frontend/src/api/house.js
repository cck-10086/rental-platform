import request from './request'
export const addHouse = (data) => request.post('/house/add', data)
export const updateHouse = (data) => request.put('/house/update', data)
export const getHouseList = (params) => request.get('/house/list', { params })
export const getHouseDetail = (id) => request.get(`/house/detail/${id}`)
export const deleteHouse = (id) => request.delete(`/house/${id}`)
