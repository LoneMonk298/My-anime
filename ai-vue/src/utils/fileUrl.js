import defaultArticleImage from '@/assets/images/default.png'
import { filebaseURL } from '@/config/index'

const pickFirstString = (...values) => {
  return values.find((value) => typeof value === 'string' && value.trim())?.trim() || ''
}

export const getArticleCover = (article = {}) => {
  return pickFirstString(
    article.coverImg,
    article.coverImage,
    article.cover,
    article.coverUrl,
    article.thumbnail,
    article.imageUrl,
  )
}

export const resolveFileUrl = (filePath, fallback = '') => {
  const path = pickFirstString(filePath)
  if (!path) return fallback
  if (/^(https?:)?\/\//i.test(path) || path.startsWith('blob:') || path.startsWith('data:')) {
    return path
  }
  if (path.startsWith('/anime-assets/')) return path
  if (path.startsWith(filebaseURL + '/')) return path
  if (path.startsWith('/')) return `${filebaseURL}${path}`
  return `${filebaseURL}/${path}`
}

export const resolveArticleCoverUrl = (article = {}) => {
  return resolveFileUrl(getArticleCover(article), defaultArticleImage)
}
