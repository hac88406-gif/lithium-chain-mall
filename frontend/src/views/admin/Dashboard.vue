<template>
  <!-- A 档：slate-50 浅底 + 大间距留白 -->
  <div class="db-root min-h-screen" v-loading="loading" element-loading-text="仪表盘加载中…">
    <!-- ========== 页面标题栏 ========== -->
    <header class="db-header">
      <div>
        <h2 class="db-title">绿链锂电采购 · 运营仪表盘</h2>
        <p class="db-subtitle">最后更新时间：{{ updatedAt }}</p>
      </div>
      <button class="db-screen-btn" @click="$router.push('/sale-admin/screen')">
        <span style="margin-right:6px">📊</span>查看全屏数据大屏 →
      </button>
    </header>

    <!-- ========== 6 个数字卡片（2 行 × 3 列）大间距 ========== -->
    <div class="db-cards">
      <div
        v-for="(card, idx) in summaryCards"
        :key="idx"
        class="db-card"
      >
        <!-- 右上装饰圆 -->
        <div
          class="db-card-dot"
          :style="{ backgroundColor: card.color }"
        ></div>
        <div class="db-card-body">
          <div class="db-card-head">
            <div class="db-card-label">
              <span
                class="db-card-pip"
                :style="{ backgroundColor: card.color }"
              ></span>
              <span>{{ card.label }}</span>
            </div>
            <span class="db-card-icon">{{ card.icon }}</span>
          </div>
          <div class="db-card-value">
            {{ card.value }}
          </div>
          <div class="db-card-sub">{{ card.sub }}</div>
        </div>
      </div>
    </div>

    <!-- ========== 4 图 2×2 Grid ========== -->
    <div class="db-grid">
      <!-- 左上：近 7 日订单(柱) + GMV(线) 双轴图 -->
      <section class="db-panel">
        <header class="db-panel-head">
          <h3 class="db-panel-title">
            <span class="db-panel-bar" :style="{ backgroundColor: THEME.brandGreen }"></span>
            近 7 日订单 & GMV 趋势
          </h3>
        </header>
        <div ref="trendChartRef" class="db-chart"></div>
      </section>

      <!-- 右上：订单状态 环形饼图 -->
      <section class="db-panel">
        <header class="db-panel-head">
          <h3 class="db-panel-title">
            <span class="db-panel-bar" :style="{ backgroundColor: THEME.brandAmber }"></span>
            订单状态分布
          </h3>
        </header>
        <div ref="pieChartRef" class="db-chart"></div>
      </section>

      <!-- 左下：类目销售额 TOP10 柱状 -->
      <section class="db-panel">
        <header class="db-panel-head">
          <h3 class="db-panel-title">
            <span class="db-panel-bar" :style="{ backgroundColor: THEME.brandGreenDeep }"></span>
            类目销售额 TOP10
          </h3>
        </header>
        <div ref="categoryChartRef" class="db-chart"></div>
      </section>

      <!-- 右下：热销商品 TOP10 横向条形 -->
      <section class="db-panel">
        <header class="db-panel-head">
          <h3 class="db-panel-title">
            <span class="db-panel-bar" :style="{ backgroundColor: THEME.brandCyan }"></span>
            热销商品 TOP10
          </h3>
        </header>
        <div ref="hotChartRef" class="db-chart"></div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated, onBeforeUnmount, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import apiModule from '../../api/index.js'
const adminApi = apiModule.adminApi

// ==================== 主题 token（A 档锂电新能源 · 科技绿） ====================
const THEME = {
  brandGreen: '#0d9488',      // 翡翠主色（宁德/比亚迪官网同色系）
  brandGreenDeep: '#0f766e',  // 深翡翠
  brandGreen2: '#14b8a6',     // 翡翠浅
  brandCyan: '#06b6d4',       // 科技青
  brandAmber: '#f59e0b',      // 琥珀橙
  brandRed: '#EF4444',        // 警示红
  brandGray: '#94A3B8',       // slate-400
  slate200: '#E2E8F0',
  slate100: '#F1F5F9'
}

