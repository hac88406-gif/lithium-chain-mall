<template>
  <div class="cart-page py-12">
    <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="page-header mb-8">
        <h1 class="page-title">我的购物车</h1>
        <p class="page-subtitle">管理您的购物车商品，确认无误后前往结算</p>
      </div>

      <div v-if="!isLoggedIn" class="empty-state py-20">
        <div class="empty-icon">
          <svg class="w-20 h-20 mx-auto text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"/>
          </svg>
        </div>
        <h3 class="text-xl font-semibold text-gray-600 mt-6 mb-3">请先登录</h3>
        <p class="text-gray-500 mb-6">登录后即可查看您的购物车</p>

        <!-- 游客本地购物车预览：登录后自动合并 -->
        <div v-if="guestItems.length > 0" class="guest-cart-preview">
          <p class="guest-cart-tip">
            您有 <strong>{{ guestItems.length }}</strong> 件商品已加入本地购物车，登录后自动合并
          </p>
          <div v-for="item in guestItems" :key="item.productId" class="guest-cart-item">
            <router-link :to="`/product/detail?id=${item.productId}`" class="guest-item-link">
              商品编号 {{ item.productId }}
            </router-link>
            <span class="guest-item-qty">× {{ item.quantity }}</span>
          </div>
        </div>

        <button class="btn-primary" @click="openLoginModal">立即登录</button>
      </div>

      <div v-else-if="cartItems.length === 0" class="empty-state py-20">
        <div class="empty-icon">
          <svg class="w-20 h-20 mx-auto text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"/>
          </svg>
        </div>
        <h3 class="text-xl font-semibold text-gray-600 mt-6 mb-3">购物车是空的</h3>
        <p class="text-gray-500 mb-6">快去挑选您喜欢的商品吧</p>
        <router-link to="/products" class="btn-primary">去购物</router-link>
      </div>

      <div v-else class="cart-content">
        <div class="cart-items">
          <div 
            v-for="item in cartItems" 
            :key="item.id" 
            class="cart-item"
          >
            <div class="item-checkbox">
              <span 
                class="checkbox" 
                :class="{ checked: selectedIds.includes(item.id) }"
                @click="toggleSelect(item.id)"
              >
                <svg v-if="selectedIds.includes(item.id)" class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
                </svg>
              </span>
            </div>
            <div class="item-image" @click="goToDetail(item.productId)">
              <img :src="getImageUrl(item.image)" :alt="item.name" />
            </div>
            <div class="item-info">
              <h3 class="item-name" @click="goToDetail(item.productId)">{{ item.name }}</h3>
              <span class="item-stock" v-if="item.stock <= 0">暂时缺货</span>
            </div>
            <div class="item-price">¥{{ item.price }}</div>
            <div class="item-quantity">
              <button class="qty-btn" @click="decreaseQty(item)" :disabled="item.quantity <= 1">-</button>
              <input type="number" v-model.number="item.quantity" class="qty-input" min="1" @blur="updateQty(item)" />
              <button class="qty-btn" @click="increaseQty(item)">+</button>
            </div>
            <div class="item-subtotal">¥{{ (parseFloat(item.price) * item.quantity).toFixed(2) }}</div>
            <div class="item-action">
              <button class="delete-btn" @click="removeItem(item.id)">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M1 7h22M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <div class="cart-summary">
          <div class="summary-left">
            <span 
              class="checkbox" 
              :class="{ checked: isAllSelected }"
              @click="toggleSelectAll"
            >
              <svg v-if="isAllSelected" class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
              </svg>
            </span>
            <span class="select-all-text">全选</span>
            <button class="clear-btn" @click="clearCart">清空购物车</button>
          </div>
          <div class="summary-right">
            <div class="total-info">
              <span>已选 <strong>{{ selectedIds.length }}</strong> 件商品</span>
              <span>合计：<strong class="total-price">¥{{ totalPrice }}</strong></span>
            </div>
            <button 
              class="btn-checkout" 
              :disabled="selectedIds.length === 0"
              @click="goCheckout"
            >
              去结算
            </button>
          </div>
        </div>
      </div>
    </div>

    <LoginModal 
      :visible="loginModalVisible"
      @close="loginModalVisible = false"
      @login="handleLoginSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index'
