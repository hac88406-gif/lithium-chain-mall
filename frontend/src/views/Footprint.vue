<template>
  <div class="footprint-page">
    <h1 class="page-title">我的浏览足迹</h1>

    <div v-if="isLoading" class="loading-center">
      <div class="inline-block w-8 h-8 border-3 border-green-500 border-t-transparent rounded-full animate-spin"></div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="footprintList.length === 0" class="empty-state">
      <div class="empty-icon">👀</div>
      <p>暂无浏览记录</p>
      <button class="btn-primary" @click="goHome">去逛逛</button>
    </div>

    <!-- 足迹网格 -->
    <div v-else class="footprint-grid">
      <div
        v-for="item in footprintList"
        :key="item.product.id"
        class="footprint-card"
        @click="goToProductDetail(item.product.id)"
      >
        <div class="fp-image">
          <img :src="getImageUrl(item.product.image)" :alt="item.product.name" />
        </div>
        <div class="fp-info">
          <h3 class="fp-name">{{ item.product.name }}</h3>
          <span class="fp-price">¥{{ item.product.price }}</span>
          <span class="fp-time">{{ formatBrowseTime(item.browseTime) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import apiModule from '../api/index.js'
const api = apiModule.api

const router = useRouter()
const footprintList = ref([])
const isLoading = ref(true)

const getImageUrl = (path) => {
  if (!path) return 'https://via.placeholder.com/200x200?text=No+Image'
  if (path.startsWith('http')) return path
  return path
}

const formatBrowseTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  // 1 小时内
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  // 24 小时内
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  // 7 天内
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return date.toLocaleDateString('zh-CN')
}

const goToProductDetail = (id) => {
  router.push(`/product/detail?id=${id}`)
}

const goHome = () => {
  router.push('/')
}

const loadFootprint = async () => {
  isLoading.value = true
  try {
    const res = await api.getFootprint()
    if (res.code === 200) {
      footprintList.value = res.data || []
    }
  } catch (e) {
    console.error('加载足迹失败：', e)
    footprintList.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadFootprint()
})
</script>

<style scoped>
.footprint-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 40px 16px 60px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.loading-center {
  text-align: center;
  padding: 60px 0;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #9ca3af;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.btn-primary {
  margin-top: 20px;
  padding: 10px 28px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-primary:hover {
  opacity: 0.9;
}

.footprint-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.footprint-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.footprint-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.fp-image {
  width: 100%;
  aspect-ratio: 1/1;
  overflow: hidden;
  background: #f3f4f6;
}

.fp-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.fp-info {
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fp-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fp-price {
  color: #059669;
  font-weight: 600;
  font-size: 15px;
}

.fp-time {
  font-size: 12px;
  color: #9ca3af;
}
</style>
