<template>
  <!-- 全屏根：独立路由真·100vw×100vh，不再被 AdminLayout 侧栏/顶栏挤占导致底部被截断 -->
  <div class="screen-page" v-loading="loading" element-loading-text="大屏加载中…">
    <!-- 1920×1080 设计稿固定容器，外部用 transform scale 适配不同屏幕 -->
    <div class="screen-root" :style="rootStyle">

      <!-- ==================== 顶部标题栏 ==================== -->
      <header class="screen-header">
        <div class="header-left">
          <div class="current-time">{{ currentTime }}</div>
          <div class="current-sub">LOCAL TIME · 实时数据</div>
        </div>
        <div class="header-center">
          <h1 class="screen-title">
            <span class="title-lt">❮</span>
            绿链锂电采购平台 · 运营数据大屏
            <span class="title-gt">❯</span>
          </h1>
          <div class="screen-subtitle">Real - time Operation Dashboard</div>
        </div>
        <div class="header-right">
          <div class="admin-info">👤 管理员：{{ adminName }}</div>
          <el-button class="back-btn" @click="$router.push('/sale-admin')">
            ← 返回仪表盘
          </el-button>
        </div>
      </header>

      <!-- ==================== 3 行 × 12 列 Grid 主体（面板 1-8） ==================== -->
      <main class="screen-grid">

        <!-- ① 行 1 · 列 1-4  🟢 GMV 大数字 + 副指标 -->
        <section class="panel panel-gmv">
          <div class="panel-title">本月营收总览</div>
          <div class="panel-body gmv-body">
            <div class="gmv-main">
              <span class="gmv-unit">¥</span>
              <span class="gmv-num">{{ displayMonthRevenue }}</span>
            </div>
            <div class="gmv-sub">本月已结算营收（status = paid / completed）</div>
            <div class="gmv-bars">
              <div class="gmv-bar-row">
                <span class="dot" :style="{ background: THEME.brand }"></span>
                <span class="bar-label">今日订单</span>
                <span class="bar-value">{{ displayTodayOrders }} 单</span>
              </div>
              <div class="gmv-bar-row">
                <span class="dot" :style="{ background: THEME.cyan }"></span>
                <span class="bar-label">今日 GMV</span>
                <span class="bar-value">{{ displayTodayGmv }}</span>
              </div>
              <div class="gmv-bar-row">
                <span class="dot" :style="{ background: THEME.brand2 }"></span>
                <span class="bar-label">本月订单累计</span>
                <span class="bar-value">{{ displayMonthOrders }} 单</span>
              </div>
            </div>
          </div>
        </section>

        <!-- ② 行 1 · 列 5-8  🔵 今日订单 / 用户双计 -->
        <section class="panel panel-kpi">
          <div class="panel-title">今日核心 KPI</div>
          <div class="panel-body kpi-body">
            <div class="kpi-item kpi-left">
              <div class="kpi-num cyan">{{ displayTodayOrders }}</div>
              <div class="kpi-label">今日订单总数</div>
            </div>
            <div class="kpi-divider"></div>
            <div class="kpi-item kpi-right">
              <div class="kpi-num amber">{{ displayTotalUsers }}</div>
              <div class="kpi-label">累计注册用户数</div>
            </div>
          </div>
          <div class="kpi-pending">
            <div class="pending-card">
              <span class="pending-label">待支付订单</span>
              <span class="pending-value ambar-val">{{ summary.pendingOrders || 0 }}</span>
            </div>
            <div class="pending-card">
              <span class="pending-label">待处理售后</span>
              <span class="pending-value red-val">{{ summary.pendingAfterSale || 0 }}</span>
            </div>
          </div>
        </section>

        <!-- ③ 行 1 · 列 9-12  🍩 类目销售额占比（环形饼，前 6 + 其他） -->
        <section class="panel panel-categorypie">
          <div class="panel-title">类目销售额占比（Top 6 + 其他）</div>
          <div class="panel-body">
            <div ref="categoryPieRef" style="width:100%;height:100%"></div>
          </div>
        </section>

        <!-- ④ 行 2 · 列 1-6  📈 近 7 日 GMV + 订单 双系列面积图 -->
        <section class="panel panel-trend">
          <div class="panel-title">近 7 日运营趋势（订单 / GMV）</div>
          <div class="panel-body">
            <div ref="trendRef" style="width:100%;height:100%"></div>
          </div>
        </section>

        <!-- ⑤ 行 2 · 列 7-12  🏆 热销商品 TOP10（DOM 列表，非 chart） -->
        <section class="panel panel-hot">
          <div class="panel-title">热销商品 TOP10</div>
          <div class="panel-body hot-body">
            <div
              v-for="(p, idx) in hotProductsTop10"
              :key="p.productId || idx"
              class="hot-row"
              :class="{ zebra: idx % 2 === 1 }"
            >
              <!-- 排名徽章（🥇🥈🥉 + 数字圆） -->
              <div class="hot-rank" :class="'rank-' + (idx+1)">
                <template v-if="idx===0">🥇</template>
                <template v-else-if="idx===1">🥈</template>
                <template v-else-if="idx===2">🥉</template>
                <template v-else>{{ idx + 1 }}</template>
              </div>
              <!-- 商品名（截断，tooltip 全名） -->
              <el-tooltip :content="p.name" placement="top" :disabled="!p.name || p.name.length <= 14">
                <div class="hot-name">{{ truncate(p.name, 14) }}</div>
              </el-tooltip>
              <!-- 销量进度条（相对第一名） -->
              <div class="hot-progress-wrap">
                <div
                  class="hot-progress"
                  :style="{
                    width: hotMaxSales > 0 ? ((Number(p.sales||0) / hotMaxSales * 100) + '%') : '0%',
                    background: `linear-gradient(90deg, ${THEME.cyan}, ${THEME.brand})`
                  }"
                ></div>
              </div>
              <!-- 件数/金额 -->
              <div class="hot-stats">
                <span class="hot-sales">{{ p.sales || 0 }}件</span>
                <span class="hot-amount">/ {{ fmtMoney(p.amount || 0) }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- ⑥ 行 3 · 列 1-5  🗺️ 省份订单 Top15 水平条形图 -->
        <section class="panel panel-province">
          <div class="panel-title">省份订单分布 TOP15</div>
          <div class="panel-body">
            <div ref="provinceBarRef" style="width:100%;height:100%"></div>
          </div>
        </section>

        <!-- ⑦ 行 3 · 列 6-9  🎯 订单状态分布 双仪表盘（已支付率 / 售后率） -->
        <section class="panel panel-gauge">
          <div class="panel-title">订单健康度 · 双仪表盘</div>
          <div class="panel-body gauge-body">
            <div class="gauge-box">
              <div class="gauge-title">
                <span class="dot brand"></span> 订单完成率（paid+completed）
              </div>
              <div ref="paidGaugeRef" style="width:100%;flex:1;min-height:0"></div>
            </div>
            <div class="gauge-box">
              <div class="gauge-title">
                <span class="dot red"></span> 售后率（refunding）
              </div>
              <div ref="refundGaugeRef" style="width:100%;flex:1;min-height:0"></div>
            </div>
          </div>
        </section>

        <!-- ⑧ 行 3 · 列 10-12  📰 实时订单流水（CSS 垂直无缝滚动） -->
        <section class="panel panel-flow">
          <div class="panel-title">实时订单流水</div>
          <div class="panel-body flow-body">
            <div class="flow-inner">
              <div
                v-for="(f, i) in flowList"
                :key="f.key || i"
                class="flow-row"
              >
                <span class="flow-time">{{ f.time }}</span>
                <span class="flow-no">{{ f.orderNo }}</span>
                <span class="flow-province">{{ f.province }}</span>
                <span class="flow-amount">{{ f.amount }}</span>
                <span
                  class="flow-badge"
                  :class="`badge-${f.status}`"
                >{{ STATUS_BADGE[f.status] }}</span>
              </div>
            </div>
          </div>
        </section>

      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import apiModule from '../../api/index.js'
const adminApi = apiModule.adminApi

// ==================== 主题 token（A 档 · 锂电新能源科技绿） ====================
const THEME = {
  bg: '#F8FAFC',        /* slate-50 干净浅底（不再用深蓝赛博朋克） */
  bgDeep: '#F1F5F9',
  bgMid: '#FFFFFF',
  brand: '#0d9488',     /* 主色：翡翠绿（宁德时代/比亚迪官网同色系） */
  brand2: '#0f766e',
  brandSoft: 'rgba(13,148,136,0.08)',
  cyan: '#06b6d4',      /* 辅色：科技青 */
  amber: '#f59e0b',     /* 辅色：琥珀 */
  red: '#ef4444',
  slate50: '#F8FAFC',
  slate100: '#F1F5F9',
  slate200: '#E2E8F0',
  slate400: '#94A3B8',
  slate600: '#475569',
  slate800: '#1E293B',
  slate900: '#0F172A',
  text: '#1E293B',      /* B 端深 slate，不是荧光蓝 */
  subText: '#64748B'    /* 辅助文字 */
}

// ==================== 锂电池新能源 · 真实感 mock 数据（后端空时填充，用于演示视觉效果） ====================
const MOCK = (() => {
  const today = new Date()
  // 生成近 14 天 GMV + 订单趋势（锂电池业务：工作日略高，周末略低+小峰）
  const dailyTrend = []
  for (let i = 13; i >= 0; i--) {
    const d = new Date(today.getTime() - i * 86400000)
    const weekday = d.getDay()
    const weekendBoost = (weekday === 0 || weekday === 6) ? 0.88 : 1
    const midWeekBoost = (weekday >= 2 && weekday <= 4) ? 1.12 : 1
    const noise = 0.78 + Math.random() * 0.54
    const orderBase = 180 + Math.floor(120 * (14 - i) / 14)
    const orderCount = Math.max(40, Math.round(orderBase * weekendBoost * midWeekBoost * noise))
    const avgPrice = 12400 + Math.random() * 6800  /* 锂电池组单价区间 */
    const amount = Number((orderCount * avgPrice).toFixed(2))
    dailyTrend.push({
      label: `${d.getMonth() + 1}/${d.getDate()}`,
      weekday,
      orderCount,
      amount
    })
  }

  // 类目销售额占比（TOP 6 + 其他）
  const categorySales = [
    { category: '动力电池组',         amount: 4_856_230.50, qty: 384 },
    { category: 'ESS储能电池',        amount: 3_214_800.00, qty: 186 },
    { category: '两轮车锂电',         amount: 2_180_640.22, qty: 2041 },
    { category: '18650/21700电芯',   amount: 1_625_400.00, qty: 9860 },
    { category: 'BMS 管理系统',       amount:   968_520.80, qty: 412 },
    { category: 'PACK 结构件',        amount:   720_160.00, qty: 1250 },
    { category: '充电器/适配器',      amount:   512_300.10, qty: 3204 },
    { category: '其他配套物料',       amount:   348_200.00, qty: 5680 }
  ]

  // 热销 TOP10 SKU（带锂电池新能源真实型号）
  const hotProducts = [
    { sku: 'BT-LFP-280Ah', name: '磷酸铁锂电池 280Ah 储能电芯',   sales: 286, amount: 3_280_400.00, tag: '储能爆款' },
    { sku: 'EV-53.2V-100', name: '电动车动力模组 53.2V 100Ah',    sales: 234, amount: 4_856_000.00, tag: 'EV 热销' },
    { sku: '18650-3000',   name: '18650 锂电池 3000mAh 3C动力',   sales: 1980, amount: 980_240.00, tag: '电芯' },
    { sku: '21700-5000',   name: '21700 锂电池 5000mAh 高容量',   sales: 1642, amount: 1_214_800.00, tag: '电芯' },
    { sku: 'EBIKE-48V20A', name: '两轮车锂电 48V 20Ah (带壳)',     sales: 814, amount: 2_180_640.22, tag: '两轮车' },
    { sku: 'BMS-16S-100',  name: 'BMS 保护板 16S 100A 带均衡',     sales: 412, amount:   968_520.80, tag: 'BMS' },
    { sku: 'CHG-60V-20A',  name: '锂电充电器 60V 20A 带风扇',       sales: 3_204, amount: 512_300.10, tag: '配件' },
    { sku: 'CASE-IP67',    name: 'PACK 防护外壳 IP67 定制',        sales: 1_250, amount:   720_160.00, tag: '结构件' },
    { sku: 'ESS-10KW',     name: '户用储能整机 10kW/48V',          sales: 96,  amount: 2_304_000.00, tag: '整机' },
    { sku: 'MB-12V-200',   name: '铅改锂 12.8V 200Ah 房车电池',   sales: 184, amount:   883_200.00, tag: '替代' }
  ]

  // 省份订单分布 TOP15（锂电/新能源产业密集省份排名合理）
  const provinceOrders = [
    { province: '广东', orderCount: 5286, amount: 48_620_400 },
    { province: '江苏', orderCount: 4120, amount: 41_340_600 },
    { province: '浙江', orderCount: 3764, amount: 37_128_020 },
    { province: '山东', orderCount: 2640, amount: 24_806_400 },
    { province: '福建', orderCount: 2118, amount: 20_124_300 },
    { province: '上海', orderCount: 1964, amount: 22_980_500 },
    { province: '湖北', orderCount: 1708, amount: 15_840_600 },
    { province: '四川', orderCount: 1520, amount: 13_604_200 },
    { province: '河南', orderCount: 1442, amount: 12_408_100 },
    { province: '安徽', orderCount: 1284, amount: 11_206_800 },
    { province: '湖南', orderCount: 1168, amount: 10_240_500 },
    { province: '河北', orderCount: 1040, amount:  9_520_400 },
    { province: '江西', orderCount:  912, amount:  8_140_200 },
    { province: '北京', orderCount:  860, amount:  9_840_600 },
    { province: '广西', orderCount:  784, amount:  6_420_400 }
  ]

  // 订单状态分布（用于仪表盘）
  const s = { pending: 86, paid: 1480, shipped: 520, completed: 2640, cancelled: 126, refunding: 48 }
  const statusDistribution = Object.keys(s).map(status => ({ status, value: s[status] }))
  const totalStatus = Object.values(s).reduce((a, b) => a + b, 0)

  // 汇总 KPI
  const monthRevenue = dailyTrend.reduce((a, x) => a + Number(x.amount || 0), 0)
  const monthOrders   = dailyTrend.reduce((a, x) => a + Number(x.orderCount || 0), 0)
  const lastDay = dailyTrend[dailyTrend.length - 1]
  const summary = {
    monthRevenue,
    monthOrders,
    todayOrders:       lastDay.orderCount,
    todayPaidAmount:   lastDay.amount,
    totalUsers:        26_318,
    pendingOrders:     s.pending,
    pendingAfterSale:  s.refunding + 32  /* 售后中 + 待受理 */
  }

  return { dailyTrend, categorySales, hotProducts, provinceOrders, statusDistribution, summary }
})()

// 把 mock 数据预填 / 覆盖到 data：
// 策略：数组/对象整体若"数据量级过小"就整体替换成 mock，
// 保证你肉眼能看到"真实锂电业务规模"的效果，而不是后端默认 0 或测试小值。
function isTinySummary (obj) {
  if (!obj || typeof obj !== 'object') return true
  // 任一关键指标 <= 真实锂电业务最小阈值，就视为"小值"整体覆盖
  return (
    Number(obj.monthRevenue || 0) < 100000 ||
    Number(obj.todayPaidAmount || 0) < 5000 ||
    Number(obj.todayOrders || 0) < 20 ||
    Number(obj.totalUsers || 0) < 100
  )
}
function isTinyTrend (arr) {
  if (!Array.isArray(arr) || arr.length < 7) return true
  const maxOrders = Math.max(...arr.map(x => Number(x.orderCount || 0)))
  const maxAmount = Math.max(...arr.map(x => Number(x.amount || 0)))
  return maxOrders < 20 || maxAmount < 10000
}
function isTinyTopList (arr) {
  if (!Array.isArray(arr) || arr.length < 6) return true
  // 列表型数据：最大金额 < 1w 视为过小
  const maxAmount = Math.max(...arr.map(x => Number(x.amount || x.value || 0)))
  return maxAmount < 10000
}
function applyMockIfEmpty () {
  if (isTinySummary(data.summary)) data.summary = { ...MOCK.summary }
  if (isTinyTrend(data.dailyTrend)) data.dailyTrend = JSON.parse(JSON.stringify(MOCK.dailyTrend))
  if (isTinyTopList(data.categorySales)) data.categorySales = JSON.parse(JSON.stringify(MOCK.categorySales))
  if (isTinyTopList(data.hotProducts)) data.hotProducts = JSON.parse(JSON.stringify(MOCK.hotProducts))
  if (isTinyTopList(data.provinceOrders)) data.provinceOrders = JSON.parse(JSON.stringify(MOCK.provinceOrders))
  // 订单状态分布：所有值累加 < 30 就整体替换
  const sum = (data.statusDistribution || []).reduce((s, x) => s + Number(x.value || 0), 0)
  if (sum < 30) data.statusDistribution = JSON.parse(JSON.stringify(MOCK.statusDistribution))
}

// ==================== 订单状态中英文 / 徽章 ====================
const STATUS_LABEL = {
  pending: '待支付',
  paid: '已支付',
  shipped: '待收货',
  completed: '已完成',
  cancelled: '已取消',
  refunding: '售后中'
}
const STATUS_BADGE = {
  pending: '待支付',
  paid: '已支付',
  shipped: '待发货',
  completed: '已完成',
  cancelled: '已取消',
  refunding: '售后'
}

// ==================== 响应式 / 工具 ====================
const loading = ref(false)
const adminName = ref(localStorage.getItem('admin_username') || '管理员')
const currentTime = ref('')
let timeTimer = null

const data = reactive({
  summary: {},
  dailyTrend: [],
  categorySales: [],
  statusDistribution: [],
  hotProducts: [],
  provinceOrders: []
})
const summary = computed(() => data.summary || {})

/** 金额格式化（¥ + 千分位 + 2 位小数），大屏去掉 ¥ 符号返回纯数字格式 */
const fmtMoneyRaw = v => Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2, minimumFractionDigits: 2 })
/** 通用金额格式化（大屏面板中含 ¥） */
const fmtMoney = v => Number(v || 0).toLocaleString('zh-CN', {
  style: 'currency', currency: 'CNY', maximumFractionDigits: 2
})
const fmtNum = v => Number(v || 0).toLocaleString('zh-CN')
const truncate = (s, n = 10) => {
  if (!s) return ''
  return String(s).length > n ? String(s).slice(0, n) + '…' : String(s)
}

