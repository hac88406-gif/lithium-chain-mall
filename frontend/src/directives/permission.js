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
 * 规则（与后端 PermissionContext.hasPermission 对齐）：
 * - 未登录（无 admin_token）：一律无权限
 * - 超级管理员（admin_role_id === 1）：拥有全部权限，直接放行
 * - 其他角色：精确匹配 localStorage 中的 admin_permissions 集合
 *   （admin_permissions 由登录接口返回并写入，见 Login.vue setPermissions）
 *
 * 注意：后端 PermissionInterceptor 才是真正的权限防线（@RequiresPermission 注解），
 * 前端仅用于菜单/按钮/路由的 UI 可见性控制。
 *
 * @param {string} permissionKey - 权限标识
 * @returns {boolean}
 */
export function hasPermission(permissionKey) {
  // 未登录：无权限
  if (!localStorage.getItem('admin_token')) return false
  // 超级管理员（roleId=1）：与后端 isSuperAdmin / 通配符 "*" 行为一致
  if (Number(localStorage.getItem('admin_role_id')) === 1) return true

  const permissions = getPermissions()
  // 通配符兜底（后端 admin 账号返回 Set.of("*") 时前端也放行）
  if (permissions.includes('*')) return true
  return permissions.includes(permissionKey)
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