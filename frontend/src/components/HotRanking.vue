<template>
  <aside class="hot-ranking">
    <div class="ranking-header">
      <span class="ranking-title">🔥 热销榜 TOP10</span>
      <span class="ranking-sub">以产品总销量统计</span>
    </div>
    <div v-if="isLoading" class="ranking-loading">加载中...</div>
    <div v-else-if="rankProducts.length === 0" class="ranking-empty">暂无热销数据</div>
    <ul v-else class="ranking-list">
      <li
        v-for="(item, index) in rankProducts"
        :key="item.product.id"
        class="ranking-item"
        @click="goToProductDetail(item.product.id)"
      >
        <span class="rank-num" :class="{ top3: index < 3 }">{{ index + 1 }}</span>
        <img :src="getImageUrl(item.product.image)" class="rank-img" />
        <div class="rank-info">
          <div class="rank-name" :title="item.product.name">
            {{ item.product.name }}
          </div>
          <div class="rank-meta">
            <span class="rank-price">¥{{ fmt(item.product.price) }}</span>
            <span class="rank-sales">已售 {{ fmt(item.sales) }}</span>
          </div>
        </div>
      </li>
    </ul>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index'

const router = useRouter()
// rank 接口返回 [{ product, sales }, ...]，sales 取自 product.sales 真实字段
const rawRankItems = ref([])
const isLoading = ref(true)

const getImageUrl = (path) => {
  if (!path) return 'https://via.placeholder.com/60x60?text=No+Img'
  if (path.startsWith('http')) return path
  return path
}

const fmt = (v) => {
  const n = Number(v) || 0
  if (n >= 10000) return (n / 10000).toFixed(n >= 100000 ? 0 : 1) + '万'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

const goToProductDetail = (id) => {
  router.push(`/product/detail?id=${id}`)
}

/**
 * 后端已按销量降序返回最多 10 条上架商品，直接透传即可。
 * 不再用 aggregateSpu 合并 SPU —— 每条就是一个独立排名项，保证 TOP10 数量。
 */
const rankProducts = computed(() => {
  return rawRankItems.value.slice(0, 10)
})

const loadRank = async () => {
  isLoading.value = true
  try {
    const res = await api.getRankTop10()
    if (res.code === 200) {
      rawRankItems.value = res.data || []
    }
  } catch (e) {
    console.warn('[HotRanking] 加载热销榜失败，隐藏榜单：', e.message)
    rawRankItems.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadRank()
})
</script>

<style scoped>
.hot-ranking {
  width: 280px;
  flex-shrink: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  align-self: flex-start;
  position: sticky;
  top: 20px;
}

.ranking-header {
  padding: 14px 16px 10px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ranking-title {
  font-weight: 700;
  font-size: 16px;
  color: #1f2937;
}

.ranking-sub {
  font-size: 11px;
  color: #9ca3af;
  font-weight: 500;
}

.ranking-loading,
.ranking-empty {
  padding: 30px 16px;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
}

.ranking-list {
  list-style: none;
  padding: 8px 0;
  margin: 0;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.ranking-item:hover {
  background: #f9fafb;
}

.rank-num {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: #e5e7eb;
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rank-num.top3 {
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  color: white;
}

.rank-img {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
  background: #f3f4f6;
}

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-name {
  font-size: 13px;
  color: #1f2937;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.3;
}

.rank-sku-count {
  color: #b45309;
  font-weight: 600;
  font-size: 11px;
}

.rank-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 2px;
}

.rank-price {
  color: #059669;
  font-size: 13px;
  font-weight: 600;
}

.rank-sales {
  font-size: 11px;
  color: #9ca3af;
}
</style>
