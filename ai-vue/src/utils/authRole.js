export const ADMIN_HOME = '/user/dashboard'
export const USER_HOME = '/'

const ADMIN_ROLES = new Set(['ADMIN', 'SUPER_ADMIN'])

export const getNumericRoleType = (userInfo = {}) => {
  const value = userInfo.roleType ?? userInfo.userType
  const roleType = Number(value)
  return Number.isFinite(roleType) ? roleType : null
}

export const getRoleName = (userInfo = {}) => {
  return String(userInfo.role || userInfo.roleName || '').trim().toUpperCase()
}

export const hasKnownRole = (userInfo = {}) => {
  return getNumericRoleType(userInfo) !== null || Boolean(getRoleName(userInfo))
}

export const isAdminRole = (userInfo = {}) => {
  return getNumericRoleType(userInfo) === 2 || ADMIN_ROLES.has(getRoleName(userInfo))
}

export const resolveHomePath = (userInfo = {}) => {
  return isAdminRole(userInfo) ? ADMIN_HOME : USER_HOME
}

export const isAdminPath = (path = '') => {
  return path === '/user' || path.startsWith('/user/')
}

export const canUseLoginRedirect = (redirect, userInfo = {}) => {
  if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return false
  }

  if (isAdminPath(redirect) && !isAdminRole(userInfo)) {
    return false
  }

  return true
}
