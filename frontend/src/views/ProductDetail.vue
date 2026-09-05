<template>
  <div class="product-detail-page py-16">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="breadcrumbs mb-8">
        <router-link to="/" class="breadcrumb-link">首页</router-link>
        <span class="breadcrumb-separator">/</span>
        <router-link to="/products" class="breadcrumb-link">产品商城</router-link>
        <span class="breadcrumb-separator">/</span>
        <span class="breadcrumb-current">{{ spuName || '商品详情' }}</span>
      </div>

      <div v-if="isLoading" class="text-center py-20">
        <div class="inline-block w-10 h-10 border-4 border-green-500 border-t-transparent rounded-full animate-spin"></div>
      </div>

      <div v-else-if="loadError" class="text-center py-20">
        <p class="text-gray-500 mb-4">{{ loadError }}</p>
        <router-link to="/products" class="btn-primary">返回商品列表</router-link>
      </div>

      <div v-else class="product-detail">
        <div class="product-image-section">
          <div class="main-image">
            <img :src="productImage" :alt="spuName" />
          </div>
        </div>

        <div class="product-info-section">
          <div class="product-title-row">
            <h1 class="product-title">{{ spuName }}</h1>
            <span v-if="skuVariants.length > 1" class="product-sku-chip">
              全系列 {{ skuVariants.length }} 个规格
            </span>
          </div>
          <div v-if="selectedSpec && skuVariants.length > 1" class="product-current-spec">
            📌 当前选择型号：<b>{{ selectedSpec }}</b>
            <span class="product-current-spec-sales">（全网热销 {{ totalSalesOfSpu }} 件）</span>
          </div>
          <div class="product-price-main">
            <span class="price-label">价格</span>
            <span class="price-value">¥{{ currentProduct.price }}</span>
            <span class="price-unit">/ {{ currentUnit }}</span>
          </div>
          <p class="product-desc">{{ currentProduct.description }}</p>

          <!-- ========= 规格选择（只有当该 SPU 有多个 SKU 时显示） ========= -->
          <div v-if="skuVariants.length > 1" class="sku-selector">
            <div class="sku-selector-title">
              <span>选择型号规格</span>
              <span class="sku-selected-hint">
                已选：<b>{{ selectedSpec }}</b>
              </span>
            </div>
            <div class="sku-options">
              <!--
                规格展示规则（业务约定）：
                - 同系列所有规格都显示，包括已下架/缺货的；
                - 已下架（管理员后台点"下架"，status=0）或缺货（stock<=0）的规格置灰、不可点击；
                - 只有管理员在后台"删除"规格，该规格才从这里消失（DB 物理删除，接口不再返回）。
              -->
              <div
                v-for="variant in skuVariants"
                :key="variant.id"
                class="sku-option"
                :class="{
                  active: selectedVariantId === variant.id,
                  disabled: variant.__disabled
                }"
                @click="selectVariant(variant)"
              >
                <div class="sku-option-main">{{ variant.__specText }}</div>
                <div class="sku-option-meta">
                  <span class="sku-option-price">¥{{ formatPrice(variant.price) }}</span>
                  <!-- 库存数量对客户隐藏，仅展示状态：已下架 / 缺货 -->
                  <span v-if="variant.__disabled" class="sku-option-stock low">{{ variant.__disabledReason }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="product-meta">
            <!-- 库存对客户隐藏，仅保留销量 -->
            <div class="meta-item">
              <span class="meta-label">销量</span>
              <span class="meta-value">{{ currentProduct.sales || 0 }}</span>
            </div>
          </div>

          <div class="pricing-section">
            <div class="quantity-control">
              <span class="qty-label">数量：</span>
              <div class="qty-stepper">
                <button class="qty-btn" @click="decreaseQty" :disabled="selectedVariantDisabled || quantity <= 1">-</button>
                <input type="number" v-model="quantity" class="qty-input" min="1" :disabled="selectedVariantDisabled" @blur="onQuantityInput" />
                <button class="qty-btn" @click="increaseQty" :disabled="selectedVariantDisabled">+</button>
              </div>
              <span class="qty-unit">{{ stockUnit }}</span>
            </div>
          </div>

          <div class="order-section">
            <div class="total-amount">
              <span class="total-label">总金额：</span>
              <span class="total-value">¥{{ totalAmount }}</span>
            </div>
            <!-- 当前选中规格下架/缺货时提示改选其他规格 -->
            <p v-if="selectedVariantDisabled" class="purchase-hint">
              该规格已「{{ selectedVariant?.__disabledReason }}」，请选择其他规格
            </p>
            <div class="order-buttons">
              <button class="btn-cart" :disabled="selectedVariantDisabled" @click="handleAddToCart">加入购物车</button>
              <button class="btn-primary" :disabled="selectedVariantDisabled" @click="handleBuyNow">立即下单</button>
            </div>
          </div>

          <div class="service-info">
            <div class="service-item">
              <svg class="service-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
              </svg>
              <span>现货发货</span>
            </div>
            <div class="service-item">
              <svg class="service-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
              </svg>
              <span>质量保证</span>
            </div>
            <div class="service-item">
              <svg class="service-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
              </svg>
              <span>售后保障</span>
            </div>
          </div>
        </div>
      </div>

      <div class="cooperation-banner mt-16 cursor-pointer" @click="goToCooperation">
        <div class="banner-content">
          <div class="banner-icon">
            <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
            </svg>
          </div>
          <div class="banner-text">
            <p class="banner-title">大批量采购、定制代工</p>
            <p class="banner-subtitle">前往「商务合作」洽谈工厂阶梯优惠价</p>
          </div>
          <div class="banner-arrow">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3"/>
            </svg>
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
import { useRouter, useRoute } from 'vue-router'
import { api } from '../api/index'
import { saveGuestCart, getGuestCart } from '../utils/guestCart'
import { spuKeyOf, extractSpec } from '../utils/spu'
import LoginModal from '../components/LoginModal.vue'

const router = useRouter()
const route = useRoute()
const quantity = ref(1)
const loginModalVisible = ref(false)
const userInfo = ref(null)
const isLoading = ref(false)
const loadError = ref('')
const currentProduct = ref({})

// ===== SPU 聚合相关状态 =====
// 同 SPU 的所有兄弟 SKU（含当前 SKU，默认选中当前）
const skuVariants = ref([])
const selectedVariantId = ref(null)

// 根据当前选中的 SKU 派生
const selectedVariant = computed(() =>
  skuVariants.value.find((v) => v.id === selectedVariantId.value) || null
)

// 当前选中规格是否不可购买（已下架 status=0 或 缺货 stock<=0）
const selectedVariantDisabled = computed(() => !!selectedVariant.value?.__disabled)

/**
 * 选择规格：下架/缺货规格置灰且不可点击；
 * 同组内切换始终允许（可买规格之间自由切换）。
 */
const selectVariant = (variant) => {
  if (!variant || variant.__disabled) return
  if (selectedVariantId.value === variant.id) return
  selectedVariantId.value = variant.id
}

// 当 skuVariants 有多个或只有一个，currentProduct 跟随 selectedVariant
// 这样后面加购 / 价格 / 库存全部联动，无需重写原逻辑
watch(selectedVariant, (v) => {
  if (!v) return
  currentProduct.value = v
  if (quantity.value > v.stock) quantity.value = Math.max(1, Number(v.stock) || 1)
}, { immediate: false })

// SPU 名（去掉所有规格）
const spuName = computed(() => spuKeyOf(currentProduct.value))
const selectedSpec = computed(() => selectedVariant.value?.__specText || extractSpec(currentProduct.value))
const totalSalesOfSpu = computed(() =>
  skuVariants.value.reduce((s, v) => s + Number(v.sales || 0), 0)
)

const formatPrice = (v) => {
  const n = Number(v) || 0
  if (n >= 10000) return (n / 10000).toFixed(n >= 100000 ? 0 : 1) + '万'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 2, minimumFractionDigits: n % 1 === 0 ? 0 : 2 })
}

