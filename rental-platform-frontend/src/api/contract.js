import request from './request'
export const uploadContract = (formData, onUploadProgress) => request.post('/contract/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' },
  onUploadProgress
})
export const reviewContract = (id) => request.post(`/contract/review/${id}`)
export const getContractList = (params) => request.get('/contract/list', { params })
export const getContractDetail = (id) => request.get(`/contract/detail/${id}`)
export const getReviewRecords = (id) => request.get(`/contract/records/${id}`)
export const deleteContract = (id) => request.delete(`/contract/${id}`)

// 审查结果反馈（有用/误报）
export const submitFeedback = (recordId, feedback) => request.post(`/contract/feedback/${recordId}`, { feedback })
export const getFeedbacks = (contractId) => request.get('/contract/feedback', { params: { contractId } })