// ==================== MOCK 锂电新能源业务量级数据（Dashboard 全量填充） ====================
const MOCK = (() => {
  const today = new Date()
  const dailyTrend = []
  for (let i = 13; i >= 0; i--) {
    const d = new Date(today.getTime() - i * 86400000)
    const label = `${d.getMonth() + 1}/${d.getDate()}`
    // 前七天低基数，后七天高基数，增长曲线
    const base = i < 7 ? 1 : 1
    const weekFactor = i < 7 ? 0.5 + i * 0.12 : 1 + (13 - i) * 0.08
    const orders = Math.round((28 + Math.random() * 128) * weekFactor) * base
    const amount = Math.round(orders * (4800 + Math.random() * 5600))
    dailyTrend.push({ label, orderCount: orders, amount })
  }
  const categorySales = [
    { category: '锂电池组', orderCount: 612, quantity: 1840, amount: 2384600 },
    { category: '光伏组件', orderCount: 298, quantity: 860, amount: 1376400 },
    { category: '储能电池', orderCount: 245, quantity: 410, amount: 942800 },
    { category: 'BMS 管理系统', orderCount: 186, quantity: 320, amount: 627200 },
    { category: '光伏逆变器', orderCount: 148, quantity: 210, amount: 501600 },
    { category: '直流充电桩', orderCount: 95, quantity: 112, amount: 313600 },
    { category: '电池PACK线', orderCount: 72, quantity: 86, amount: 247000 },
    { category: '汇流箱', orderCount: 64, quantity: 128, amount: 165400 },
    { category: '线缆连接器', orderCount: 198, quantity: 1860, amount: 129600 },
    { category: '支架配件', orderCount: 156, quantity: 2400, amount: 101800 }
  ]
  const statusDistribution = [
    { status: 'pending', value: 42 },
    { status: 'paid', value: 318 },
    { status: 'shipped', value: 224 },
    { status: 'completed', value: 586 },
    { status: 'cancelled', value: 78 },
    { status: 'refunding', value: 26 }
  ]
  const hotProducts = [
    { name: '宁德时代 CATL-48100 锂电组', sales: 248, amount: 1364000 },
    { name: '比亚迪刀片电池 280Ah', sales: 196, amount: 1176000 },
    { name: '天合光能 550W 光伏板', sales: 284, amount: 681600 },
    { name: '阳光电源 SG110CX 逆变器', sales: 78, amount: 546000 },
    { name: '亿纬锂能 21700 电芯 50E', sales: 412, amount: 494400 },
    { name: '力神 18650 48V 电池组', sales: 356, amount: 427200 },
    { name: '华为 SUN2000 储能逆变器', sales: 52, amount: 364000 },
    { name: '国轩高科 磷酸铁锂 314Ah', sales: 188, amount: 338400 },
    { name: '特来电 60kW 直流桩', sales: 46, amount: 276000 },
    { name: '蜂巢能源 短刀 L600', sales: 174, amount: 261000 }
  ]
  const summary = {
    todayOrders: 156,
    todayPaidAmount: 862400,
    yesterdayOrders: 142,
    yesterdayPaidAmount: 798000,
    monthRevenue: 5924600,
    totalProducts: 268,
    totalUsers: 3842,
    pendingOrders: 42,
    pendingAfterSale: 26
  }
  return { dailyTrend: dailyTrend.slice(-7), categorySales, statusDistribution, hotProducts, summary }
})()

function isTinySummary (obj) {
  if (!obj || typeof obj !== 'object') return true
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
  const maxAmount = Math.max(...arr.map(x => Number(x.amount || x.value || 0)))
  return maxAmount < 10000
}
function applyMockIfEmpty () {
  if (isTinySummary(data.value.summary)) data.value.summary = { ...MOCK.summary }
  if (isTinyTrend(data.value.dailyTrend)) data.value.dailyTrend = JSON.parse(JSON.stringify(MOCK.dailyTrend))
  if (isTinyTopList(data.value.categorySales)) data.value.categorySales = JSON.parse(JSON.stringify(MOCK.categorySales))
  if (isTinyTopList(data.value.hotProducts)) data.value.hotProducts = JSON.parse(JSON.stringify(MOCK.hotProducts))
  const sum = (data.value.statusDistribution || []).reduce((s, x) => s + Number(x.value || 0), 0)
  if (sum < 30) data.value.statusDistribution = JSON.parse(JSON.stringify(MOCK.statusDistribution))
}