import { getGuestCart } from '../utils/guestCart'
import LoginModal from '../components/LoginModal.vue'

const router = useRouter()
const cartItems = ref([])
const selectedIds = ref([])
const loginModalVisible = ref(false)
const userInfo = ref(null)

// 游客本地购物车预览数据（未登录时展示）
const guestItems = ref([])

// 读取本地游客购物车（仅展示预览，登录后由 LoginModal 自动合并清空）
const loadGuestCart = () => {
  guestItems.value = getGuestCart()
}

const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const isAllSelected = computed(() => {
  return cartItems.value.length > 0 && selectedIds.value.length === cartItems.value.length
})

const totalPrice = computed(() => {
  return cartItems.value
    .filter(item => selectedIds.value.includes(item.id))
    .reduce((sum, item) => sum + parseFloat(item.price) * item.quantity, 0)
    .toFixed(2)
})

const getImageUrl = (path) => {
  if (!path) return 'https://via.placeholder.com/100x100?text=No+Image'
  if (path.startsWith('http')) return path
  return path
}

const loadCart = async () => {
  if (!isLoggedIn.value) return
  try {
    const response = await api.getCartList()
    if (response.code === 200) {
      cartItems.value = response.data || []
      selectedIds.value = cartItems.value.map(item => item.id)
    }
  } catch (e) {
    // code 401 等异常已经在 axios 全局拦截器里处理（清缓存 + 跳首页）
    console.error('Failed to load cart:', e)
  }
}

const toggleSelect = (id) => {
  const index = selectedIds.value.indexOf(id)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
  } else {
    selectedIds.value.push(id)
  }
}

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = cartItems.value.map(item => item.id)
  }
}

const increaseQty = async (item) => {
  item.quantity++
  await updateQty(item)
}

const decreaseQty = async (item) => {
  if (item.quantity > 1) {
    item.quantity--
    await updateQty(item)
  }
}

const updateQty = async (item) => {
  if (item.quantity < 1) item.quantity = 1
  try {
    const response = await api.updateCart(item.id, { quantity: item.quantity })
    if (response.code !== 200) {
      alert(response.message || '更新失败')
    }
  } catch (e) {
    console.error('Failed to update cart:', e)
  }
}

const removeItem = async (id) => {
  if (!confirm('确定要删除该商品吗？')) return
  try {
    const response = await api.deleteCart(id)
    if (response.code === 200) {
      cartItems.value = cartItems.value.filter(item => item.id !== id)
      selectedIds.value = selectedIds.value.filter(sid => sid !== id)
    }
  } catch (e) {
    console.error('Failed to remove item:', e)
  }
}

const clearCart = async () => {
  if (!confirm('确定要清空购物车吗？')) return
  try {
    const response = await api.clearCart()
    if (response.code === 200) {
      cartItems.value = []
      selectedIds.value = []
    }
  } catch (e) {
    console.error('Failed to clear cart:', e)
  }
}

const goCheckout = () => {
  if (selectedIds.value.length === 0) {
    alert('请先选择要结算的商品')
    return
  }
  router.push({
    path: '/checkout',
    query: { ids: selectedIds.value.join(',') }
  })
}

const goToDetail = (productId) => {
  router.push(`/product/detail?id=${productId}`)
}

const openLoginModal = () => {
  loginModalVisible.value = true
}

const handleLoginSuccess = (user) => {
  userInfo.value = user
  loginModalVisible.value = false
  loadCart()
}

/** 鉴权失效 / 登出时同步清空本页用户与服务端购物车数据 UI */
const onAuthCleared = () => {
  userInfo.value = null
  cartItems.value = []
  selectedIds.value = []
  loadGuestCart()
}

onMounted(() => {
  const savedUser = localStorage.getItem('userInfo')
  // 必须与 token 联动：有 userInfo 但没 token 就当做未登录处理
  if (savedUser && localStorage.getItem('token')) {
    try {
      userInfo.value = JSON.parse(savedUser)
    } catch (e) {}
  }
  loadCart()
  // 未登录时读取本地游客购物车用于预览展示
  if (!isLoggedIn.value) {
    loadGuestCart()
  }
  window.addEventListener('gc-auth-cleared', onAuthCleared)
})