const defaultImage = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCBmaWxsPSIjZjBmMGYwIiB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMjAiIGZpbGw9IiM5OTkiIGRvbWluYW50LWJhc2VsaW5lPSJtaWRkbGUiIHRleHQtYW5jaG9yPSJtaWRkbGUiPuWujOi/neWNnOaWsOWKoOW3peWKoOS6puihqCBJbWFnZSBMb2FkZWQiPC90ZXh0Pjwvc3ZnPg=='

const currentUnit = computed(() => {
  if (currentProduct.value.name && currentProduct.value.name.includes('电芯')) return '节'
  if (currentProduct.value.name && currentProduct.value.name.includes('模组')) return '组'
  if (currentProduct.value.name && currentProduct.value.name.includes('电池')) return '个'
  if (currentProduct.value.name && currentProduct.value.name.includes('BMS')) return '个'
  if (currentProduct.value.name && currentProduct.value.name.includes('电源')) return '台'
  return '件'
})

const stockUnit = computed(() => currentUnit.value)

const totalAmount = computed(() => {
  const price = parseFloat(currentProduct.value.price) || 0
  return (price * quantity.value).toFixed(2)
})

const productImage = computed(() => {
  const img = currentProduct.value.image
  if (!img) return defaultImage
  if (img.startsWith('http')) return img
  return img
})