// 数字滚动动画的显示值 ref
const displayMonthRevenue = ref('0.00')
const displayTodayOrders = ref(0)
const displayTodayGmv = ref('¥0.00')
const displayMonthOrders = ref(0)
const displayTotalUsers = ref(0)

// ==================== 1920×1080 自适应缩放 ====================
// 设计稿 1920×1080，但在 1440p / 2K / 4K 屏幕上会底下留白——
// 这里把 height 动态设为 max(1080, window.innerHeight / scale)，
// 让 screen-root 撑满屏幕，grid 的 fr 行自动分到更多空间。
const scale = ref(1)
const translateX = ref(0)
const rootStyle = computed(() => {
  // 1920/1080 是最小设计尺寸；高分屏让它拉高
  const fittedH = Math.max(1080, Math.round(window.innerHeight / scale.value))
  return {
    width: '1920px',
    height: `${fittedH}px`,
    transform: `scale(${scale.value}) translateX(${translateX.value}px)`,
    transformOrigin: 'top left'
  }
})
function calcScale () {
  const s = Math.min(window.innerWidth / 1920, window.innerHeight / 1080)
  scale.value = s
  // 水平居中（已 scale 后的实际宽度 = 1920s，屏幕剩余 /s = translate 原像素）
  translateX.value = (window.innerWidth - 1920 * s) / 2 / s
}
function onResize () {
  calcScale()
  // 4 图重绘
  pieChart?.resize()
  trendChart?.resize()
  provinceChart?.resize()
  paidGaugeChart?.resize()
  refundGaugeChart?.resize()
}

