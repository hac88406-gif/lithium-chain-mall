<template>
  <section id="products" class="ps-root">
    <div class="ps-inner">
      <div class="ps-title-bar">
        <h2 class="ps-title">
          <span class="ps-title-accent">热销现货</span> 产品
        </h2>
        <p class="ps-sub">精选优质锂电产品 · 工厂现货直发 · 48 小时发货</p>
      </div>

      <!-- 搜索栏：标题与分类筛选之间，主题色沿用翡翠绿 -->
      <div class="ps-search-row">
        <input
          v-model="searchKeyword"
          class="ps-search-input"
          type="text"
          placeholder="请输入产品名称/型号进行搜索"
          @keyup.enter="onSearch"
        />
        <button class="ps-search-btn" @click="onSearch">搜索</button>
        <button v-if="searchKeyword" class="ps-search-clear" @click="onClearSearch" title="清除搜索">✕</button>
      </div>

      <!-- 分类筛选：从后端动态加载；首项"全部"，后续按 sort 排序 -->
      <div class="ps-filter-row">
        <button
          v-for="cat in categoriesAll"
          :key="'f_' + cat.id"
          class="ps-filter-btn"
          :class="{ active: selectedCategory === cat.id }"
          @click="onSelectCategory(cat.id)"
        >
          {{ cat.name }}
        </button>
      </div>

      <div v-if="isLoading" class="ps-empty">
        <div class="spinner"></div>
        <p>正在加载商品...</p>
      </div>
      <div v-else-if="filteredSpu.length === 0" class="ps-empty">
        <svg class="ps-empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/>
        </svg>
        <p class="ps-empty-t1">
          {{ searchKeyword ? '没有找到匹配的产品' : '该分类暂无商品' }}
        </p>
        <p class="ps-empty-t2">
          {{ searchKeyword ? '试试换个关键词，或清空搜索查看全部商品' : '试试「全部」，或稍后再来看新品' }}
        </p>
      </div>

      <div v-else class="ps-grid">
        <article
          v-for="product in filteredSpu"
          :key="product.id"
          class="ps-card"
          @click="goToProductDetail(product.id)"
        >
          <div class="ps-card-img-wrap">
            <img :src="resolveImage(product.image || product.imageUrl)" :alt="product.name" />
            <!-- ===== 差异化蒙版：按商品 id 生成固定配色，同图不同 SKU 肉眼可区分 ===== -->
            <div class="ps-card-tint" :style="tintStyle(product.id)"></div>
            <!-- ===== 左上：型号/分类角标（白底深字，避免与渐变冲突） ===== -->
            <div class="ps-card-tag ps-card-tag--cat" :style="tagStyle(product.id, 0)">
              <span class="ps-card-tag-label">MODEL</span>
              <span class="ps-card-tag-value">{{ modelCode(product) }}</span>
            </div>
            <!-- ===== 右上：系列色点（给每个商品一个独立"身份色"） ===== -->
            <div class="ps-card-tag ps-card-tag--accent" :style="tagStyle(product.id, 1)">
              <span class="ps-card-dot" :style="{ background: accentColor(product.id, 2) }"></span>
              <span class="ps-card-series">{{ seriesText(product) }}</span>
            </div>
            <!-- ===== 原有业务徽章（左下 + 右下，位置避开角标） ===== -->
            <div v-if="product.sales && product.sales > 100" class="ps-card-badge hot">
              热销 TOP
            </div>
            <div v-if="product.stock != null && product.stock < 50" class="ps-card-badge low">
              库存紧张
            </div>
          </div>
          <div class="ps-card-body">
            <div class="ps-card-category">
              {{ product.categoryName || '推荐好物' }}
              <span v-if="product.skuCount && product.skuCount > 1" class="ps-card-sku-tag">
                共 {{ product.skuCount }} 款
              </span>
            </div>
            <h3 class="ps-card-name">{{ product.name }}</h3>
            <p class="ps-card-desc">{{ truncate(product.description, 48) }}</p>
            <div class="ps-card-footer">
              <div class="ps-card-price-wrap">
                <span class="ps-card-price-symbol">¥</span>
                <span class="ps-card-price">{{ formatMoney(product.minPrice) }}</span>
              </div>
              <div class="ps-card-sales">{{ product.totalSales || 0 }}+ 已售</div>
            </div>
          </div>
          <div class="ps-card-hover-btn">查看详情 →</div>
        </article>
      </div>


    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index'