const loadProduct = async () => {
  const productId = route.query.id
  if (!productId) {
    loadError.value = '商品参数错误'
    return
  }

  isLoading.value = true
  loadError.value = ''
  skuVariants.value = []
  selectedVariantId.value = null
  quantity.value = 1
  try {
    const res = await api.getProductById(productId)
    if (res.code !== 200 || !res.data) {
      loadError.value = (res && res.message) || '商品不存在'
      return
    }
    const main = res.data
    currentProduct.value = main

    /* ===== 查"同 SPU"的所有兄弟 SKU（含已下架，不含已删除） =====
     *   - 专用接口 /client/products/{id}/siblings：返回同分类下"上架 + 下架"商品；
     *     商城列表接口 /client/products 只返回上架商品，用它会导致下架规格从选择器里"消失"。
     *   - 聚合键：categoryId + spuKeyOf(name)，规则统一在前端 utils/spu.js。
     *   - 下架(status=0)/缺货(stock<=0)规格打 __disabled 标记：模板置灰、不可选，
     *     但仍然展示；只有管理员在后台删除规格（DB 物理删除）才不会返回。
     */
    try {
      const sibRes = await api.getProductSiblings(main.id)
      const all = (sibRes?.code === 200 && Array.isArray(sibRes.data)) ? sibRes.data : []
      const myKey = `${main.categoryId || 0}#${spuKeyOf(main)}`
      // 计算单个 SKU 的可购买状态标记
      const decorate = (p) => {
        const offShelf = String(p.status) === '0'
        const soldout = !p.stock || Number(p.stock) <= 0
        return {
          ...p,
          __specText: extractSpec(p),
          __offShelf: offShelf,
          __disabled: offShelf || soldout,
          __disabledReason: offShelf ? '已下架' : (soldout ? '缺货' : '')
        }
      }
      const siblings = all
        .filter((p) => `${p.categoryId || 0}#${spuKeyOf(p)}` === myKey)
        .map(decorate)
        .sort((a, b) => {
          // 可买规格排前面；同状态按价格升序、库存降序
          if (a.__disabled !== b.__disabled) return a.__disabled ? 1 : -1
          if (Number(a.price) !== Number(b.price)) return Number(a.price) - Number(b.price)
          return Number(b.stock || 0) - Number(a.stock || 0)
        })
      skuVariants.value = siblings.length ? siblings : [decorate(main)]
      // 默认选中：当前规格可买→选它；当前规格已下架（用户拿旧链接进来）→自动选第一个可买规格；
      // 全系列都不可买→仍选中当前（按钮置灰并提示）
      const mainInList = skuVariants.value.find((v) => String(v.id) === String(main.id))
      const firstAvailable = skuVariants.value.find((v) => !v.__disabled)
      const target = (mainInList && !mainInList.__disabled)
        ? mainInList
        : (firstAvailable || mainInList || skuVariants.value[0])
      selectedVariantId.value = target.id
    } catch (e) {
      // 兄弟 SKU 拉失败，回退到只有当前 SKU（不显示多规格卡片，length==1）
      console.warn('[ProductDetail] 拉取同 SPU 兄弟 SKU 失败，仅展示当前规格：', e.message)
      const offShelf = String(main.status) === '0'
      const soldout = !main.stock || Number(main.stock) <= 0
      skuVariants.value = [{
        ...main,
        __specText: extractSpec(main),
        __offShelf: offShelf,
        __disabled: offShelf || soldout,
        __disabledReason: offShelf ? '已下架' : (soldout ? '缺货' : '')
      }]
      selectedVariantId.value = main.id
    }
  } catch (e) {
    console.error('Failed to load product:', e)
    loadError.value = '加载商品失败，请稍后重试'
  } finally {
    isLoading.value = false
  }
}