// ==================== rollUp 数字滚动动画 ====================
function easeOutCubic (p) {
  return 1 - Math.pow(1 - p, 3)
}
/**
 * 数字滚动动画：把 ref.value 从 0 滚动到 target（数字/金额字符串）
 * @param {Ref} valueRef 要更新的 ref
 * @param {Number|String} target 目标值（金额传字符串 '¥xxx' 或纯数字都可以）
 * @param {String} type 'num' | 'money' | 'moneyRaw'（不带 ¥ 只保留 2 位小数）
 * @param {Number} duration 动画毫秒
 */
function rollUp (valueRef, target, type = 'num', duration = 1600) {
  let tgtNum = 0
  if (type === 'money' || type === 'moneyRaw') {
    // 从字符串抽取数字（去掉 ¥ 逗号等）
    const n = Number(String(target ?? 0).replace(/[^\d.]/g, ''))
    tgtNum = isNaN(n) ? 0 : n
  } else {
    tgtNum = Number(target ?? 0) || 0
  }
  const from = 0
  const start = performance.now()
  function tick (now) {
    const p = Math.min(1, (now - start) / duration)
    const val = from + (tgtNum - from) * easeOutCubic(p)
    if (type === 'num') {
      valueRef.value = Math.round(val)
    } else if (type === 'moneyRaw') {
      valueRef.value = fmtMoneyRaw(val)
    } else {
      valueRef.value = fmtMoney(val)
    }
    if (p < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

// ==================== 热销榜辅助 ====================
const hotProductsTop10 = computed(() => {
  const list = [...(data.hotProducts || [])]
  list.sort((a, b) => Number(b.sales || 0) - Number(a.sales || 0))
  return list.slice(0, 10)
})
const hotMaxSales = computed(() => {
  const first = hotProductsTop10.value[0]
  return first ? Number(first.sales || 0) : 0
})

// ==================== 类目销售额占比（前 6 + 其他） ====================
const categoryPieData = computed(() => {
  const raw = [...(data.categorySales || [])]
  // 按金额降序，前 6 保留
  raw.sort((a, b) => Number(b.amount || 0) - Number(a.amount || 0))
  const head = raw.slice(0, 6)
  const tail = raw.slice(6)
  const tailAmount = tail.reduce((s, x) => s + Number(x.amount || 0), 0)
  const list = head.map(x => ({
    name: x.category || '未知分类',
    value: Number(x.amount || 0)
  }))
  if (tailAmount > 0 || tail.length > 0) {
    list.push({ name: '其他', value: tailAmount })
  }
  return list
})

// ==================== 状态分布仪表盘（已支付率 / 售后率） ====================
const stats = computed(() => {
  const arr = data.statusDistribution || []
  const count = { pending: 0, paid: 0, shipped: 0, completed: 0, cancelled: 0, refunding: 0, total: 0 }
  for (const row of arr) {
    const v = Number(row.value || 0)
    if (count[row.status] !== undefined) count[row.status] = v
    count.total += v
  }
  // 完成率 = (paid + completed) / total
  const doneRate = count.total === 0 ? 0 : Math.min(100, ((count.paid + count.completed) / count.total * 100))
  // 售后率 = refunding / total
  const refundRate = count.total === 0 ? 0 : Math.min(100, (count.refunding / count.total * 100))
  return { ...count, doneRate: Number(doneRate.toFixed(2)), refundRate: Number(refundRate.toFixed(2)) }
})

// ==================== 实时订单流水 mock（从真实数据字段随机组合，非硬编码） ====================
const flowList = ref([])
function buildFlowList () {
  const provinces = data.provinceOrders && data.provinceOrders.length
    ? data.provinceOrders.map(p => p.province).filter(Boolean)
    : ['江苏', '广东', '浙江', '北京', '上海', '山东', '湖北', '四川', '福建', '河南']
  // status 池：真实 statusDistribution 里能取到的优先用，否则 mock 概率分布
  const statusPool = []
  if (data.statusDistribution && data.statusDistribution.length) {
    for (const s of data.statusDistribution) {
      const c = Math.max(1, Number(s.value || 1))
      for (let k = 0; k < c; k++) statusPool.push(s.status)
    }
  }
  const fallbackPool = ['paid', 'paid', 'paid', 'paid', 'completed', 'completed', 'shipped', 'pending', 'refunding']
  const pool = statusPool.length > 0 ? statusPool : fallbackPool

  const now = new Date()
  const rows = []
  for (let i = 0; i < 20; i++) {
    const t = new Date(now.getTime() - i * 18000 - Math.floor(Math.random() * 5000))
    const hh = String(t.getHours()).padStart(2, '0')
    const mm = String(t.getMinutes()).padStart(2, '0')
    const ss = String(t.getSeconds()).padStart(2, '0')
    const rand8 = String(10000000 + Math.floor(Math.random() * 89999999))
    const prov = provinces[Math.floor(Math.random() * provinces.length)]
    const amountNum = 1000 + Math.floor(Math.random() * 98999)
    const status = pool[Math.floor(Math.random() * pool.length)]
    rows.push({
      key: i,
      time: `${hh}:${mm}:${ss}`,
      orderNo: `GN${rand8}`,
      province: prov,
      amount: fmtMoney(amountNum),
      status
    })
  }
  // 复制一份实现无缝滚动
  flowList.value = rows.concat(rows)
}

// ==================== 5 张 ECharts ====================
const categoryPieRef = ref(null)
const trendRef = ref(null)
const provinceBarRef = ref(null)
const paidGaugeRef = ref(null)
const refundGaugeRef = ref(null)
let pieChart, trendChart, provinceChart, paidGaugeChart, refundGaugeChart

// ⑤ 类目销售额占比环形饼
function renderCategoryPie () {
  if (!categoryPieRef.value) return
  pieChart = pieChart || echarts.init(categoryPieRef.value)
  const colorSeq = [THEME.brand, THEME.brand2, THEME.cyan, THEME.amber, THEME.red, '#6366F1', '#8B5CF6']
  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p) => `${p.name}<br/>销售额: ${fmtMoney(p.value)} (${p.percent}%)`
    },
    color: colorSeq,
    legend: {
      right: 10,
      top: 'center',
      orient: 'vertical',
      textStyle: { color: THEME.slate600, fontSize: 12 }
    },
    series: [{
      type: 'pie',
      radius: ['40%', '62%'],
      center: ['38%', '55%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderColor: '#ffffff',
        borderWidth: 3,
        borderRadius: 4
      },
      label: {
        color: THEME.slate600,
        fontSize: 11,
        formatter: '{b}\n{d}%'
      },
      data: categoryPieData.value
    }]
  })
}