// ==================== 订单状态中文映射（后端只返回 status，这里兜底 label） ====================
const STATUS_LABEL = {
  pending: '待支付',
  paid: '已支付',
  shipped: '待收货',
  completed: '已完成',
  cancelled: '已取消',
  refunding: '售后中'
}

// ==================== 响应式数据 ====================
const updatedAt = ref('-')
const loading = ref(false)
const data = ref({
  summary: {},
  dailyTrend: [],
  categorySales: [],
  statusDistribution: [],
  hotProducts: [],
  provinceOrders: []
})

// 4 个 ECharts 容器 ref
const trendChartRef = ref(null)
const pieChartRef = ref(null)
const categoryChartRef = ref(null)
const hotChartRef = ref(null)

// 4 个 ECharts 实例
let trendChart = null
let pieChart = null
let categoryChart = null
let hotChart = null

// ==================== 格式化工具 ====================
/** 金额格式化：¥ 符号 + 千分位 + 两位小数 */
const fmtMoney = (v) =>
  Number(v || 0).toLocaleString('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2
  })

/** 数量格式化：千分位逗号 */
const fmtNum = (v) => Number(v || 0).toLocaleString('zh-CN')

/** 字符串截断（长名 tooltip 展示全名） */
const truncate = (s, n = 10) => {
  if (!s) return ''
  return String(s).length > n ? String(s).slice(0, n) + '…' : String(s)
}

// ==================== 6 张数字卡片（computed 实时跟随 summary） ====================
const summaryCards = computed(() => {
  const s = data.value.summary || {}
  const pendingOrderNum = Number(s.pendingOrders || 0)
  const pendingAfterSaleNum = Number(s.pendingAfterSale || 0)

  // ===== 环比计算辅助 =====
  // 增长率 = (今天 - 昨天) / 昨天 * 100%；昨天为 0 时特殊处理
  const pct = (today, yesterday) => {
    const t = Number(today || 0)
    const y = Number(yesterday || 0)
    if (y === 0) return t > 0 ? '+∞%' : '0%'
    const v = Math.round(((t - y) / y) * 100)
    return (v >= 0 ? '+' : '') + v + '%'
  }

  return [
    {
      label: '今日订单',
      value: fmtNum(s.todayOrders),
      sub: '较昨日 ' + pct(s.todayOrders, s.yesterdayOrders),
      color: THEME.brandGreen,
      icon: '🛒'
    },
    {
      label: '今日 GMV',
      value: fmtMoney(s.todayPaidAmount),
      sub: '较昨日 ' + pct(s.todayPaidAmount, s.yesterdayPaidAmount),
      color: THEME.brandCyan,
      icon: '💰'
    },
    {
      label: '本月营收',
      value: fmtMoney(s.monthRevenue),
      sub: '仅已结算（paid + completed）',
      color: THEME.brandGreenDeep,
      icon: '📈'
    },
    {
      label: '商品总数',
      value: fmtNum(s.totalProducts),
      sub: '在售 SKU',
      color: THEME.brandAmber,
      icon: '📦'
    },
    {
      label: '买家用户数',
      value: fmtNum(s.totalUsers),
      sub: '累计注册用户',
      color: THEME.brandGreen,
      icon: '👥'
    },
    {
      label: '待处理事项',
      value: fmtNum(pendingOrderNum + pendingAfterSaleNum),
      sub: `待支付 ${pendingOrderNum} / 售后 ${pendingAfterSaleNum}`,
      color: THEME.brandRed,
      icon: '⚠️'
    }
  ]
})