const increaseQty = () => {
  const stock = Number(currentProduct.value.stock) || 0
  if (!stock) return
  if (quantity.value < stock) {
    quantity.value++
  } else {
    alert(`该规格最多买 ${stock} 件`)
  }
}

const decreaseQty = () => {
  if (quantity.value > 1) {
    quantity.value--
  }
}

const onQuantityInput = () => {
  if (quantity.value < 1) {
    quantity.value = 1
    return
  }
  const stock = Number(currentProduct.value.stock) || 0
  if (stock && quantity.value > stock) {
    quantity.value = stock
    alert(`该规格最多买 ${stock} 件`)
  }
}

const isLoggedIn = () => !!localStorage.getItem('token')

const ensureLogin = () => {
  if (!isLoggedIn()) {
    loginModalVisible.value = true
    return false
  }
  return true
}

const handleAddToCart = async () => {
  if (!currentProduct.value.id) return

  // 未登录：不请求后端，追加写入本地游客购物车（登录后自动合并）
  if (!isLoggedIn()) {
    const list = getGuestCart()
    const exist = list.find(item => item.productId === currentProduct.value.id)
    if (exist) {
      // 本地已有该商品，数量累加
      exist.quantity += quantity.value
    } else {
      list.push({ productId: currentProduct.value.id, quantity: quantity.value })
    }
    saveGuestCart(list)
    alert('已加入本地购物车，登录后自动合并')
    return
  }

  // 已登录：保持原有逻辑，直接调用后端加购接口
  try {
    const res = await api.addCart({
      productId: currentProduct.value.id,
      quantity: quantity.value
    })
    if (res.code === 200) {
      alert('已加入购物车！')
    } else {
      alert(res.message || '加入购物车失败')
    }
  } catch (e) {
    console.error('Failed to add cart:', e)
    alert('加入购物车失败，请重试')
  }
}

const handleBuyNow = async () => {
  // 下架/缺货规格：先拦截，避免弹登录框后才发现不能买
  if (currentProduct.value.__disabled) {
    alert(`该规格已「${currentProduct.value.__disabledReason || '不可购买'}」，请选择其他规格`)
    return
  }
  if (!ensureLogin()) return
  if (!currentProduct.value.id) return

  try {
    const addRes = await api.addCart({
      productId: currentProduct.value.id,
      quantity: quantity.value
    })
    if (addRes.code === 200) {
      const cartRes = await api.getCartList()
      if (cartRes.code === 200) {
        const items = cartRes.data || []
        const ids = items.map(item => item.id).join(',')
        router.push({ path: '/checkout', query: { ids } })
      }
    } else {
      alert(addRes.message || '下单失败')
    }
  } catch (e) {
    console.error('Failed to buy:', e)
    alert('下单失败，请重试')
  }
}

