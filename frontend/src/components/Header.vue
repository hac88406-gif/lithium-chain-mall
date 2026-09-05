<template>
  <header class="bg-white shadow-md sticky top-0 z-50">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex items-center justify-between h-16">
        <router-link to="/" class="flex items-center space-x-3 brand-logo">
          <div class="w-8 h-8 bg-primary rounded-lg flex items-center justify-center brand-logo-icon">
            <span class="text-white text-lg font-bold leading-none">L</span>
          </div>
          <span class="text-xl font-bold text-dark">绿链锂电</span>
        </router-link>

        <nav class="hidden md:flex items-center space-x-8">
          <router-link 
            to="/" 
            class="nav-link"
            :class="{ active: currentRoute === '/' }"
          >首页</router-link>
          
          <template v-if="!currentRoute.startsWith('/cooperation')">
            <router-link 
              to="/products" 
              class="nav-link"
              :class="{ active: currentRoute === '/products' }"
            >产品商城</router-link>
          </template>

          <router-link 
            v-if="currentRoute.startsWith('/cooperation')"
            to="/cooperation/about" 
            class="nav-link"
            :class="{ active: currentRoute === '/cooperation/about' }"
          >关于我们</router-link>

          <router-link 
            v-if="currentRoute.startsWith('/cooperation')"
            to="/cooperation/technology" 
            class="nav-link"
            :class="{ active: currentRoute === '/cooperation/technology' }"
          >技术科技</router-link>

          <button 
            @click="goToCooperation"
            class="nav-btn"
            :class="{ active: currentRoute === '/cooperation' }"
          >商务合作</button>

          <router-link 
            v-if="currentRoute.startsWith('/cooperation')"
            to="/cooperation/download" 
            class="nav-link download-btn"
            :class="{ active: currentRoute === '/cooperation/download' }"
          >资料下载</router-link>

          <router-link 
            v-if="!currentRoute.startsWith('/cooperation')"
            to="/contact" 
            class="nav-link"
            :class="{ active: currentRoute === '/contact' }"
          >联系我们</router-link>

          <router-link 
            to="/my-orders" 
            class="nav-link"
            :class="{ active: currentRoute === '/my-orders' }"
          >我的订单</router-link>

          <router-link 
            to="/cart" 
            class="nav-cart-icon"
            :class="{ active: currentRoute === '/cart' }"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"/>
            </svg>
          </router-link>

          <!-- 用户菜单/登录按钮 -->
          <div class="user-dropdown" @click="toggleUserDropdown">
            <button v-if="!userInfo" class="login-btn" @click.stop="openLoginModal">
              登录
            </button>
            <button v-else class="user-btn">
              {{ userInfo.nickname || userInfo.username }}
              <svg class="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
              </svg>
            </button>
            <div v-if="userDropdownOpen && userInfo" class="dropdown-menu user-menu">
              <button 
                class="dropdown-item"
                @click.stop="goToMyOrders"
              >
                我的订单
              </button>
              <button 
                class="dropdown-item"
                @click.stop="goToMyApplications"
              >
                我的洽谈申请记录
              </button>
              <hr class="dropdown-divider">
              <button 
                class="dropdown-item logout"
                @click.stop="handleLogout"
              >
                退出登录
              </button>
            </div>
          </div>
        </nav>

        <button class="md:hidden text-gray-600" @click="toggleMenu">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
          </svg>
        </button>
      </div>

      <div v-if="menuOpen" class="md:hidden py-4 border-t">
        <nav class="flex flex-col space-y-3">
          <router-link 
            to="/" 
            class="nav-link-mobile"
            :class="{ active: currentRoute === '/' }"
            @click="menuOpen = false"
          >首页</router-link>
          
          <router-link 
            to="/products" 
            class="nav-link-mobile"
            :class="{ active: currentRoute === '/products' }"
            @click="menuOpen = false"
          >产品商城</router-link>

          <!-- 移动端登录/用户菜单 -->
          <div class="mobile-user-section">
            <template v-if="!userInfo">
              <button 
                class="login-btn-mobile"
                @click="openLoginModal"
              >
                登录
              </button>
            </template>
            <template v-else>
              <router-link 
                to="/my-orders" 
                class="nav-link-mobile"
                @click="menuOpen = false"
              >
                我的订单
              </router-link>
              <router-link 
                to="/cart" 
                class="nav-link-mobile"
                @click="menuOpen = false"
              >
                购物车
              </router-link>
              <router-link 
                to="/my-applications" 
                class="nav-link-mobile"
                @click="menuOpen = false"
              >
                我的洽谈申请记录
              </router-link>
              <button 
                class="nav-link-mobile logout"
                @click="handleLogout"
              >
                退出登录
              </button>
            </template>
          </div>
        </nav>
      </div>
    </div>

    <!-- 登录弹窗 -->
    <LoginModal 
      :visible="loginModalVisible"
      @close="closeLoginModal"
      @login="handleLoginSuccess"
    />
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import LoginModal from './LoginModal.vue'

const router = useRouter()
const route = useRoute()
const menuOpen = ref(false)
const userDropdownOpen = ref(false)
const loginModalVisible = ref(false)

// 用户信息，从localStorage读取
const userInfo = ref(null)

const currentRoute = computed(() => route.path)

const toggleMenu = () => {
  menuOpen.value = !menuOpen.value
}

const goToCooperation = () => {
  menuOpen.value = false
  router.push('/cooperation')
}