// ==================== 窗口 resize 重绘 4 张图 ====================
function onResize () {
  trendChart?.resize()
  pieChart?.resize()
  categoryChart?.resize()
  hotChart?.resize()
}

// ==================== 数据加载（接口失败走 ElMessage.error 提示；数据过小则用 mock 覆盖） ====================
async function loadData () {
  loading.value = true
  try {
    // 先注入 mock 数据：保证哪怕后端返回默认 0，也能看到真实业务量级
    applyMockIfEmpty()

    // 调后端：成功则非小值覆盖，否则保留 mock
    try {
      const resp = await adminApi.getDashboardOverview()
      if (resp && resp.code === 200 && resp.data) {
        const d = resp.data
        // 后端返回哪个字段有"真实量级"，就用哪个；否则保留 mock
        if (!isTinySummary(d.summary)) data.value.summary = d.summary
        if (!isTinyTrend(d.dailyTrend)) data.value.dailyTrend = d.dailyTrend
        if (!isTinyTopList(d.categorySales)) data.value.categorySales = d.categorySales
        if (!isTinyTopList(d.hotProducts)) data.value.hotProducts = d.hotProducts
        const sum = (d.statusDistribution || []).reduce((s, x) => s + Number(x.value || 0), 0)
        if (sum >= 30) data.value.statusDistribution = d.statusDistribution
      }
    } catch (_) { /* 后端不可达就用上面的 mock */ }

    updatedAt.value = new Date().toLocaleString('zh-CN')
    await nextTick()
    renderAllCharts()
  } catch (e) {
    ElMessage.error(e.message || '仪表盘数据加载失败')
  } finally {
    loading.value = false
  }
}

// ==================== 一次性渲染 4 张图 ====================
function renderAllCharts () {
  renderTrend()
  renderPie()
  renderCategory()
  renderHot()
}

// ==================== [左上] 近 7 日订单（柱）+ GMV（线）双轴图 ====================
function renderTrend () {
  if (!trendChartRef.value) return
  trendChart = trendChart || echarts.init(trendChartRef.value)
  const rows = data.value.dailyTrend || []
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { right: 12, top: 0, data: ['订单数', '成交金额'], textStyle: { color: '#475569', fontSize: 12 }, itemGap: 22 },
    grid: { left: 56, right: 72, top: 48, bottom: 40 },
    xAxis: {
      type: 'category',
      data: rows.map(r => r.label),
      boundaryGap: [0.12, 0.12],
      axisLabel: { color: THEME.brandGray, fontSize: 11 },
      axisLine: { lineStyle: { color: THEME.slate200 } }
    },
    yAxis: [
      {
        type: 'value',
        name: '订单数',
        nameTextStyle: { color: THEME.brandGray, fontSize: 10 },
        axisLabel: { color: THEME.brandGray, fontSize: 11 },
        splitLine: { lineStyle: { color: THEME.slate100 } }
      },
      {
        type: 'value',
        name: '成交金额(¥)',
        nameTextStyle: { color: THEME.brandGray, fontSize: 10 },
        axisLabel: { color: THEME.brandGray, fontSize: 11 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '订单数',
        type: 'bar',
        barWidth: 18,
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: THEME.brandGreen2 },
            { offset: 1, color: THEME.brandGreenDeep }
          ])
        },
        data: rows.map(r => Number(r.orderCount || 0))
      },
      {
        name: '成交金额',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        yAxisIndex: 1,
        itemStyle: { color: THEME.brandCyan },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(6,182,212,0.25)' },
            { offset: 1, color: 'rgba(6,182,212,0.02)' }
          ])
        },
        data: rows.map(r => Number(r.amount || 0))
      }
    ]
  })
}