const goToCooperation = () => {
  router.push('/cooperation')
}

const onAuthCleared = () => {
  userInfo.value = null
  loginModalVisible.value = false
}

const handleLoginSuccess = (user) => {
  userInfo.value = user
  loginModalVisible.value = false
}

onMounted(() => {
  const savedUser = localStorage.getItem('userInfo')
  if (savedUser && localStorage.getItem('token')) {
    try {
      userInfo.value = JSON.parse(savedUser)
    } catch (e) {}
  }
  window.addEventListener('gc-auth-cleared', onAuthCleared)
  loadProduct()
})

onUnmounted(() => {
  window.removeEventListener('gc-auth-cleared', onAuthCleared)
})

watch(() => route.query.id, () => {
  quantity.value = 1
  loadProduct()
})
</script>

<style scoped>
.product-detail-page {
  background: #f9fafb;
}

.breadcrumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #6B7280;
}

.breadcrumb-link {
  color: #059669;
  text-decoration: none;
}

.breadcrumb-link:hover {
  text-decoration: underline;
}

.breadcrumb-separator {
  color: #9CA3AF;
}

.breadcrumb-current {
  color: #1f2937;
  font-weight: 500;
}

.product-detail {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 48px;
  background: white;
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.product-image-section {
  display: flex;
  justify-content: center;
  align-items: center;
}

.main-image {
  width: 100%;
  max-width: 450px;
  aspect-ratio: 1/1;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.product-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.product-title {
  font-size: 1.8rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}
.product-sku-chip {
  padding: 4px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(13,148,136,.1), rgba(6,182,212,.1));
  color: #0f766e;
  border: 1px solid rgba(13,148,136,.2);
  font-size: 12px;
  font-weight: 600;
}
.product-current-spec {
  padding: 10px 14px;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(245,158,11,.08), #fff7ed);
  border: 1px solid rgba(245,158,11,.25);
  color: #92400e;
  font-size: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.product-current-spec b {
  color: #78350f;
}
.product-current-spec-sales {
  margin-left: auto;
  color: #a16207;
  font-weight: 500;
  font-size: 13px;
}

/* ============ 规格选择器 ============ */
.sku-selector {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 14px;
  background: #fafbfc;
  border: 1px solid #eef2f7;
}
.sku-selector-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}
.sku-selected-hint {
  color: #475569;
  font-weight: 500;
  font-size: 13px;
}
.sku-selected-hint b {
  color: #0d9488;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
}
.sku-options {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
}
.sku-option {
  padding: 10px 12px;
  border-radius: 12px;
  background: #fff;
  border: 1.5px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.15s ease;
}
.sku-option:hover:not(.disabled) {
  border-color: #0d9488;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(13,148,136,.08);
}
.sku-option.active {
  border-color: #0d9488;
  background: linear-gradient(135deg, rgba(13,148,136,.06), #ffffff);
  box-shadow: 0 0 0 3px rgba(13,148,136,.1);
}
/* 已下架/缺货规格：置灰、斜纹底、禁止点击（仍然展示，不隐藏） */
.sku-option.disabled {
  opacity: 0.55;
  cursor: not-allowed;
  background: repeating-linear-gradient(
    45deg, #f8fafc, #f8fafc 6px, #f1f5f9 6px, #f1f5f9 12px
  );
}
.sku-option.disabled .sku-option-main,
.sku-option.disabled .sku-option-price {
  color: #94a3b8;
}
.sku-option-main {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 6px;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  letter-spacing: 0.2px;
}
.sku-option-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.sku-option-price {
  color: #059669;
  font-weight: 700;
  font-size: 13px;
}
.sku-option-stock {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
}
.sku-option-stock.low {
  color: #ef4444;
}

.product-price-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price-label {
  color: #6B7280;
  font-size: 1rem;
}

.price-value {
  color: #059669;
  font-size: 2.5rem;
  font-weight: 700;
}

.price-unit {
  color: #6B7280;
}

.product-desc {
  color: #6B7280;
  line-height: 1.7;
  font-size: 1rem;
}

.product-meta {
  display: flex;
  gap: 32px;
  padding: 16px 0;
  border-top: 1px solid #E5E7EB;
  border-bottom: 1px solid #E5E7EB;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  color: #9CA3AF;
  font-size: 0.85rem;
}

.meta-value {
  color: #1f2937;
  font-weight: 600;
  font-size: 1.1rem;
}

.meta-value.low {
  color: #dc2626;
}

.pricing-section {
  padding-top: 16px;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 12px;
}

.qty-label {
  color: #6B7280;
  font-size: 0.95rem;
}

.qty-stepper {
  display: flex;
  align-items: center;
  border: 1px solid #D1D5DB;
  border-radius: 8px;
  overflow: hidden;
}

.qty-btn {
  width: 40px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
  border: none;
  border-right: 1px solid #D1D5DB;
  cursor: pointer;
  font-size: 1.1rem;
  color: #374151;
  transition: background 0.15s;
}

.qty-stepper .qty-btn:last-child {
  border-right: none;
  border-left: 1px solid #D1D5DB;
}

.qty-btn:hover:not(:disabled) {
  background: #ecfdf5;
  color: #059669;
}

.qty-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.qty-input {
  width: 64px;
  height: 38px;
  text-align: center;
  border: none;
  font-size: 1rem;
  outline: none;
  -moz-appearance: textfield;
}

.qty-input::-webkit-outer-spin-button,
.qty-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.qty-unit {
  color: #6B7280;
  font-size: 0.95rem;
}

.order-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  background: #f0fdf4;
  border-radius: 12px;
}

.total-amount {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.total-label {
  color: #6B7280;
  font-size: 0.95rem;
}

.total-value {
  color: #dc2626;
  font-size: 1.8rem;
  font-weight: 700;
}

.order-buttons {
  display: flex;
  gap: 12px;
}

/* 下架/缺货规格的提示文案 */
.purchase-hint {
  margin: 0 0 10px;
  font-size: 13px;
  color: #ef4444;
  font-weight: 500;
}

/* 按钮禁用态（下架/缺货规格时） */
.order-buttons button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

.btn-cart {
  flex: 1;
  background: white;
  color: #059669;
  border: 2px solid #059669;
  padding: 14px 24px;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cart:hover {
  background: #f0fdf4;
  transform: translateY(-2px);
}

.btn-primary {
  flex: 1;
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  color: white;
  border: none;
  padding: 14px 24px;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  text-decoration: none;
  display: inline-block;
  text-align: center;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(5, 150, 105, 0.3);
}

.service-info {
  display: flex;
  gap: 32px;
}

.service-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6B7280;
  font-size: 0.9rem;
}

.service-icon {
  width: 20px;
  height: 20px;
  color: #059669;
}

.cooperation-banner {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  border-radius: 16px;
  padding: 24px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.cooperation-banner:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(5, 150, 105, 0.3);
}

.banner-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.banner-icon {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.banner-text {
  flex: 1;
}

.banner-title {
  color: white;
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 4px;
}

.banner-subtitle {
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.9rem;
}

.banner-arrow {
  color: white;
  flex-shrink: 0;
}

@media (max-width: 1024px) {
  .product-detail {
    grid-template-columns: 1fr;
    gap: 32px;
  }
}

@media (max-width: 640px) {
  .service-info {
    flex-wrap: wrap;
    gap: 16px;
  }

  .banner-content {
    flex-direction: column;
    text-align: center;
  }
  
  .order-buttons {
    flex-direction: column;
  }
}
</style>