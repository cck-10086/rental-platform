import request from './request'

export const addHouseViewing = (data) => request.post('/house-viewing/add', data)
export const updateHouseViewing = (data) => request.put('/house-viewing/update', data)
export const getHouseViewingList = () => request.get('/house-viewing/list')
export const deleteHouseViewing = (id) => request.delete(`/house-viewing/${id}`)