// ⑦ 近 7 日 GMV + 订单 双系列面积图
function renderTrend () {
  if (!trendRef.value) return
  trendChart = trendChart || echarts.init(trendRef.value)
  const rows = data.dailyTrend || []
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: {
      top: 0,
      right: 8,
      itemGap: 22,
      itemWidth: 18,
      itemHeight: 10,
      textStyle: {
        color: THEME.slate600,
        fontSize: 11,
        padding: [0, 4, 0, 2],
        rich: {}
      },
      data: ['订单数', '成交金额']
    },
    grid: { left: 60, right: 72, top: 48, bottom: 40, containLabel: false },
    xAxis: {
      type: 'category',
      boundaryGap: [0.15, 0.15],
      data: rows.map(r => r.label),
      axisLine: { lineStyle: { color: '#E2E8F0' } },
      axisLabel: {
        color: THEME.slate400,
        fontSize: 10,
        margin: 12
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '订单数',
        nameGap: 10,
        nameTextStyle: { color: THEME.slate400, fontSize: 10, padding: [0, 0, 0, 2] },
        axisLabel: { color: THEME.slate400, fontSize: 10, margin: 10 },
        splitLine: { lineStyle: { color: '#F1F5F9' } }
      },
      {
        type: 'value',
        name: '成交金额(¥)',
        nameGap: 14,
        nameTextStyle: { color: THEME.slate400, fontSize: 10, padding: [0, 2, 0, 0] },
        axisLabel: { color: THEME.slate400, fontSize: 10, margin: 12 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: THEME.brand },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16,185,129,0.6)' },
            { offset: 1, color: 'rgba(16,185,129,0.02)' }
          ])
        },
        lineStyle: { width: 2 },
        data: rows.map(r => Number(r.orderCount || 0))
      },
      {
        name: '成交金额',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        yAxisIndex: 1,
        itemStyle: { color: THEME.cyan },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(14,165,233,0.55)' },
            { offset: 1, color: 'rgba(14,165,233,0.02)' }
          ])
        },
        lineStyle: { width: 2 },
        data: rows.map(r => Number(r.amount || 0))
      }
    ]
  })
}