import { aggregateSpu } from '../utils/spu'

// 可选 prop：限制展示的 SPU 数量；首页传 6，产品商城不传（展示全部）
const props = defineProps({
  maxItems: { type: Number, default: 0 } // 0 = 不限制
})

const router = useRouter()
const products = ref([])
const rawCategories = ref([])
const selectedCategory = ref(0)
const isLoading = ref(true)
const searchKeyword = ref('') // 搜索关键词（空=全部商品）

// "全部 + 后端分类" 合成（全部 id=0，前端约定）
const categoriesAll = computed(() => {
  const base = [{ id: 0, name: '全部' }]
  if (rawCategories.value.length) return base.concat(rawCategories.value)
  return base.concat([
    { id: 1, name: '锂电池组' },
    { id: 2, name: '家用储能电池' },
    { id: 3, name: '便携式储能电源' },
    { id: 4, name: '光伏组件' },
    { id: 5, name: 'BMS电池管理系统' },
    { id: 6, name: '充电配件' }
  ])
})

const filteredProducts = computed(() => {
  if (selectedCategory.value === 0) return products.value
  return products.value.filter(
    (p) => Number(p.categoryId) === Number(selectedCategory.value)
  )
})

/**
 * SPU / SKU 聚合（SKU 列表 → 按产品基础名聚合为 SPU 卡片）
 * 核心逻辑统一在 @/utils/spu.js
 */

// ProductSection 最终用 SPU 列表渲染；首页通过 maxItems 截取前 N 条
const filteredSpu = computed(() => {
  const all = aggregateSpu(filteredProducts.value)
  return props.maxItems > 0 ? all.slice(0, props.maxItems) : all
})

const onSelectCategory = (id) => {
  selectedCategory.value = id
}

const formatMoney = (v) => {
  if (v == null) return '0'
  const n = Number(v)
  if (n >= 10000) return (n / 10000).toFixed(n >= 100000 ? 0 : 1) + '万'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 0 })
}

const truncate = (s, n) => {
  if (!s) return '工厂现货 · 正品保障 · 量大从优'
  return s.length > n ? s.slice(0, n) + '…' : s
}

const resolveImage = (src) => {
  if (!src) return defaultImage
  if (src.startsWith('http') || src.startsWith('data:')) return src
  return src
}

/* ==========================================================================
 * 图片差异化（解决"58 个 SKU 只有 17 张图，大量重复"的视觉问题）
 * 思路：基于商品 id 生成稳定的散列值，对同一商品每次都渲染同色
 *   1) 渐变色蒙版覆盖原图 → 同图不同商品「整体色调完全不同」
 *   2) 左上型号角标 MODEL ×××-××× → 文本层差异化
 *   3) 右上身份色点 + 系列文案 → 每个 SKU 有独立"品牌色"
 * ========================================================================== */

// 基础调色板：翡翠绿 A 档品牌色体系内取 12 色（主色×辅色×中性色），永远不脱离 A 档审美
const PALETTE = [
  '#0d9488', '#06b6d4', '#f59e0b', '#14b8a6',
  '#0891b2', '#d97706', '#0f766e', '#0e7490',
  '#b45309', '#059669', '#0284c7', '#ea580c'
]