onUnmounted(() => {
  window.removeEventListener('gc-auth-cleared', onAuthCleared)
})

watch(() => isLoggedIn.value, (loggedIn) => {
  if (loggedIn) loadCart()
})
</script>

<style scoped>
.cart-page {
  background: #f9fafb;
  min-height: calc(100vh - 64px);
}

.page-header {
  text-align: center;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
}

.page-subtitle {
  color: #6b7280;
  margin-top: 8px;
}

.empty-state {
  text-align: center;
}

.btn-primary {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  color: white;
  border: none;
  padding: 12px 32px;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  display: inline-block;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(5, 150, 67, 0.3);
}

/* 游客本地购物车预览 */
.guest-cart-preview {
  max-width: 420px;
  margin: 0 auto 24px;
  padding: 16px 20px;
  background: #ffffff;
  border: 1px dashed #d1d5db;
  border-radius: 10px;
  text-align: left;
}

.guest-cart-tip {
  color: #374151;
  font-size: 0.875rem;
  margin-bottom: 12px;
}

.guest-cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-top: 1px solid #f3f4f6;
  font-size: 0.875rem;
}

.guest-item-link {
  color: #059669;
  text-decoration: none;
}

.guest-item-link:hover {
  text-decoration: underline;
}

.guest-item-qty {
  color: #6b7280;
}

.cart-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.cart-items {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.cart-item {
  display: grid;
  grid-template-columns: 40px 100px 1fr 120px 140px 120px 60px;
  gap: 16px;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #f3f4f6;
}

.cart-item:last-child {
  border-bottom: none;
}

.item-checkbox {
  display: flex;
  justify-content: center;
}

.checkbox {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 2px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.checkbox.checked {
  background: #059669;
  border-color: #059669;
}

.item-image {
  width: 100px;
  height: 100px;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-name {
  font-size: 1rem;
  font-weight: 500;
  color: #1f2937;
  cursor: pointer;
  transition: color 0.2s;
}

.item-name:hover {
  color: #059669;
}

.item-stock {
  color: #ef4444;
  font-size: 0.85rem;
}

.item-price {
  color: #059669;
  font-weight: 600;
  font-size: 1rem;
}

.item-quantity {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.qty-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  color: #374151;
}

.qty-btn:hover:not(:disabled) {
  background: #f3f4f6;
}

.qty-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.qty-input {
  width: 50px;
  height: 32px;
  text-align: center;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.95rem;
}

.item-subtotal {
  color: #dc2626;
  font-weight: 600;
  font-size: 1.05rem;
  text-align: center;
}

.item-action {
  display: flex;
  justify-content: center;
}

.delete-btn {
  background: none;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  padding: 6px;
  transition: color 0.2s;
}

.delete-btn:hover {
  color: #ef4444;
}

.cart-summary {
  background: white;
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  position: sticky;
  bottom: 0;
}

.summary-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.select-all-text {
  color: #374151;
  font-size: 0.95rem;
}

.clear-btn {
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  font-size: 0.9rem;
  transition: color 0.2s;
}

.clear-btn:hover {
  color: #ef4444;
}

.summary-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.total-info {
  display: flex;
  align-items: center;
  gap: 24px;
  color: #374151;
}

.total-info strong {
  color: #1f2937;
}

.total-price {
  color: #dc2626;
  font-size: 1.3rem;
}

.btn-checkout {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  color: white;
  border: none;
  padding: 12px 36px;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-checkout:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(5, 150, 105, 0.3);
}

.btn-checkout:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .cart-item {
    grid-template-columns: 30px 80px 1fr;
    grid-template-rows: auto auto;
    gap: 12px;
  }
  
  .item-quantity, .item-subtotal, .item-price {
    grid-column: 1 / -1;
    text-align: left;
  }
  
  .cart-summary {
    flex-direction: column;
    gap: 16px;
  }
}
</style>