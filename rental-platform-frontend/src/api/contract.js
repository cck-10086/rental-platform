import request from './request'
export const uploadContract = (formData) => request.post('/contract/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const reviewContract = (id) => request.post(`/contract/review/${id}`)
export const getContractList = (params) => request.get('/contract/list', { params })
export const getContractDetail = (id) => request.get(`/contract/detail/${id}`)
export const getReviewRecords = (id) => request.get(`/contract/records/${id}`)
export const deleteContract = (id) => request.delete(`/contract/${id}`)