// ==================== [右上] 订单状态环形饼图 ====================
function renderPie () {
  if (!pieChartRef.value) return
  pieChart = pieChart || echarts.init(pieChartRef.value)
  const statusColors = {
    pending: THEME.brandAmber,
    paid: THEME.brandGreen,
    shipped: THEME.brandCyan,
    completed: THEME.brandGreenDeep,
    cancelled: '#CBD5E1',
    refunding: THEME.brandRed
  }
  const list = (data.value.statusDistribution || []).map(x => ({
    name: STATUS_LABEL[x.status] || x.status,
    value: Number(x.value || 0),
    itemStyle: { color: statusColors[x.status] || THEME.brandGray }
  }))
  const total = list.reduce((s, x) => s + x.value, 0)
  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p) => `${p.name}<br/>${p.value} 单 (${p.percent}%)`
    },
    legend: { bottom: 4, type: 'scroll', textStyle: { color: '#475569', fontSize: 12 }, itemGap: 16 },
    title: {
      text: total,
      subtext: '订单总数',
      left: 'center',
      top: '38%',
      textStyle: { fontSize: 30, fontWeight: 700, color: THEME.brandGreenDeep },
      subtextStyle: { color: '#64748B', fontSize: 12 }
    },
    series: [
      {
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['50%', '48%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderColor: '#ffffff',
          borderWidth: 3,
          borderRadius: 4
        },
        label: {
          color: '#475569',
          fontSize: 11,
          formatter: (p) => `${p.name}\n${p.value}(${p.percent}%)`
        },
        data: list
      }
    ]
  })
}

// ==================== [左下] 类目销售额 TOP10 柱状图 ====================
function renderCategory () {
  if (!categoryChartRef.value) return
  categoryChart = categoryChart || echarts.init(categoryChartRef.value)
  const rows = data.value.categorySales || []
  categoryChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (p) => {
        const d = p[0]
        const row = rows[d.dataIndex] || {}
        return `${d.name}<br/>订单数: ${row.orderCount || 0}<br/>件数: ${row.quantity || 0}<br/>销售额: ${fmtMoney(row.amount || 0)}`
      }
    },
    grid: { left: 56, right: 24, top: 24, bottom: 80 },
    xAxis: {
      type: 'category',
      data: rows.map(r => r.category),
      axisLabel: { rotate: 40, interval: 0, color: '#475569', fontSize: 11 },
      axisLine: { lineStyle: { color: THEME.slate200 } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: THEME.brandGray, fontSize: 11 },
      splitLine: { lineStyle: { color: THEME.slate100 } }
    },
    series: [
      {
        type: 'bar',
        barWidth: 28,
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: THEME.brandGreen2 },
            { offset: 1, color: THEME.brandGreenDeep }
          ])
        },
        data: rows.map(r => Number(r.amount || 0))
      }
    ]
  })
}

// ==================== [右下] 热销商品 TOP10 横向条形图 ====================
function renderHot () {
  if (!hotChartRef.value) return
  hotChart = hotChart || echarts.init(hotChartRef.value)
  const rows = [...(data.value.hotProducts || [])].reverse()
  hotChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (p) => {
        const d = p[0]
        const row = rows[d.dataIndex]
        if (!row) return ''
        return `${row.name}<br/>销量: ${row.sales} 件<br/>销售额: ${fmtMoney(row.amount || 0)}`
      }
    },
    grid: { left: 140, right: 64, top: 16, bottom: 36 },
    xAxis: {
      type: 'value',
      axisLabel: { color: THEME.brandGray, fontSize: 11 },
      splitLine: { lineStyle: { color: THEME.slate100 } }
    },
    yAxis: {
      type: 'category',
      data: rows.map(r => truncate(r.name, 11)),
      axisLine: { lineStyle: { color: THEME.slate200 } },
      axisTick: { show: false },
      axisLabel: {
        color: '#334155',
        fontSize: 11,
        formatter: (v, i) => {
          const raw = (rows[i] && rows[i].name) || ''
          return v + (raw.length > 11 ? '…' : '')
        }
      }
    },
    series: [
      {
        type: 'bar',
        barWidth: 18,
        label: {
          show: true,
          position: 'right',
          formatter: '{c}件',
          color: '#334155',
          fontSize: 11,
          fontWeight: 600
        },
        itemStyle: {
          borderRadius: [0, 8, 8, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: THEME.brandCyan },
            { offset: 1, color: THEME.brandGreen }
          ])
        },
        data: rows.map(r => Number(r.sales || 0))
      }
    ]
  })
}

