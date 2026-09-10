import request from './request'
export const addExpense = (data) => request.post('/expense/add', data)
export const updateExpense = (data) => request.put('/expense/update', data)
export const getExpenseList = (params) => request.get('/expense/list', { params })
export const markPaid = (id) => request.put(`/expense/mark-paid/${id}`)
export const deleteExpense = (id) => request.delete(`/expense/${id}`)
