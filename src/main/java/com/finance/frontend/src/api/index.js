import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
  response => response.data,
  error => {
    const message = error.response?.data?.msg || error.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default {
  // 认证
  login: (data) => api.post('/auth/login', data),

  // 报销单
  getClaims: () => api.get('/claims/list'),
  getPendingClaims: () => api.get('/claims/pending'),
  getClaim: (id) => api.get(`/claims/${id}`),
  createClaim: (data) => api.post('/claims/submit', data),
  deleteClaim: (id) => api.delete(`/claims/${id}`),

  // 新审批接口（推荐）
  approveClaim: (claimId, approved, comment) => api.post('/claims/approve', { claimId, approved, comment }),

  // 旧审批接口（兼容）
  auditClaim: (id, data) => api.post('/claims/audit-v2', { id, ...data }),

  getHighRiskClaims: () => api.get('/claims/high-risk'),

  // 发票
  uploadInvoice: (formData) => api.post('/invoices/ocr', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  getInvoice: (id) => api.get(`/invoices/${id}`),
  recognizeInvoice: (id) => api.post(`/invoices/${id}/recognize`),

  // 风险
  detectRisk: (claimId) => api.post('/risk/detect', { claimId }),
  getRiskAlerts: () => api.get('/risk/alerts'),
  getRiskDetail: (claimId) => api.get(`/risk/detail/${claimId}`),
  reDetectRisk: (claimId) => api.post(`/risk/re-detect/${claimId}`),
  handleAlert: (id, data) => api.put(`/risk/alerts/${id}/handle`, data),

  // 审计
  auditCompliance: (claimId) => api.get(`/audit/compliance/${claimId}`),
  auditProcess: (claimId) => api.get(`/audit/process/${claimId}`),
  getExpenseTrend: (params) => api.get('/audit/stats/trend', { params }),
  getAnomalyStats: (params) => api.get('/audit/stats/anomaly', { params }),
  getDepartmentAnalysis: (deptId, year) => api.get('/audit/stats/department', { params: { deptId, year } }),

  // 规则
  getRules: () => api.get('/rules'),
  getCategories: () => api.get('/rules/categories'),
  validateClaim: (data) => api.post('/rules/validate', data),
  reloadRules: () => api.put('/rules/reload'),

  // 部门
  getDepartments: () => api.get('/departments'),
  getDepartment: (id) => api.get(`/departments/${id}`),
  getDepartmentBudget: (id, year) => api.get(`/departments/${id}/budget`, { params: { year } }),
  updateBudget: (id, data) => api.put(`/departments/${id}/budget`, data),

  // 用户管理（管理员）
  getUsers: () => api.get('/users'),
  createUser: (data) => api.post('/users', data),
  updateUser: (id, data) => api.put(`/users/${id}`, data),
  deleteUser: (id) => api.delete(`/users/${id}`),

  // 审计日志（管理员）
  getAuditLogs: (params) => api.get('/audit/logs', { params })
}