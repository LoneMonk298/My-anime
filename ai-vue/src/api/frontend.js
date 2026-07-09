import service from '@/utils/request'

export function getArticleList(params) {
  return service.get('/article/page', { params })
}

export function getArticleCategoryTree() {
  return service.get('/article/category/tree')
}

export function getArticleDetail(articleId) {
  return service.get(`/article/${articleId}`)
}

export function getArticleView(articleId) {
  return service.get(`/article/${articleId}/view`)
}

export function getFriendLinks(params) {
  return service.get('/link/enabled', { params })
}

export function applyFriendLink(data, logoFile) {
  if (logoFile) {
    const formData = new FormData()
    Object.entries(data || {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        formData.append(key, value)
      }
    })
    formData.append('logo', logoFile)
    return service.post('/link/apply', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000,
    })
  }
  return service.post('/link/apply', data)
}

export function recordFriendLinkVisit(id) {
  return service.post(`/link/${id}/visit`, null, { silent: true })
}

export function logout() {
  return service.post('/user/logout')
}

export function getUserProfile() {
  return service.get('/user/profile')
}

export function updateUserProfile(data) {
  return service.put('/user/profile', data)
}

export function updateUserPassword(data) {
  return service.put('/user/password', data)
}

export function updateUserEmail(data) {
  return service.put('/user/email', data)
}

export function uploadUserAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('businessType', 'USER_AVATAR')
  formData.append('businessId', crypto.randomUUID())
  formData.append('businessField', 'avatar')

  return service.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
}