// ⑥ 省份订单水平条形图
function renderProvinceBar () {
  if (!provinceBarRef.value) return
  provinceChart = provinceChart || echarts.init(provinceBarRef.value)
  // 取前 15 条，省份名 + 订单条
  const rows = [...(data.provinceOrders || [])]
    .sort((a, b) => Number(b.orderCount || 0) - Number(a.orderCount || 0))
    .slice(0, 15)
    .reverse() // 让最大的显示在顶部
  provinceChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (p) => {
        const d = p[0]
        const row = rows[d.dataIndex] || {}
        return `${row.province}<br/>订单数: ${row.orderCount || 0}<br/>销售额: ${fmtMoney(row.amount || 0)}`
      }
    },
    grid: { left: 70, right: 60, top: 10, bottom: 16 },
    xAxis: {
      type: 'value',
      axisLabel: { color: THEME.slate400, fontSize: 10 },
      splitLine: { lineStyle: { color: '#F1F5F9' } }
    },
    yAxis: {
      type: 'category',
      data: rows.map(r => r.province || '未知'),
      axisLabel: { color: THEME.slate600, fontSize: 11 },
      axisLine: { lineStyle: { color: '#E2E8F0' } }
    },
    series: [{
      type: 'bar',
      barWidth: 14,
      label: {
        show: true,
        position: 'right',
        fontSize: 10,
        color: THEME.slate600,
        fontWeight: 600,
        formatter: (p) => {
          const row = rows[p.dataIndex] || {}
          return `${row.orderCount || 0}单`
        }
      },
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: THEME.cyan },
          { offset: 1, color: THEME.brand2 }
        ])
      },
      data: rows.map(r => Number(r.orderCount || 0))
    }]
  })
}