const toggleUserDropdown = () => {
  userDropdownOpen.value = !userDropdownOpen.value
}

const openLoginModal = () => {
  loginModalVisible.value = true
}

const closeLoginModal = () => {
  loginModalVisible.value = false
}

const handleLoginSuccess = (user) => {
  userInfo.value = user
}

const goToMyApplications = () => {
  menuOpen.value = false
  userDropdownOpen.value = false
  router.push('/my-applications')
}

const goToMyOrders = () => {
  menuOpen.value = false
  userDropdownOpen.value = false
  router.push('/my-orders')
}

const handleLogout = () => {
  // 登出必须与 401 清理使用同一套缓存键，避免只清 userInfo 导致
  // 残留 token 被下一次登录请求带上去，或刷新后 userInfo 又冒出来
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('rememberedUsername')
  userInfo.value = null
  userDropdownOpen.value = false
  menuOpen.value = false
  try {
    window.dispatchEvent(new CustomEvent('gc-auth-cleared'))
  } catch (e) { /* ignore */ }
  alert('已退出登录')
}

/**
 * 读取登录态：两个条件都成立才显示用户菜单
 *   ① localStorage 中有 token        —— 证明确实登录过
 *   ② localStorage 中有 userInfo JSON —— 拿到用户名/nickname 显示
 *
 * 只要缺一个（例如：后端重启 JWT 密钥轮换、token 过期被接口清掉、
 * 手工在 DevTools 里删了 token 但是 userInfo 没清……），就立即把
 * userInfo 也清掉，不把"123"这种脏数据挂在右上角继续骗人。
 */
const loadSavedLogin = () => {
  const hasToken = !!localStorage.getItem('token')
  const savedUser = hasToken ? localStorage.getItem('userInfo') : null
  if (savedUser) {
    try {
      userInfo.value = JSON.parse(savedUser)
    } catch (e) {
      console.error('Failed to parse user info:', e)
      localStorage.removeItem('userInfo')
    }
  } else {
    // 无 token，或有 token 但 userInfo 解析失败 → 直接清空用户态 UI
    userInfo.value = null
    if (!hasToken) localStorage.removeItem('userInfo')
  }
}

/** 外部（axios 拦截器 / 登出按钮）清理登录态后，所有 Header 实例同步刷新 UI */
const onAuthCleared = () => {
  userInfo.value = null
  userDropdownOpen.value = false
}

onMounted(() => {
  loadSavedLogin()
  window.addEventListener('gc-auth-cleared', onAuthCleared)
})

onUnmounted(() => {
  window.removeEventListener('gc-auth-cleared', onAuthCleared)
})
</script>

<style scoped>
.brand-logo {
  padding-left: 14px;
  text-decoration: none;
}

.nav-link {
  color: #666;
  text-decoration: none;
  transition: color 0.3s ease;
  padding: 8px 16px;
  border-radius: 8px;
}

.nav-link:hover,
.nav-link.active {
  color: #0d9488;
  background: #f0fdf4;
}

.nav-btn {
  color: #666;
  padding: 8px 16px;
  border-radius: 8px;
  text-decoration: none;
  transition: color 0.3s ease;
  font-weight: 500;
  border: none;
  cursor: pointer;
}

.nav-btn:hover,
.nav-btn.active {
  color: #0d9488;
  background: #f0fdf4;
}

.nav-link-mobile {
  color: #666;
  text-decoration: none;
  padding: 10px 16px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.nav-link-mobile:hover,
.nav-link-mobile.active {
  color: #0d9488;
  background: #f0fdf4;
}

.nav-btn-mobile {
  color: #666;
  padding: 10px 16px;
  border-radius: 6px;
  text-decoration: none;
  text-align: center;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
}

.nav-btn-mobile:hover,
.nav-btn-mobile.active {
  color: #0d9488;
  background: #f0fdf4;
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  min-width: 120px;
  padding: 4px 0;
  z-index: 100;
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 10px 16px;
  text-align: left;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #374151;
  transition: background 0.2s ease;
}

.dropdown-item:hover {
  background: #f3f4f6;
}

.dropdown-item.active {
  background: #f0fdf4;
  color: #059669;
  font-weight: 500;
}

.user-dropdown {
  position: relative;
}

.login-btn {
  padding: 8px 20px;
  background: linear-gradient(135deg, #0d9488 0%, #0891b2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 150, 136, 0.3);
}

.user-btn {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  color: #666;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.user-btn:hover {
  color: #0d9488;
  background: #f0fdf4;
}

.user-menu {
  min-width: 180px;
}

.dropdown-divider {
  margin: 4px 0;
  border: none;
  border-top: 1px solid #e5e7eb;
}

.dropdown-item.logout {
  color: #ef4444;
}

.dropdown-item.logout:hover {
  background: #fef2f2;
  color: #ef4444;
}

.login-btn-mobile {
  padding: 10px 16px;
  background: linear-gradient(135deg, #0d9488 0%, #0891b2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
}

.login-btn-mobile:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 150, 136, 0.3);
}

.nav-link-mobile.logout {
  color: #ef4444;
}

.nav-link-mobile.logout:hover {
  background: #fef2f2;
  color: #ef4444;
}

.nav-cart-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: #666;
  transition: all 0.3s ease;
  text-decoration: none;
}

.nav-cart-icon:hover,
.nav-cart-icon.active {
  color: #0d9488;
  background: #f0fdf4;
}

.mobile-user-section {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #e5e7eb;
}
</style>