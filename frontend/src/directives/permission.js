/**
 * 权限指令 v-permission
 * 用于根据权限动态显示/隐藏元素
 * 注意：此指令仅用于界面隐藏，后端接口仍需进行权限校验
 */
export default {
  mounted(el, binding) {
    const { value } = binding
    if (value && typeof value === 'string') {
      if (!hasPermission(value)) {
        el.style.display = 'none'
      }
    }
  },
  updated(el, binding) {
    const { value } = binding
    if (value && typeof value === 'string') {
      if (!hasPermission(value)) {
        el.style.display = 'none'
      } else {
        el.style.display = ''
      }
    }
  }
}

/**
 * 检查是否拥有指定权限
 * 【项目形态：仅一个管理员端】
 * 只要已登录（admin_token 存在），管理员就拥有全部权限，永远放行。
 * 这样彻底根除"新功能 permission_key 没加到 fallback / localStorage 缓存过期"
 * 导致菜单、按钮、子页面被误过滤的问题。
 * 真正的权限校验仍在后端接口层执行，前端仅控制 UI 可见性。
 *
 * @param {string} permissionKey - 权限标识
 * @returns {boolean}
 */
export function hasPermission(permissionKey) {
  // 只要有 token 就认为是管理员 → 拥有全部权限，任意 permissionKey 直接放行
  if (localStorage.getItem('admin_token')) return true
  return false
}

/**
 * 获取当前用户的权限集合
 * @returns {string[]}
 */
export function getPermissions() {
  try {
    const token = localStorage.getItem('admin_token')
    if (!token) return []
    
    // 从localStorage获取权限信息
    const permissionsStr = localStorage.getItem('admin_permissions')
    if (permissionsStr) {
      return JSON.parse(permissionsStr)
    }
    
    return []
  } catch (e) {
    console.error('获取权限失败:', e)
    return []
  }
}

/**
 * 设置权限集合
 * @param {string[]} permissions - 权限数组
 */
export function setPermissions(permissions) {
  localStorage.setItem('admin_permissions', JSON.stringify(permissions))
}

/**
 * 清除权限集合
 */
export function clearPermissions() {
  localStorage.removeItem('admin_permissions')
}