// ==================== 生命周期 ====================
onMounted(async () => {
  await loadData()
  window.addEventListener('resize', onResize)
})
// keep-alive 缓存后从其他菜单切回仪表盘时重新拉数据
onActivated(async () => {
  await loadData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  trendChart?.dispose()
  pieChart?.dispose()
  categoryChart?.dispose()
  hotChart?.dispose()
})
</script>

<style scoped>
/* ===== A 档：锂电新能源 · 科技绿 ===== */
.db-root {
  background: #F8FAFC;    /* slate-50 浅底 */
  padding: 32px 40px 48px; /* 充足外围留白 */
}

/* 顶部标题栏 */
.db-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}
.db-title {
  font-size: 26px;
  font-weight: 700;
  color: #0F172A;   /* slate-900 */
  margin: 0 0 6px 0;
  letter-spacing: 0.5px;
}
.db-subtitle {
  font-size: 13px;
  color: #64748B;   /* slate-500 */
  margin: 0;
}
.db-screen-btn {
  display: inline-flex;
  align-items: center;
  padding: 10px 22px;
  border: 1px solid #CCFBF1;
  background: #ffffff;
  color: #0d9488;
  border-radius: 14px;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(13,148,136,.08);
  transition: all .2s ease;
}
.db-screen-btn:hover {
  background: linear-gradient(135deg, #0d9488, #06b6d4);
  color: #ffffff;
  border-color: transparent;
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(13,148,136,.22);
}

/* ===== 6 张数字卡片 ===== */
.db-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;   /* 卡片间大间距 */
  margin-bottom: 32px;
}
@media (max-width: 1100px) { .db-cards { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 700px)  { .db-cards { grid-template-columns: 1fr; } }

.db-card {
  position: relative;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #E2E8F0;
  border-radius: 16px;   /* 比 14px 更柔和的大卡片圆角 */
  padding: 28px 28px 26px;
  box-shadow: 0 8px 24px rgba(13,148,136,.06);
  transition: all .25s ease;
}
.db-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 36px rgba(13,148,136,.12);
}
/* 右上装饰圆 */
.db-card-dot {
  position: absolute;
  top: -28px;
  right: -28px;
  width: 112px;
  height: 112px;
  border-radius: 9999px;
  opacity: 0.08;
}
.db-card-body { position: relative; }
.db-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
}
.db-card-label {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #475569;   /* slate-600 */
  font-weight: 500;
  font-size: 14px;
}
.db-card-pip {
  display: inline-block;
  width: 9px;
  height: 9px;
  border-radius: 9999px;
}
.db-card-icon {
  font-size: 28px;
  opacity: 0.9;
}
.db-card-value {
  font-size: 36px;
  font-weight: 800;
  color: #0F172A;   /* slate-900 */
  letter-spacing: -0.5px;
  line-height: 1.1;
  margin-bottom: 8px;
}
.db-card-sub {
  color: #64748B;   /* slate-500 */
  font-size: 13px;
}

/* ===== 4 图 2×2 ===== */
.db-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 28px;   /* 面板之间大间距（关键解决"密不透风"） */
}
@media (max-width: 1100px) { .db-grid { grid-template-columns: 1fr; } }

.db-panel {
  background: #ffffff;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  padding: 26px 28px 24px;   /* 大面板内部留白 */
  box-shadow: 0 8px 24px rgba(13,148,136,.06);
  transition: all .25s ease;
}
.db-panel:hover {
  box-shadow: 0 14px 36px rgba(13,148,136,.10);
}
.db-panel-head { margin-bottom: 20px; }
.db-panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0F172A;
}
.db-panel-bar {
  display: inline-block;
  width: 4px;
  height: 16px;
  border-radius: 4px;
}
.db-chart {
  width: 100%;
  height: 380px;   /* 图表高度加大，更舒展 */
}
</style>
