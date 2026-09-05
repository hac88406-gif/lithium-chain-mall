/**
 * 游客购物车工具（localStorage）
 *
 * 存储格式：[{productId: number, quantity: number}, ...]
 * 未登录时加购写入本地，登录成功后由 LoginModal 调用后端合并接口并清空。
 */

// localStorage 存储键名
const GUEST_CART_KEY = 'guest_cart'

/**
 * 写入游客购物车到 localStorage
 * @param {Array<{productId:number, quantity:number}>} list 游客购物车列表
 */
export function saveGuestCart(list) {
  localStorage.setItem(GUEST_CART_KEY, JSON.stringify(list || []))
}

/**
 * 读取游客购物车
 * @returns {Array<{productId:number, quantity:number}>} 无数据或解析失败时返回空数组
 */
export function getGuestCart() {
  try {
    const raw = localStorage.getItem(GUEST_CART_KEY)
    const list = raw ? JSON.parse(raw) : []
    return Array.isArray(list) ? list : []
  } catch (e) {
    // 数据损坏时重置为空，避免影响后续加购
    console.warn('guest_cart 数据解析失败，已重置:', e)
    return []
  }
}