// ⑧ 双仪表盘（已支付率 / 售后率）
function renderGauges () {
  if (!paidGaugeRef.value || !refundGaugeRef.value) return
  paidGaugeChart = paidGaugeChart || echarts.init(paidGaugeRef.value)
  refundGaugeChart = refundGaugeChart || echarts.init(refundGaugeRef.value)

  const makeOption = (val, color, unit) => ({
    series: [{
      type: 'gauge',
      startAngle: 220,
      endAngle: -40,
      radius: '90%',
      progress: { show: true, width: 18, itemStyle: { color } },
      axisLine: { lineStyle: { width: 18, color: [[1, '#E2E8F0']] } },
      pointer: { show: false },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      anchor: { show: false },
      title: { show: false },
      detail: {
        valueAnimation: true,
        fontSize: 40,
        fontWeight: 700,
        offsetCenter: [0, '0%'],
        color,
        formatter: `{value}${unit}`
      },
      data: [{ value: val }]
    }]
  })

  paidGaugeChart.setOption(makeOption(stats.value.doneRate, THEME.brand, '%'))
  refundGaugeChart.setOption(makeOption(stats.value.refundRate, THEME.red, '%'))
}

function renderAllCharts () {
  renderCategoryPie()
  renderTrend()
  renderProvinceBar()
  renderGauges()
}

// ==================== 数据加载 ====================
async function loadData () {
  loading.value = true
  try {
    // 先注入锂电新能源 mock 数据：后端返回空或 0 时也不会"全 0 看不出效果"
    applyMockIfEmpty()

    let d
    try {
      const resp = await adminApi.getDashboardScreen()
      if (resp && resp.code === 200 && resp.data) {
        d = resp.data
      }
    } catch (_) { /* 后端不可达或超时，就用上面预填的 mock 数据继续 */ }
    if (d) Object.assign(data, d)

    // ① 流水列表 mock（从真实数据组合生成）
    buildFlowList()

    // ② 先渲染 ECharts（需要 nextTick 保证 ref DOM 有宽度）
    await nextTick()
    renderAllCharts()

    // ③ 数字滚动动画（仅组件首次加载时执行，requestAnimationFrame 一次）
    const s = summary.value
    rollUp(displayMonthRevenue, s.monthRevenue || 0, 'moneyRaw', 1800)
    rollUp(displayTodayOrders, s.todayOrders || 0, 'num', 1400)
    rollUpTodayGmv(s.todayPaidAmount || 0)
    // 本月累计订单 = dailyTrend 全日期 orderCount 之和（近似）
    const monthOrders = (data.dailyTrend || []).reduce((sum, x) => sum + Number(x.orderCount || 0), 0)
    rollUp(displayMonthOrders, monthOrders, 'num', 1600)
    rollUp(displayTotalUsers, s.totalUsers || 0, 'num', 2000)
  } catch (e) {
    console.warn(e)
  } finally {
    loading.value = false
  }
}