// 稳定散列：把任意值映射成 0~N-1 的整数，同一输入永远同一输出
const stableHash = (seed, salt = 0) => {
  const s = String(seed) + '#' + String(salt)
  let h = 2166136261
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i)
    h = Math.imul(h, 16777619) >>> 0
  }
  return h
}
const pick = (seed, salt = 0, total = PALETTE.length) => PALETTE[stableHash(seed, salt) % total]

// 1) 蒙版：双色线性渐变 + 40%~55% 不透明度，既保留原图细节，又整体"换色"
const tintStyle = (id) => {
  const c1 = pick(id, 1)
  const c2 = pick(id, 2)
  const deg = (stableHash(id, 3) % 7) * 20 + 110 // 角度 110°~230°
  const opacity = 0.42 + (stableHash(id, 4) % 14) / 100  // 0.42~0.55
  return {
    background: `linear-gradient(${deg}deg, ${hexToRgba(c1, opacity)} 0%, ${hexToRgba(c2, opacity + 0.08)} 100%)`,
    mixBlendMode: (stableHash(id, 5) % 2) ? 'multiply' : 'overlay'
  }
}
const hexToRgba = (hex, alpha) => {
  const h = hex.replace('#', '')
  const r = parseInt(h.substring(0, 2), 16)
  const g = parseInt(h.substring(2, 4), 16)
  const b = parseInt(h.substring(4, 6), 16)
  return `rgba(${r},${g},${b},${alpha})`
}

// 2) 角标样式：边框 & 文字用按 id 的稳定色
const tagStyle = (id, salt) => {
  const c = pick(id, salt + 10)
  return {
    borderColor: hexToRgba(c, 0.35),
    color: '#0f172a',
    background: `linear-gradient(135deg, ${hexToRgba(c, 0.08)}, #ffffffcc)`,
    backdropFilter: 'blur(6px)'
  }
}

// 3) 单一身份色（给小圆点用），只取调色板
const accentColor = (id, salt) => pick(id, salt + 20)

// 4) 型号代码：按分类前缀 + 容量/电流/电压数字 + id 末两位，保证肉眼不一样
const CATEGORY_PREFIX = {
  1: 'LC',   // 锂电池（Lithium Cell）
  2: 'PB',   // 动力电池（Power Battery）
  3: 'ES',   // 储能电池（Energy Storage）
  4: 'BM',   // BMS
  5: 'PV',   // 光伏
  6: 'CG'    // 充电配件（CharginG）
}
const CATEGORY_SERIES = {
  1: ['标准系列', '高倍率', '低温型', '储能级', '启动型', '高镍', 'A品电芯'],
  2: ['CTP标准', '800V高压', '换电标准', '液冷版', '590模组', '刀片式', 'CTB'],
  3: ['户用堆叠', '工商业', '便携户外', '通信基站', '微网离网', '数据中心', '房车版'],
  4: ['动力主从', '储能高压', '入门均衡', 'AFE采集', '无线蓝牙', '继电器集成', '云网关'],
  5: ['单晶', 'N型TOPCon', 'HJT', '柔性组件'],
  6: ['便携充', '超充CCS', '家用交流', 'PD线材', '均衡修复', '双枪直流', '光伏线']
}
const modelCode = (p) => {
  const cid = Number(p.categoryId) || 0
  const prefix = CATEGORY_PREFIX[cid] || 'GL'
  const idPart = String(p.id ?? 0).padStart(3, '0')
  // 从价格或容量里抓一个数字，让代码更像真实型号
  const priceNum = Number(p.price) || 0
  const priceTag = Math.max(1, Math.round(priceNum / (priceNum > 10000 ? 1000 : priceNum > 1000 ? 100 : 10)))
  return `${prefix}-${(priceTag % 900) + 100}${idPart.slice(-2)}`
}
const seriesText = (p) => {
  const cid = Number(p.categoryId) || 0
  const arr = CATEGORY_SERIES[cid] || ['工厂现货']
  return arr[stableHash(p.id, 9) % arr.length]
}

