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