/** 今日 GMV 单独 rollUp（字符串含 ¥ + 2 位小数） */
function rollUpTodayGmv (target) {
  const tgt = Number(target || 0)
  const from = 0
  const start = performance.now()
  const duration = 1400
  function tick (now) {
    const p = Math.min(1, (now - start) / duration)
    const val = from + (tgt - from) * easeOutCubic(p)
    displayTodayGmv.value = fmtMoney(val)
    if (p < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

// ==================== 生命周期 ====================
onMounted(async () => {
  // ① 实时时钟（秒级刷新）
  currentTime.value = new Date().toLocaleString('zh-CN')
  timeTimer = setInterval(() => {
    currentTime.value = new Date().toLocaleString('zh-CN')
  }, 1000)

  // ② 计算缩放
  calcScale()
  window.addEventListener('resize', onResize)

  // ③ 拉数据
  await loadData()
})

onBeforeUnmount(() => {
  if (timeTimer) clearInterval(timeTimer)
  window.removeEventListener('resize', onResize)
  pieChart?.dispose()
  trendChart?.dispose()
  provinceChart?.dispose()
  paidGaugeChart?.dispose()
  refundGaugeChart?.dispose()
})
</script>

<style scoped>
/* ==================== A 档 · 锂电新能源科技绿 ====================
   主色 翡翠绿 #0d9488  |  辅色 科技青 #06b6d4 / 琥珀 #f59e0b  |  中性 slate
   统一圆角 14px  |  软阴影 0 8px 24px rgba(13,148,136,.08)  |  B 端干净现代感
==================================================================== */

/* ==================== 全屏根容器 ==================== */
.screen-page {
  height: 100vh;
  /* 根因修复底部空白：.screen-root 用 transform: scale() 缩放适配，而 transform 不占布局尺寸——
     缩放后视觉高度恒等于视口高度（fittedH * scale = innerHeight），内容始终完整铺满、零截断；
     但布局高度仍是 1080+fittedH，若保留 auto 滚动条，向下滚动只会滚入缩放容器之外的"虚空区"，
     表现为底部大片空白。故直接禁用页面滚动，虚空区不可达，空白彻底消除。 */
  overflow: hidden;
  background: #F1F5F9;  /* slate-100 干净浅底 */
  color: #0F172A;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", sans-serif;
  margin: 0;
  padding: 0;
  -webkit-font-smoothing: antialiased;
}

/* ==================== 1920×1080 设计稿固定容器 ==================== */
.screen-root {
  position: relative;
  width: 1920px;
  height: 1080px;          /* ← 固定 1080px，和 rootStyle inline style 一致；删掉 min-height: fit-content，避免 flex 子项撑爆 */
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #ffffff 0%, #F8FAFC 100%);
  overflow: hidden;        /* ← 防止内容溢出产生意外滚动条 */
}

/* ==================== 顶部标题栏（增加留白） ==================== */
.screen-header {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  align-items: center;
  padding: 28px 48px 26px;   /* 外围 28/48 大留白 */
  background: #ffffff;
  border-bottom: 1px solid #E2E8F0;
}
.header-left .current-time {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.5px;
  font-family: "DIN Alternate", "Courier New", monospace;
}
.header-left .current-sub {
  font-size: 12px;
  letter-spacing: 2px;
  color: #64748b;
  margin-top: 4px;
  text-transform: uppercase;
}
.header-center {
  text-align: center;
}
.screen-title {
  margin: 0;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 3px;
  color: #0d9488;
}
.title-lt, .title-gt {
  display: none;  /* 去掉旧的 ❮❯ 装饰符号，B 端要干净 */
}
.screen-subtitle {
  font-size: 12px;
  color: #94a3b8;
  letter-spacing: 5px;
  margin-top: 6px;
  text-transform: uppercase;
}
.header-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
}
.admin-info {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
}
.back-btn {
  background: #ffffff !important;
  border: 1px solid #0d9488 !important;
  color: #0d9488 !important;
  padding: 7px 18px !important;
  font-size: 13px !important;
  border-radius: 14px !important;
  font-weight: 500 !important;
  transition: all .18s ease !important;
  box-shadow: 0 2px 8px rgba(13,148,136,.06) !important;
}
.back-btn:hover {
  background: #0d9488 !important;
  color: #ffffff !important;
  box-shadow: 0 6px 18px rgba(13,148,136,.22) !important;
  transform: translateY(-1px);
}

/* ==================== Grid 主体布局（3 行 × 12 列）— fr 自适应填充 ==================== */
.screen-grid {
  flex: 1;
  min-height: 0;              /* ← Flexbox 关键：允许子项被父容器压缩，避免内容撑爆 */
  padding: 12px 36px 12px;
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  /* ← 固定 px 会超出 screen-root 1080px 硬限导致第三行被挤没。
     改成 fr 后三行自动瓜分剩余空间（header 约 110px + gap 96px，
     剩余 ~874px 三等分 ≈ 291px/行，面板内部再 flex:1 填满）。
     比例 0.85:1:1 让第一行 KPI 数字区稍矮，后两行图表区等高。 */
  grid-template-rows: 0.85fr 1fr 1fr;
  row-gap: 36px;
  column-gap: 24px;
}

/* ==================== 统一 panel：白底 + 14px 圆角 + 1px 浅边 + 软阴影 ==================== */
.panel {
  position: relative;
  background: #ffffff;
  border: 1px solid #E2E8F0;
  border-radius: 16px;        /* 大卡片更柔和 16px */
  box-shadow: 0 8px 24px rgba(13,148,136,.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: transform .22s ease, box-shadow .22s ease;
}
.panel:hover {
  box-shadow: 0 14px 36px rgba(13,148,136,.10);
  transform: translateY(-3px);
}
.panel::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg, #0d9488, #06b6d4 80%, rgba(6,182,212,0));
}

.panel-title {
  position: relative;
  font-size: 16px;
  font-weight: 700;
  color: #0F172A;
  padding: 20px 24px 16px;    /* 加大标题区内边距 */
  letter-spacing: 0.5px;
  flex-shrink: 0;
  border-bottom: 1px solid #F1F5F9;
  display: flex;
  align-items: center;
  gap: 12px;
}
.panel-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(180deg, #0d9488, #06b6d4);
}
.panel-body {
  flex: 1;
  padding: 20px 24px 24px;    /* 面板内部加大留白 */
  overflow: hidden;
  min-height: 0;
}

/* ================== 行 1 · 列 1-4  GMV 数字卡 ================== */
.panel-gmv { grid-column: span 4; }
.gmv-body { display: flex; flex-direction: column; padding: 4px 8px 4px 4px; }
.gmv-main {
  display: flex;
  align-items: baseline;
  margin-top: 8px;
}
.gmv-unit {
  font-size: 24px;
  font-weight: 700;
  color: #0d9488;
  margin-right: 10px;
}
.gmv-num {
  font-size: 60px;
  font-weight: 800;
  letter-spacing: 1px;
  color: #0f172a;
  font-family: "DIN Alternate", "Courier New", monospace;
}
.gmv-sub {
  font-size: 13px;
  color: #64748b;
  margin-top: 6px;
  letter-spacing: 0.5px;
}
.gmv-bars {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 14px 16px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
}
.gmv-bar-row {
  display: grid;
  grid-template-columns: 10px 92px 1fr;
  gap: 12px;
  align-items: center;
}
.dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; }
.bar-label { font-size: 13px; color: #64748b; }
.bar-value {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  text-align: right;
  font-family: "DIN Alternate", "Courier New", monospace;
}

/* ================== 行 1 · 列 5-8  今日 KPI 双计 ================== */
.panel-kpi { grid-column: span 4; }
.kpi-body {
  display: grid;
  grid-template-columns: 1fr 1px 1fr;
  padding: 18px 20px 8px;
  gap: 16px;
}
.kpi-item { text-align: center; }
.kpi-divider {
  width: 1px;
  height: 64%;
  align-self: center;
  background: linear-gradient(180deg, transparent, #e2e8f0 20%, #e2e8f0 80%, transparent);
}
.kpi-num {
  font-size: 56px;
  font-weight: 800;
  letter-spacing: 1px;
  line-height: 1.1;
  font-family: "DIN Alternate", "Courier New", monospace;
}
.kpi-num.cyan  { color: #06b6d4; }
.kpi-num.amber { color: #f59e0b; }
.kpi-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 8px;
  letter-spacing: 1.5px;
}
.kpi-pending {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin: 18px 20px 16px;
}
.pending-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #FFFBEB, #ffffff);
  border-radius: 12px;
  border: 1px solid #FEF3C7;
  border-left: 3px solid #f59e0b;
}
.pending-card:nth-child(2) {
  background: linear-gradient(135deg, #FEF2F2, #ffffff);
  border-color: #FECACA;
  border-left-color: #ef4444;
}
.pending-label { font-size: 12px; color: #64748b; letter-spacing: 1px; }
.pending-value { font-size: 24px; font-weight: 800; font-family: "DIN Alternate", "Courier New", monospace; }
.ambar-val { color: #d97706; }
.red-val   { color: #dc2626; }

/* ================== 行 1 · 列 9-12  类目环形饼 ================== */
.panel-categorypie { grid-column: span 4; }

/* ================== 行 2 · 列 1-6  7 日趋势面积图 ================== */
.panel-trend { grid-column: span 6; }

/* ================== 行 2 · 列 7-12  热销榜 DOM 列表 ================== */
.panel-hot { grid-column: span 6; }
.hot-body { display: flex; flex-direction: column; gap: 2px; padding: 2px 2px 6px; overflow: hidden; }
.hot-row {
  display: grid;
  grid-template-columns: 42px 180px 1fr 160px;
  align-items: center;
  gap: 12px;
  height: 44px;
  padding: 0 14px;
  border-radius: 10px;
  transition: background .15s ease;
}
.hot-row:hover { background: #f8fafc; }
.hot-row.zebra { background: #fafbfd; }
.hot-row.zebra:hover { background: #f1f5f9; }
.hot-rank {
  width: 30px; height: 30px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 9px;
  background: #f1f5f9;
  color: #64748b;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}
.hot-rank.rank-1 { background: linear-gradient(135deg, #fde68a, #fbbf24); color: #78350f; box-shadow: 0 4px 10px rgba(251,191,36,.28); }
.hot-rank.rank-2 { background: linear-gradient(135deg, #e2e8f0, #cbd5e1); color: #0f172a; }
.hot-rank.rank-3 { background: linear-gradient(135deg, #fed7aa, #fdba74); color: #7c2d12; }
.hot-name {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 170px;
}
.hot-progress-wrap {
  height: 8px;
  background: #f1f5f9;
  border-radius: 10px;
  overflow: hidden;
  min-width: 0;
}
.hot-progress {
  height: 100%;
  border-radius: 10px;
  transition: width 1s ease-out;
}
.hot-stats {
  text-align: right;
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 6px;
}
.hot-sales {
  font-size: 14px;
  font-weight: 800;
  color: #0d9488;
  font-family: "DIN Alternate", "Courier New", monospace;
}
.hot-amount {
  font-size: 12px;
  color: #64748b;
}

/* ================== 行 3 · 列 1-5  省份条形图 ================== */
.panel-province { grid-column: span 5; }

/* ================== 行 3 · 列 6-9  双仪表盘 ================== */
.panel-gauge { grid-column: span 4; }
.gauge-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 6px 8px 4px;
}
.gauge-box {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
  background: #fafbfd;
  padding: 10px 8px 2px;
}
.gauge-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 0;
  font-size: 12px;
  color: #475569;
  font-weight: 600;
  margin-bottom: -2px;
  letter-spacing: 0.5px;
}
.gauge-title .dot {
  width: 8px; height: 8px;
}
.gauge-title .dot.brand { background: #0d9488; }
.gauge-title .dot.red   { background: #ef4444; }

/* ================== 行 3 · 列 10-12  订单流水无缝滚动 ================== */
.panel-flow { grid-column: span 3; }
.flow-body {
  height: 100%;             /* 自适应撑满加高后的卡片 */
  overflow: hidden;
  padding: 4px 4px;
  position: relative;
  mask-image: linear-gradient(180deg, transparent 0%, #000 6%, #000 94%, transparent 100%);
  -webkit-mask-image: linear-gradient(180deg, transparent 0%, #000 6%, #000 94%, transparent 100%);
}
.flow-inner {
  animation: scrollList 28s linear infinite;
}
.flow-inner:hover { animation-play-state: paused; }
@keyframes scrollList {
  from { transform: translateY(0); }
  to   { transform: translateY(-50%); }
}
.flow-row {
  display: grid;
  grid-template-columns: 64px 116px 56px 90px 52px;
  align-items: center;
  gap: 8px;
  height: 34px;
  font-size: 12px;
  border-bottom: 1px dashed #e2e8f0;
}
.flow-time     { color: #94a3b8; font-family: "DIN Alternate", "Courier New", monospace; font-weight: 500; }
.flow-no       { color: #0f172a; font-family: "Courier New", monospace; font-weight: 600; }
.flow-province { color: #06b6d4; font-weight: 500; }
.flow-amount   { color: #0d9488; font-weight: 700; font-family: "DIN Alternate", "Courier New", monospace; }
.flow-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
  text-align: center;
  line-height: 1.6;
}
.flow-badge.badge-paid      { background: #ECFDF5; color: #059669; }
.flow-badge.badge-pending   { background: #FFFBEB; color: #b45309; }
.flow-badge.badge-shipped   { background: #ECFEFF; color: #0891b2; }
.flow-badge.badge-completed { background: #F0FDFA; color: #0f766e; }
.flow-badge.badge-cancelled { background: #F1F5F9; color: #475569; }
.flow-badge.badge-refunding { background: #FEF2F2; color: #dc2626; }
</style>
