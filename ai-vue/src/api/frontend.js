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

export function applyFriendLink(data) {
  return service.post('/link/apply', data)
}

export function recordFriendLinkVisit(id) {
  return service.post(`/link/${id}/visit`, null, { silent: true })
}