const defaultImage =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400">
      <defs>
        <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#F0FDFA"/>
          <stop offset="1" stop-color="#E0F2FE"/>
        </linearGradient>
      </defs>
      <rect width="100%" height="100%" fill="url(#g)"/>
      <circle cx="200" cy="180" r="68" fill="rgba(13,148,136,.12)"/>
      <path d="M148 180c0 -28 23 -52 52 -52s52 24 52 52 -23 52 -52 52 -52 -24 -52 -52zm-34 78l34 -46 28 30 22 -22 32 38z" fill="#0d9488" opacity=".55"/>
      <text x="200" y="315" fill="#0f766e" font-family="Arial" font-size="20" font-weight="700" text-anchor="middle">绿链锂电 · 工厂直供</text>
      <text x="200" y="345" fill="#0891b2" font-family="Arial" font-size="14" text-anchor="middle">现货48小时内发出</text>
    </svg>`
  )

const loadCategories = async () => {
  try {
    const res = await api.getCategories()
    if (res?.code === 200 && Array.isArray(res.data)) {
      rawCategories.value = (res.data || [])
        .sort((a, b) => (a.sort || 0) - (b.sort || 0))
        .filter((c) => c.status == null || c.status === 1)
        .map((c) => ({ id: c.id, name: c.name, sort: c.sort }))
    }
  } catch (e) {
    console.warn('[ProductSection] 分类加载失败，使用内置分类兜底：', e)
  }
}

const loadProducts = async () => {
  isLoading.value = true
  try {
    // 拉取所有在售商品；支持 keyword 模糊搜索（后端已实现 name + description 匹配）
    const params = { size: 100 }
    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }
    const res = await api.getProducts(params)
    if (res?.code === 200) {
      const list = Array.isArray(res.data) ? res.data : res.data?.records || []
      // 优先按销量倒序（热销榜优先），其次按 ID 新 -> 老
      products.value = list
        .filter((p) => p.status == null || String(p.status) === '1')
        .sort((a, b) => Number(b.sales || 0) - Number(a.sales || 0) || Number(b.id || 0) - Number(a.id || 0))
    }
  } catch (e) {
    console.error('[ProductSection] 商品加载失败：', e)
    products.value = []
  } finally {
    isLoading.value = false
  }
}

/** 点击搜索按钮或按回车触发：调用后端 keyword 模糊搜索 */
const onSearch = () => {
  loadProducts()
}

/** 清除搜索：输入框清空，恢复全部商品 */
const onClearSearch = () => {
  searchKeyword.value = ''
  loadProducts()
}

const goToProductDetail = (id) => {
  router.push(`/product/detail?id=${id}`)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  await loadCategories()
  await loadProducts()
})
</script>

<style scoped>
/* ============ A 档 · 翡翠绿 主题 ============ */
.ps-root {
  --brand: #0d9488;
  --brand-2: #0f766e;
  --cyan: #06b6d4;
  --amber: #f59e0b;
  --slate-50: #F8FAFC;
  --slate-100: #F1F5F9;
  --slate-200: #E2E8F0;
  --slate-400: #94A3B8;
  --slate-500: #64748B;
  --slate-700: #334155;
  --slate-900: #0F172A;

  background: var(--slate-50);
  padding: 72px 0 64px;
}
.ps-inner {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ========= 标题 ========= */
.ps-title-bar { text-align: center; margin-bottom: 36px; }
.ps-title {
  margin: 0 0 10px;
  font-size: 32px;
  font-weight: 700;
  color: var(--slate-900);
  letter-spacing: 0.3px;
}
.ps-title-accent {
  color: var(--brand);
  position: relative;
  padding: 0 6px;
}
.ps-title-accent::after {
  content: '';
  position: absolute;
  left: 4px; right: 4px; bottom: 4px;
  height: 10px;
  background: linear-gradient(90deg, rgba(13,148,136,.18), rgba(6,182,212,.18));
  border-radius: 4px;
  z-index: -1;
}
.ps-sub {
  margin: 0;
  font-size: 14px;
  color: var(--slate-500);
  letter-spacing: 0.2px;
}

/* ========= 搜索栏 ========= */
.ps-search-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 28px;
}
.ps-search-input {
  flex: 1;
  max-width: 560px;
  height: 42px;
  padding: 0 16px;
  border: 1.5px solid var(--slate-200);
  border-radius: 12px;
  background: #ffffff;
  font-size: 14px;
  color: var(--slate-900);
  outline: none;
  transition: border-color .2s ease, box-shadow .2s ease;
  box-shadow: 0 1px 2px rgba(15,23,42,.03);
}
.ps-search-input::placeholder {
  color: var(--slate-400);
}
.ps-search-input:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(13,148,136,.12), 0 2px 8px rgba(13,148,136,.08);
}
.ps-search-btn {
  height: 42px;
  padding: 0 26px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--brand), var(--cyan));
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform .15s ease, box-shadow .2s ease, filter .15s ease;
  box-shadow: 0 4px 14px rgba(13,148,136,.28);
}
.ps-search-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(13,148,136,.38);
  filter: brightness(1.05);
}
.ps-search-btn:active {
  transform: translateY(0);
  filter: brightness(.96);
}
.ps-search-clear {
  height: 32px;
  width: 32px;
  border: 1px solid var(--slate-200);
  border-radius: 50%;
  background: #ffffff;
  color: var(--slate-400);
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .15s ease;
  flex-shrink: 0;
}
.ps-search-clear:hover {
  border-color: #ef4444;
  color: #ef4444;
  background: #fef2f2;
}

/* ========= 筛选 ========= */
.ps-filter-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-bottom: 38px;
}
.ps-filter-btn {
  padding: 9px 22px;
  border: 1px solid var(--slate-200);
  border-radius: 999px;
  background: #ffffff;
  color: var(--slate-500);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all .2s ease;
  box-shadow: 0 1px 2px rgba(15,23,42,.03);
}
.ps-filter-btn:hover {
  border-color: var(--brand);
  color: var(--brand);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(13,148,136,.08);
}
.ps-filter-btn.active {
  background: linear-gradient(135deg, var(--brand), var(--cyan));
  border-color: transparent;
  color: #fff;
  font-weight: 600;
  box-shadow: 0 6px 18px rgba(13,148,136,.28);
}

/* ========= loading / empty ========= */
.ps-empty {
  padding: 80px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: var(--slate-400);
}
.ps-empty-icon { width: 72px; height: 72px; opacity: .6; }
.ps-empty-t1 { font-size: 16px; color: var(--slate-500); margin: 0; }
.ps-empty-t2 { font-size: 13px; margin: 0; }
.spinner {
  width: 40px; height: 40px;
  border: 4px solid rgba(13,148,136,.15);
  border-top-color: var(--brand);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ========= 商品卡片网格 ========= */
.ps-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 28px 24px;
}
.ps-card {
  background: #ffffff;
  border: 1px solid var(--slate-200);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  position: relative;
  transition: transform .25s ease, box-shadow .25s ease, border-color .25s ease;
  box-shadow: 0 4px 14px rgba(15,23,42,.04);
}
.ps-card:hover {
  transform: translateY(-6px);
  border-color: rgba(13,148,136,.25);
  box-shadow: 0 20px 44px rgba(13,148,136,.14);
}

.ps-card-img-wrap {
  aspect-ratio: 1 / 1;
  width: 100%;
  overflow: hidden;
  background: var(--slate-100);
  position: relative;
}
.ps-card-img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform .45s ease;
}
.ps-card:hover .ps-card-img-wrap img {
  transform: scale(1.06);
}

/* ========= 图片差异化蒙版（覆盖在原图上） ========= */
.ps-card-tint {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

/* ========= 两个角标：左上 MODEL + 右上 系列色点 ========= */
.ps-card-tag {
  position: absolute;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border: 1px solid;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
}
.ps-card-tag--cat {
  top: 12px; left: 12px;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 6px 10px;
  line-height: 1.1;
}
.ps-card-tag-label {
  font-size: 9px;
  opacity: 0.55;
  letter-spacing: 0.8px;
}
.ps-card-tag-value {
  font-size: 13px;
  font-weight: 800;
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  letter-spacing: 0.4px;
}
.ps-card-tag--accent {
  top: 12px; right: 12px;
  padding: 5px 10px;
}
.ps-card-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  box-shadow: 0 0 0 2px #ffffff;
  flex-shrink: 0;
}
.ps-card-series {
  font-size: 11px;
  font-weight: 700;
}

.ps-card-badge {
  position: absolute;
  bottom: 12px; left: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.3px;
  backdrop-filter: blur(4px);
  z-index: 2;
}
.ps-card-badge.hot {
  background: linear-gradient(135deg, rgba(245,158,11,.94), rgba(239,68,68,.94));
  color: #fff;
  box-shadow: 0 4px 10px rgba(245,158,11,.3);
}
.ps-card-badge.low {
  top: auto; bottom: 12px; right: 12px; left: auto;
  background: rgba(15,23,42,.65);
  color: #fff;
}

.ps-card-body {
  padding: 18px 18px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}
.ps-card-category {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  align-self: flex-start;
  padding: 3px 9px;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(13,148,136,.08), rgba(6,182,212,.08));
  color: var(--brand-2);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
}
.ps-card-sku-tag {
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
  font-size: 10px;
  font-weight: 700;
  border: 1px dashed rgba(245, 158, 11, 0.4);
}
.ps-card-name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--slate-900);
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: calc(2 * 15px * 1.45);
}
.ps-card-desc {
  margin: 0;
  font-size: 12.5px;
  color: var(--slate-500);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: calc(2 * 12.5px * 1.6);
}
.ps-card-footer {
  margin-top: auto;
  padding-top: 12px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}
.ps-card-price-wrap {
  display: flex;
  align-items: baseline;
  gap: 2px;
  color: var(--brand);
}
.ps-card-price-symbol {
  font-size: 13px;
  font-weight: 600;
  transform: translateY(-1px);
}
.ps-card-price {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0.3px;
  line-height: 1;
}
.ps-card-price-sep {
  font-size: 14px;
  font-weight: 600;
  color: var(--slate-400);
  margin: 0 2px;
}
.ps-card-sales {
  font-size: 12px;
  color: var(--slate-400);
}

.ps-card-hover-btn {
  position: absolute;
  left: 18px; right: 18px; bottom: 16px;
  padding: 10px 16px;
  background: linear-gradient(135deg, var(--brand), var(--cyan));
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  border-radius: 12px;
  opacity: 0;
  transform: translateY(8px);
  transition: all .25s ease;
  pointer-events: none;
  box-shadow: 0 10px 26px rgba(13,148,136,.3);
}
.ps-card:hover .ps-card-hover-btn {
  opacity: 1;
  transform: translateY(-46px);
}

/* ========= 响应式 ========= */
@media (max-width: 1024px) {
  .ps-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px 20px; }
  .ps-title { font-size: 27px; }
  .ps-root { padding: 56px 0 48px; }
}
@media (max-width: 640px) {
  .ps-grid { grid-template-columns: 1fr; gap: 20px; }
  .ps-inner { padding: 0 16px; }
  .ps-title { font-size: 24px; }
  .ps-filter-btn { padding: 8px 18px; font-size: 13px; }
  .ps-search-row { gap: 8px; margin-bottom: 22px; }
  .ps-search-input { height: 38px; font-size: 13px; }
  .ps-search-btn { height: 38px; padding: 0 18px; font-size: 13px; }
}
</style>
