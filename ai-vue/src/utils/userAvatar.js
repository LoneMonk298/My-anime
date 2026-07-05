import defaultAvatar from '@/assets/images/user.png'
import { resolveFileUrl } from '@/utils/fileUrl'

const pickFirstString = (...values) => {
  return values.find((value) => typeof value === 'string' && value.trim())?.trim() || ''
}

export const getUserAvatar = (user = {}) => {
  return pickFirstString(
    user.avatarUrl,
    user.avatar,
    user.userAvatar,
    user.headImg,
    user.headImage,
    user.photo,
  )
}

export const resolveUserAvatarUrl = (user = {}) => {
  return resolveFileUrl(getUserAvatar(user), defaultAvatar)
}

export const normalizeUserInfo = (user = {}) => {
  const avatarUrl = getUserAvatar(user)
  return {
    ...user,
    avatar: avatarUrl,
    avatarUrl,
  }
}

export const getUploadFileUrl = (data = {}) => {
  return pickFirstString(
    data.url,
    data.fileUrl,
    data.filePath,
    data.path,
  )
}
