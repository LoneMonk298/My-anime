import service from '@/utils/request'

export function login(data) {
  return service.post('/user/login', data, { silent: true })
}

export function register(data) {
  return service.post('/user/register', data)
}

export function logout() {
  return service.post('/user/logout')
}

export function sendResetPasswordCode(email) {
  return service.post('/user/password/reset-code', { email })
}

export function resetPasswordByEmail(data) {
  return service.put('/user/password/reset', data)
}

export function createAdmin(data) {
  return service.post('/user/admin', data)
}

export function updateAdminPassword(data) {
  return service.put('/user/password', data)
}

export function dashboardSummary() {
  return service.get('/dashboard/summary')
}

export function categoryTree() {
  return service.get('/article/category/tree', { params: { includeDisabled: true } })
}

export function addCategory(data) {
  return service.post('/article/category', data)
}

export function updateCategory(id, data) {
  return service.put(`/article/category/${id}`, data)
}

export function deleteCategory(id) {
  return service.delete(`/article/category/${id}`)
}

export function articleList(params) {
  return service.get('/article/page', { params })
}

export function uploadFile(file, businessInfo = {}) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('businessType', businessInfo.businessType || 'ARTICLE')
  formData.append('businessId', businessInfo.businessId || crypto.randomUUID())
  formData.append('businessField', businessInfo.businessField || 'cover')

  return service.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
}

export function addArticle(data) {
  return service.post('/article', data)
}

export function getArticleDetail(id) {
  return service.get(`/article/${id}`)
}

export function updateArticle(id, data) {
  return service.put(`/article/${id}`, data)
}

export function deleteArticle(id) {
  return service.delete(`/article/${id}`)
}

export function changeArticleStatus(id, data) {
  return service.put(`/article/${id}/status`, data)
}

export function friendLinkList(params) {
  return service.get('/link/page', { params })
}

export function addFriendLink(data) {
  return service.post('/link', data)
}

export function updateFriendLink(id, data) {
  return service.put(`/link/${id}`, data)
}

export function changeFriendLinkStatus(id, data) {
  return service.put(`/link/${id}/status`, data)
}

export function deleteFriendLink(id) {
  return service.delete(`/link/${id}`)
}
