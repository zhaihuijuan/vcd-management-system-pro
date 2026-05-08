/** 与后端 UserRole 一致 */
export const ROLE = {
  USER: 'USER',
  ADMIN: 'ADMIN'
}

/** 临时写死：该用户名一律按管理员界面处理（须与登录页密码约定一致） */
export const HARDCODED_ADMIN_USERNAME = 'admin'

export function getStoredRole() {
  try {
    const token = localStorage.getItem('jwt_token')
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]))
      // 临时写死：JWT 用户名为 admin 时一律走管理员界面（不依赖 localStorage 旧值）
      if (payload.sub === HARDCODED_ADMIN_USERNAME) {
        return ROLE.ADMIN
      }
      if (payload.role === ROLE.ADMIN || payload.role === ROLE.USER) {
        return payload.role
      }
    }
  } catch {
    /* ignore */
  }
  const stored = localStorage.getItem('user_role')
  if (stored === ROLE.USER || stored === ROLE.ADMIN) {
    return stored
  }
  return ROLE.USER
}

export function isAdmin() {
  return getStoredRole() === ROLE.ADMIN
}

/** 普通用户仅可访问的路由（与侧栏一致） */
export const USER_ALLOWED_PATHS = ['/home', '/catalog', '/purchase', '/rental', '/return']

/** 管理员禁止访问（租赁中心、采购入库） */
export const ADMIN_FORBIDDEN_PATHS = ['/purchase', '/rental', '/return']

export function canAccessPath(path) {
  const normalized = path === '/' ? '/home' : path
  const role = getStoredRole()
  if (role === ROLE.USER) {
    return USER_ALLOWED_PATHS.includes(normalized)
  }
  if (role === ROLE.ADMIN) {
    return !ADMIN_FORBIDDEN_PATHS.includes(normalized)
  }
  return false
}
