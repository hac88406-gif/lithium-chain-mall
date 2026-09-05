<template>
  <div class="process-graph-page min-h-screen bg-gray-50">
    <!-- 顶部标题区 -->
    <section class="page-header bg-white shadow-sm">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 class="text-3xl font-bold text-dark mb-2">锂电池生产工艺图谱 · 数字孪生</h1>
        <p class="text-gray-500">
          基于 Neo4j 图数据库：14 道工序 · 8 台设备 · 7 类质量风险 · 6 类缺陷 · 4 款产品的全链路关联与实时图算法推演
        </p>
      </div>
    </section>

    <!-- 主体内容区 -->
    <section class="main-content py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex flex-col lg:flex-row gap-6">
          <!-- 左侧控制面板 -->
          <aside class="lg:w-72 flex-shrink-0">
            <div class="bg-white rounded-xl shadow-sm p-6">
              <!-- 产品线切换 -->
              <div class="mb-6">
                <h3 class="text-sm font-semibold text-gray-700 mb-3">产品线</h3>
                <div class="flex flex-col gap-2">
                  <button
                    @click="switchProductLine('cell')"
                    class="w-full py-2.5 px-3 rounded-lg text-sm font-medium transition-all"
                    :class="activeProductLine === 'cell' ? 'bg-primary text-white shadow shadow-primary/20' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                  >
                    ⚡ 电芯产线
                    <span class="ml-1 text-xs opacity-75">(P001~P009 · 9 道)</span>
                  </button>
                  <button
                    @click="switchProductLine('module')"
                    class="w-full py-2.5 px-3 rounded-lg text-sm font-medium transition-all"
                    :class="activeProductLine === 'module' ? 'bg-primary text-white shadow shadow-primary/20' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                  >
                    🔋 储能模组
                    <span class="ml-1 text-xs opacity-75">(P010~P014 · PACK 5 道)</span>
                  </button>
                  <button
                    @click="switchProductLine('both')"
                    class="w-full py-2.5 px-3 rounded-lg text-sm font-medium transition-all border"
                    :class="activeProductLine === 'both'
                      ? 'bg-gradient-to-r from-amber-50 to-orange-50 border-amber-300 text-amber-800 shadow'
                      : 'bg-white border-gray-200 text-gray-500 hover:border-amber-200 hover:text-amber-700'"
                  >
                    🔗 完整链路（电芯 + PACK 一体化）
                  </button>
                </div>
                <!-- 当前范围提示 -->
                <div class="mt-3 px-3 py-2 rounded-lg text-xs" :class="activeHint.bg">
                  <b>{{ activeHint.title }}</b>
                  <span class="ml-1 opacity-80">{{ activeHint.desc }}</span>
                </div>
              </div>

              <!-- 智能图分析（Neo4j 图算法） -->
              <div class="mb-6">
                <h3 class="text-sm font-semibold text-gray-700 mb-1">智能图分析</h3>
                <p class="text-xs text-gray-400 mb-3">全部由 Neo4j 原生 Cypher 图算法实时计算</p>

                <div class="space-y-2">
                  <button
                    @click="runCriticalPath"
                    class="ana-btn"
                    :class="analysisMode === 'critical' ? 'ana-btn-active-amber' : ''"
                  >
                    🛣️ 关键路径 / 瓶颈分析
                  </button>
                  <button
                    @click="runEquipmentConflict"
                    class="ana-btn"
                    :class="analysisMode === 'equipment' ? 'ana-btn-active-purple' : ''"
                  >
                    ⚙️ 设备共用冲突
                  </button>
                  <select
                    v-model="selectedRisk"
                    @change="runRiskChain"
                    class="ana-select"
                    :class="analysisMode === 'risk' ? 'ana-select-active-orange' : ''"
                  >
                    <option value="">🛡 选择质量风险 → 传播链推演</option>
                    <option v-for="r in riskOptions" :key="r" :value="r">🛡 {{ r }}</option>
                  </select>
                  <select
                    v-model="selectedProduct"
                    @change="runProductTrace"
                    class="ana-select"
                    :class="analysisMode === 'product' ? 'ana-select-active-emerald' : ''"
                  >
                    <option value="">📦 选择产品 → 工艺追溯</option>
                    <option v-for="p in products" :key="p.productId" :value="p.productId">📦 {{ p.productName }}</option>
                  </select>
                </div>

                <!-- 分析结果卡片 -->
                <div v-if="analysisMode !== 'none'" class="mt-3 analysis-result">
                  <div class="flex items-start justify-between gap-2">
                    <div class="flex-1 min-w-0">
                      <div class="text-xs font-bold text-gray-700 mb-1">{{ analysisTitle }}</div>
                      <div class="text-xs text-gray-500 leading-relaxed">{{ analysisDesc }}</div>
                      <ul v-if="analysisList.length" class="mt-2 space-y-1">
                        <li
                          v-for="(item, i) in analysisList"
                          :key="i"
                          class="text-xs text-gray-600 leading-relaxed"
                          :class="item.nodeId ? 'cursor-pointer hover:text-primary hover:bg-primary/5 rounded px-1 -mx-1 transition-colors' : ''"
                          :title="item.nodeId ? '点击在图中定位聚光该节点' : ''"
                          @click="focusFromList(item)"
                        >
                          <span class="text-gray-400 mr-1">{{ i + 1 }}.</span>{{ item.text }}
                          <span v-if="item.nodeId" class="ml-1 text-primary">◎</span>
                        </li>
                      </ul>
                    </div>
                    <button
                      @click="clearAnalysis"
                      class="flex-shrink-0 px-2 py-1 text-xs text-red-500 hover:text-red-700 hover:bg-red-50 rounded"
                    >
                      ✕ 清除
                    </button>
                  </div>
                </div>

                <div v-if="insightUnavailable" class="mt-3 px-3 py-2 rounded-lg text-xs bg-red-50 text-red-600 border border-red-100">
                  ⚠️ Neo4j 图算法数据不可用，请确认 Neo4j 已启动
                </div>
              </div>

              <!-- 工序搜索 -->
              <div class="mb-6">
                <h3 class="text-sm font-semibold text-gray-700 mb-3">工序搜索</h3>
                <div class="relative">
                  <input
                    v-model="searchKeyword"
                    type="text"
                    placeholder="输入工序名称搜索..."
                    class="w-full py-2 px-4 pr-10 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                    @keyup.enter="searchProcess"
                  />
                  <button
                    @click="searchProcess"
                    class="absolute right-2 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-primary"
                  >
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
                    </svg>
                  </button>
                </div>
              </div>

              <!-- 图例说明 -->
              <div>
                <h3 class="text-sm font-semibold text-gray-700 mb-3">图例说明</h3>
                <div class="space-y-3">
                  <div class="flex items-center gap-3">
                    <div class="w-4 h-4 rounded-full bg-primary"></div>
                    <span class="text-sm text-gray-600">关键工序</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <div class="w-4 h-4 rounded-full bg-blue-400"></div>
                    <span class="text-sm text-gray-600">普通工序</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <div class="w-4 h-4 rounded-full bg-orange-400"></div>
                    <span class="text-sm text-gray-600">质检工序</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <div class="w-4 h-4 rounded-full bg-green-400"></div>
                    <span class="text-sm text-gray-600">包装工序</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <div class="w-4 h-4 rounded-full ring-2 ring-amber-400 ring-offset-2 bg-white"></div>
                    <span class="text-sm text-gray-600">聚光节点（脉动主角）</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <div class="w-4 h-4 rounded-full bg-white border-2 border-amber-400 opacity-60"></div>
                    <span class="text-sm text-gray-600">关联层（路径节点）</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <div class="w-4 h-4 rounded-full bg-gray-300 opacity-40"></div>
                    <span class="text-sm text-gray-600">无关节点（淡出）</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <div class="w-8 h-0.5 bg-primary"></div>
                    <span class="text-sm text-gray-600">工序流向</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <div class="flex gap-1">
                      <div class="w-3 h-4 rounded-sm" style="background:#b91c1c"></div>
                      <div class="w-3 h-4 rounded-sm" style="background:#ea580c"></div>
                      <div class="w-3 h-4 rounded-sm" style="background:#f59e0b"></div>
                      <div class="w-3 h-4 rounded-sm" style="background:#fbbf24"></div>
                    </div>
                    <span class="text-sm text-gray-600">影响推演（近→远渐变）</span>
                  </div>
                </div>
              </div>
            </div>
          </aside>

          <!-- 右侧画布容器 -->
          <main class="flex-1 min-w-0">
            <!-- 数字孪生 KPI 面板（Neo4j 图算法实时计算） -->
            <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
              <div class="kpi-card">
                <div class="kpi-value text-emerald-600">{{ kpi.totalDuration ?? '—' }}<span class="kpi-unit">h</span></div>
                <div class="kpi-label">🔄 总吞吐周期</div>
                <div class="kpi-sub">{{ kpi.nodeCount ?? '—' }} 道工序合计工时</div>
              </div>
              <div class="kpi-card">
                <div class="kpi-value text-blue-600">{{ kpi.balanceRate ?? '—' }}<span class="kpi-unit">%</span></div>
                <div class="kpi-label">⚖️ 产线平衡率</div>
                <div class="kpi-sub">瓶颈：{{ kpi.bottleneckName ?? '—' }} {{ kpi.bottleneckDuration ?? '' }}h</div>
              </div>
              <div class="kpi-card">
                <div class="kpi-value text-amber-600">{{ kpi.yieldRate ?? '—' }}<span class="kpi-unit">%</span></div>
                <div class="kpi-label">✅ 模拟良率</div>
                <div class="kpi-sub">全链路 7 个风险点连乘</div>
              </div>
              <div class="kpi-card">
                <div class="kpi-value text-orange-600">{{ kpi.criticalPathTotal ?? '—' }}<span class="kpi-unit">h</span></div>
                <div class="kpi-label">🛣️ 关键路径时长</div>
                <div class="kpi-sub">瓶颈占比 {{ kpi.bottleneckShare ?? '—' }}% · {{ (kpi.criticalPathIds || []).length }} 道工序</div>
              </div>
            </div>

            <div class="bg-white rounded-xl shadow-sm overflow-hidden">
              <!-- 画布工具栏 -->
              <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100">
                <span class="text-sm text-gray-500">
                  工艺图谱
                  <span v-if="analysisMode !== 'none'" class="ml-2 px-2 py-0.5 rounded-full text-xs bg-amber-50 text-amber-700 border border-amber-200">
                    分析中：{{ analysisTitle }}
                  </span>
                </span>
                <div class="flex items-center gap-2">
                  <button
                    v-if="analysisMode !== 'none'"
                    @click="clearAnalysis"
                    class="px-3 py-1.5 text-sm text-red-500 hover:bg-red-50 rounded-lg transition-all"
                  >
                    退出分析
                  </button>
                  <button
                    @click="resetView"
                    class="px-3 py-1.5 text-sm text-gray-600 hover:text-primary hover:bg-gray-100 rounded-lg transition-all"
                  >
                    重置视图
                  </button>
                  <button
                    @click="zoomIn"
                    class="px-3 py-1.5 text-sm text-gray-600 hover:text-primary hover:bg-gray-100 rounded-lg transition-all"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM10 7v3m0 0v3m0-3h3m-3 0H7"/>
                    </svg>
                  </button>
                  <button
                    @click="zoomOut"
                    class="px-3 py-1.5 text-sm text-gray-600 hover:text-primary hover:bg-gray-100 rounded-lg transition-all"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM10 15v3m0 0v3m0-3h3m-3 0H7"/>
                    </svg>
                  </button>
                </div>
              </div>

              <!-- 画布区域 -->
              <div ref="chartRef" class="chart-container" :class="{ loading: isLoading }">
                <!-- Loading状态 -->
                <div v-show="isLoading" class="loading-overlay">
                  <div class="loading-content">
                    <svg class="w-12 h-12 text-primary animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <p class="mt-4 text-gray-500">加载工艺数据中...</p>
                  </div>
                </div>

                <!-- 错误状态 -->
                <div v-show="!isLoading && hasError" class="error-overlay">
                  <div class="error-content">
                    <svg class="w-16 h-16 text-red-400 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                    <p class="mt-4 text-gray-600">加载工艺数据失败</p>
                    <p class="text-sm text-gray-400 mt-1">{{ errorMessage }}</p>
                    <button
                      @click="loadGraphData"
                      class="mt-4 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark transition-all"
                    >
                      重试加载
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </main>
        </div>
      </div>
    </section>

    <!-- 节点详情弹窗（多 Tab：基础 / 设备 / 风险 / 缺陷 / 产品） -->
    <div
      v-if="showNodeDetail"
      class="modal-overlay"
      @click.self="closeNodeDetail"
      @keydown.escape="closeNodeDetail"
    >
      <div class="modal-content detail-modal">
        <div class="modal-header">
          <h3>{{ selectedNode?.name }} <span class="text-xs font-normal text-gray-400 ml-1">{{ selectedNode?.id }}</span></h3>
          <button class="modal-close" @click="closeNodeDetail">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <!-- Tab 导航 -->
        <div class="modal-tabs">
          <button class="modal-tab" :class="{ active: detailTab === 'base' }" @click="detailTab = 'base'">📋 基础</button>
          <button class="modal-tab" :class="{ active: detailTab === 'equip' }" @click="detailTab = 'equip'">⚙️ 设备 {{ insightEquipments.length }}</button>
          <button class="modal-tab" :class="{ active: detailTab === 'risk' }" @click="detailTab = 'risk'">🛡 风险 {{ insightRisks.length }}</button>
          <button class="modal-tab" :class="{ active: detailTab === 'defect' }" @click="detailTab = 'defect'">⚠️ 缺陷 {{ insightDefects.length }}</button>
          <button class="modal-tab" :class="{ active: detailTab === 'product' }" @click="detailTab = 'product'">📦 产品 {{ insightProducts.length }}</button>
        </div>

        <div class="detail-body">
          <!-- Tab: 基础 -->
          <div v-show="detailTab === 'base'">
            <div class="detail-row">
              <span class="detail-label">工序类型</span>
              <span class="detail-value">{{ selectedNode?.type }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">所属车间</span>
              <span class="detail-value">{{ selectedNode?.workshop }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">预计耗时</span>
              <span class="detail-value">{{ selectedNode?.duration }}小时</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">描述</span>
              <span class="detail-value">{{ selectedNode?.description }}</span>
            </div>
            <div v-if="selectedNode?.prevSteps && selectedNode.prevSteps.length > 0" class="detail-section">
              <h4>前置工序</h4>
              <div class="tag-list">
                <span v-for="step in selectedNode.prevSteps" :key="step" class="tag">{{ step }}</span>
              </div>
            </div>
            <div v-if="selectedNode?.nextSteps && selectedNode.nextSteps.length > 0" class="detail-section">
              <h4>后置工序</h4>
              <div class="tag-list">
                <span v-for="step in selectedNode.nextSteps" :key="step" class="tag">{{ step }}</span>
              </div>
            </div>
          </div>

          <!-- Tab: 设备 -->
          <div v-show="detailTab === 'equip'">
            <div v-if="insightEquipments.length === 0" class="empty-tip">该工序暂无绑定设备</div>
            <div v-for="(eq, i) in insightEquipments" :key="i" class="insight-card bg-blue-50 border-blue-100">
              <div class="font-medium text-gray-800 text-sm">⚙️ {{ eq.name }}</div>
              <div class="text-xs text-gray-500 mt-1">型号：{{ eq.model }} · 稼动率(OEE)：{{ eq.oeePercent }}%</div>
            </div>
          </div>

          <!-- Tab: 风险 -->
          <div v-show="detailTab === 'risk'">
            <div v-if="insightRisks.length === 0" class="empty-tip">该工序暂无登记质量风险</div>
            <div v-for="(r, i) in insightRisks" :key="i" class="insight-card bg-amber-50 border-amber-100">
              <div class="font-medium text-gray-800 text-sm">🛡 {{ r.name }}</div>
              <div class="text-xs text-gray-500 mt-1">
                发生率：{{ r.ratePercent }}% · 等级：
                <span :class="r.level === 'HIGH' ? 'text-red-600 font-bold' : 'text-amber-600'">
                  {{ r.level === 'HIGH' ? '🔴 高' : '🟡 中' }}
                </span>
              </div>
            </div>
          </div>

          <!-- Tab: 缺陷 -->
          <div v-show="detailTab === 'defect'">
            <div v-if="insightDefects.length === 0" class="empty-tip">该工序的风险暂无关联缺陷</div>
            <div class="tag-list">
              <span v-for="d in insightDefects" :key="d" class="tag tag-defect">⚠️ {{ d }}</span>
            </div>
            <p class="text-xs text-gray-400 mt-3">由该工序的风险经 LEADS_TO 关系传播得到（Neo4j 3 跳遍历）</p>
          </div>

          <!-- Tab: 产品 -->
          <div v-show="detailTab === 'product'">
            <div v-if="insightProducts.length === 0" class="empty-tip">暂无产品包含该工序</div>
            <div class="tag-list">
              <span v-for="p in insightProducts" :key="p" class="tag tag-product">📦 {{ p }}</span>
            </div>
            <p class="text-xs text-gray-400 mt-3">由 Product-[:CONTAINS]->Process 反向关系查询得到</p>
          </div>
        </div>

        <!-- 底部操作：影响范围推演 -->
        <div class="modal-footer">
          <button @click="runImpactFromModal" class="impact-btn">
            🌐 推演该工序异常的下游影响（多跳遍历）
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import api from '../api/index.js'
const graphApi = api.api || api

// ===== 基础状态 =====
const chartRef = ref(null)
const isLoading = ref(false)
const hasError = ref(false)
const errorMessage = ref('')
const activeProductLine = ref('cell')
const activeHint = computed(() => {
  switch (activeProductLine.value) {
    case 'cell':
      return {
        bg: 'bg-emerald-50 text-emerald-800 border border-emerald-200',
        title: '① 电芯制程：',
        desc: '从浆料涂布 → 卷绕/入壳 → 化成/分容，出厂是独立电芯。'
      }
    case 'module':
      return {
        bg: 'bg-cyan-50 text-cyan-800 border border-cyan-200',
        title: '② PACK 模组组装：',
        desc: '把电芯焊接成模组 → 封装电池包 → 性能/出厂检测，成品是完整的储能 PACK。'
      }
    case 'both':
    default:
      return {
        bg: 'bg-amber-50 text-amber-800 border border-amber-200',
        title: '③ 一体化全流程：',
        desc: '共 14 道工序，展示「电芯 → 模组 → 电池包」完整上下游关系。'
      }
  }
})
const searchKeyword = ref('')
const showNodeDetail = ref(false)
const selectedNode = ref(null)
const detailTab = ref('base')

// ===== 数字孪生图算法状态 =====
const kpi = ref({})
const riskChains = ref([])
const equipmentConflicts = ref([])
const products = ref([])
const insightUnavailable = ref(false)
const selectedRisk = ref('')
const selectedProduct = ref('')

// 当前图分析状态（none | critical | equipment | risk | product | impact）
const analysisMode = ref('none')
const analysisTitle = ref('')
const analysisDesc = ref('')
// analysisList 条目为 { text, nodeId? }：带 nodeId 的条目可点击，联动图中聚光
const analysisList = ref([])
const highlightIds = ref(new Set())   // 关联层：路径上的普通高亮节点
const spotlightIds = ref(new Set())   // 聚光层：≤3 个主角节点（脉动 + 放大）
const highlightColor = ref('#f59e0b')
let impactHops = null                 // 影响推演：Map<节点id, 距源头跳数>（渐变着色用）

// 当前渲染数据缓存（供高亮重渲染 / 脉动动画复用）
let currentNodes = []
let currentLinks = []
let currentNodeMap = new Map()
let lastNodeData = []    // 最近一次构建的完整 ECharts 节点数据（含 label，供脉动动画复用，防止名字被 merge 冲掉）

let chartInstance = null
let isUnmounted = false
let pulseTimer = null
let pulseGrow = false
let playbackTimers = []               // 播放式呈现的定时器队列（逐节点点亮）
let waitRunTimer = null               // 风险分析/源头分析时等待图数据加载的轮询定时器
let isPlaying = false                 // 播放中：暂停脉动，避免与重渲染打架

// 影响推漸变色板：源头深红 → 远端浅橙（按跳数取色）
const IMPACT_GRADIENT = ['#b91c1c', '#dc2626', '#ea580c', '#f59e0b', '#fbbf24']

// hex 转 rgba（用于节点底色的透明度控制）
const hexToRgba = (hex, alpha) => {
  const n = parseInt(hex.slice(1), 16)
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${alpha})`
}

// 按跳数取渐变色（hop=0 源头最深，hop=maxHop 最浅）
const hopColor = (hop, maxHop) => {
  const ratio = maxHop <= 1 ? 0 : hop / maxHop
  const idx = Math.min(IMPACT_GRADIENT.length - 1, Math.round(ratio * (IMPACT_GRADIENT.length - 1)))
  return IMPACT_GRADIENT[idx]
}

// 前端 BFS 计算各节点距源头的跳数（用于影响推演渐变着色，与后端多跳遍历互为印证）
const computeHops = (sourceId) => {
  const adj = new Map()
  currentLinks.forEach(l => {
    if (!adj.has(l.source)) adj.set(l.source, [])
    adj.get(l.source).push(l.target)
  })
  const hops = new Map([[sourceId, 0]])
  const queue = [sourceId]
  while (queue.length) {
    const cur = queue.shift()
    for (const nxt of (adj.get(cur) || [])) {
      if (!hops.has(nxt)) {
        hops.set(nxt, hops.get(cur) + 1)
        queue.push(nxt)
      }
    }
  }
  return hops
}

// 节点 id 数字化（P001→1，用于排序）
const numOfId = (id) => parseInt(String(id || '').replace(/\D/g, ''), 10) || 0

// 类型中文展示
const TYPE_CATEGORY = {
  PREPROCESS:        { label: '原料预处理', category: '关键',  color: '#059669' },
  COATING:           { label: '涂布',      category: '关键',  color: '#059669' },
  ROLLING:           { label: '辊压',      category: '普通',  color: '#60a5fa' },
  SLITTING:          { label: '分切',      category: '普通',  color: '#60a5fa' },
  WINDING:           { label: '卷绕',      category: '关键',  color: '#059669' },
  ENCASING:          { label: '入壳',      category: '普通',  color: '#60a5fa' },
  INJECTION:         { label: '注液',      category: '关键',  color: '#059669' },
  FORMATION:         { label: '化成',      category: '关键',  color: '#059669' },
  CAPACITY_TEST:     { label: '分容',      category: '质检',  color: '#fb923c' },
  WELDING:           { label: '焊接',      category: '关键',  color: '#059669' },
  MODULE_ASSEMBLY:   { label: '模组组装',  category: '关键',  color: '#059669' },
  PACKAGING:         { label: '电池包封装',category: '包装',  color: '#4ade80' },
  PERFORMANCE_TEST:  { label: '性能检测',  category: '质检',  color: '#fb923c' },
  FINAL_INSPECTION:  { label: '出厂检验',  category: '质检',  color: '#fb923c' }
}

const resolveTypeMeta = (type) => {
  const k = String(type || '').toUpperCase()
  return TYPE_CATEGORY[k] || { label: type || '未知', category: '普通', color: '#60a5fa' }
}

// 弹窗多 Tab 数据（从 /insight/node/{id} 拉取）
const nodeInsight = ref({})
const insightEquipments = computed(() => {
  const names = (nodeInsight.value.equipment || []).filter(v => v && String(v).trim())
  const oees = nodeInsight.value.equipmentOee || []
  return names.map((name, i) => {
    const oee = Number(oees[i] || 0)
    return { name, model: name.split(' ').slice(-1)[0] || name, oeePercent: Math.round(oee * 100) }
  })
})
const insightRisks = computed(() => {
  const names = (nodeInsight.value.riskNames || []).filter(v => v && String(v).trim())
  const rates = nodeInsight.value.riskRates || []
  const levels = nodeInsight.value.riskLevels || []
  return names.map((name, i) => ({
    name,
    ratePercent: (Number(rates[i] || 0) * 100).toFixed(1),
    level: levels[i] || 'MEDIUM'
  }))
})
const insightDefects = computed(() =>
  (nodeInsight.value.defects || []).filter(v => v && String(v).trim()))
const insightProducts = computed(() =>
  (nodeInsight.value.products || []).filter(v => v && String(v).trim()))

// 左栏风险下拉选项（从风险传播链去重）
const riskOptions = computed(() => {
  const set = new Set()
  riskChains.value.forEach(c => { if (c.riskName) set.add(c.riskName) })
  return [...set]
})

// ===== 加载图谱数据 =====
const loadGraphData = async () => {
  if (isUnmounted) return
  stopPlayback()   // 切换产品线时终止进行中的播放动画

  isLoading.value = true
  hasError.value = false
  errorMessage.value = ''

  try {
    const resp = await graphApi.getProcessChain()
    if (!resp || resp.code !== 200) {
      throw new Error((resp && resp.message) || '工序图谱接口返回异常')
    }
    const payload = resp.data || {}
    const allNodes = Array.isArray(payload.nodes) ? payload.nodes : []
    const allEdges = Array.isArray(payload.edges) ? payload.edges : []

    if (allNodes.length === 0) {
      throw new Error('Neo4j 中暂无 ProcessNode 节点，请确认 Neo4j 已启动并完成初始化。')
    }

    // 按产品线筛选节点与边
    const line = activeProductLine.value || 'both'
    const inLine = (id) => {
      if (line === 'both') return true
      const num = parseInt(String(id || '').replace(/\D/g, ''), 10) || 0
      return line === 'cell' ? num <= 9 : num >= 10
    }
    const nodes = allNodes.filter(n => inLine(n.id))
    if (nodes.length === 0) {
      throw new Error(line === 'cell' ? '电芯产线暂无节点数据（请检查 P001~P009 是否入库）' : '储能模组产线暂无节点数据（请检查 P010~P014 是否入库）')
    }
    const remainIdSet = new Set(nodes.map(n => n.id))
    const edges = allEdges.filter(e => remainIdSet.has(e.source) && remainIdSet.has(e.target))

    const normalizeWorkshop = (w) => {
      if (w == null) return ''
      if (typeof w === 'string') return w
      if (typeof w === 'object') return w.name || ''
      return String(w)
    }

    // 节点半径映射耗时占比：耗时越长圆越大（瓶颈工序一眼可辨）
    const maxDur = Math.max(...nodes.map(n => Number(n.duration) || 0), 1)
    const processedNodes = nodes.map(node => {
      const meta = resolveTypeMeta(node.type)
      const durRatio = (Number(node.duration) || 0) / maxDur
      const catBase = meta.category === '关键' ? 54 : (meta.category === '质检' ? 47 : 41)
      return {
        id: node.id,
        name: node.name,
        type: meta.label,
        category: meta.category,
        description: node.description,
        workshop: normalizeWorkshop(node.workshop),
        duration: node.duration,
        nextStepIds: (Array.isArray(node.nextStepIds) ? node.nextStepIds : []).filter(nid => remainIdSet.has(nid)),
        // 基础尺寸 = 工序类型基数 × (0.72 + 0.46 × 耗时占比)：普通最短 ≈30，关键最长 ≈64
        symbolSize: Math.round(catBase * (0.72 + 0.46 * durRatio)),
        _metaColor: meta.color,
        _metaCategory: meta.category
      }
    })

    const nodeMap = new Map()
    processedNodes.forEach(n => nodeMap.set(n.id, n))

    let graphLinks = edges && edges.length ? edges.slice() : []
    if (!graphLinks.length) {
      processedNodes.forEach(n => {
        ;(n.nextStepIds || []).forEach(nid => {
          graphLinks.push({ source: n.id, target: nid, relationType: 'NEXT_PROCESS' })
        })
      })
    }

    // 缓存当前数据（供分析高亮重渲染）
    currentNodes = processedNodes
    currentLinks = graphLinks
    currentNodeMap = nodeMap

    // 切换产品线后，正在进行的分析若超出范围则清除
    if (analysisMode.value !== 'none') {
      const stillValid = [...highlightIds.value].some(id => remainIdSet.has(id))
      if (!stillValid) {
        // 只清状态不渲染（下面紧接着 fullRebuild 统一渲染，避免多余 merge）
        stopPlayback()
        analysisMode.value = 'none'
        highlightIds.value = new Set()
        spotlightIds.value = new Set()
        impactHops = null
        analysisTitle.value = ''
        analysisDesc.value = ''
        analysisList.value = []
        selectedRisk.value = ''
        selectedProduct.value = ''
      }
    }

    renderChart(true)   // 数据重载：整体重建 + 重新布局
  } catch (e) {
    if (isUnmounted) return
    hasError.value = true
    errorMessage.value = e?.message || '加载失败'
  } finally {
    if (!isUnmounted) isLoading.value = false
  }
}

// ===== 加载图算法总览（KPI + 风险链 + 设备冲突 + 产品清单） =====
const loadInsightOverview = async () => {
  try {
    const resp = await graphApi.getProcessInsightOverview()
    if (!resp || resp.code !== 200 || !resp.data) {
      throw new Error('overview 接口异常')
    }
    kpi.value = resp.data.kpi || {}
    riskChains.value = resp.data.riskChains || []
    equipmentConflicts.value = resp.data.equipmentConflicts || []
    products.value = resp.data.products || []
    insightUnavailable.value = false
  } catch (e) {
    insightUnavailable.value = true
  }
}

// ===== 渲染图表（支持分析高亮 + 淡出无关节点） =====
// fullRebuild=true：初次渲染/切换产品线，notMerge 重建并重新力导向布局
// fullRebuild=false：分析高亮更新，merge 模式只改样式，节点坐标保持不动（防抖动）
const renderChart = (fullRebuild = false) => {
  if (isUnmounted) return
  if (!chartRef.value) return

  if (!chartInstance) {
    try {
      chartInstance = echarts.init(chartRef.value)
      fullRebuild = true   // 首次必然重建
    } catch (e) {
      return
    }
  }

  const hasHighlight = highlightIds.value.size > 0
  const hiColor = highlightColor.value
  const maxHop = impactHops ? Math.max(...[...impactHops.values()], 1) : 1

  const nodeData = currentNodes.map(node => {
    const palettes = {
      '关键': { bg: '#dcfce7', border: '#059669', shadow: 'rgba(5, 150, 105, 0.35)' },
      '普通': { bg: '#dbeafe', border: '#2563eb', shadow: 'rgba(37, 99, 235, 0.28)' },
      '质检': { bg: '#ffedd5', border: '#ea580c', shadow: 'rgba(234, 88, 12, 0.3)' },
      '包装': { bg: '#d1fae5', border: '#10b981', shadow: 'rgba(16, 185, 129, 0.28)' }
    }
    const cat = node._metaCategory || node.category || '普通'
    const palette = palettes[cat] || palettes['普通']
    const isHi = hasHighlight && highlightIds.value.has(node.id)
    // 聚光节点必须已被播放点亮（在 highlightIds 中）才显示金色光圈，
    // 否则播放初期瓶颈节点会从头亮到尾，失去"流水线走到瓶颈"的顺序感
    const isSpot = hasHighlight && spotlightIds.value.has(node.id) && highlightIds.value.has(node.id)

    let bgColor = palette.bg
    let borderColor = palette.border
    let borderWidth = 2.5
    let shadowBlur = 14
    let shadowColor = palette.shadow
    let symbolSize = node.symbolSize || 44
    let labelColor = '#1f2937'
    let opacity = 1

    if (hasHighlight) {
      if (isSpot) {
        // ① 聚光层（≤3 个主角）：金环 + 放大 + 强阴影，由 startPulse 持续脉动
        bgColor = '#fffbeb'
        borderColor = hiColor
        shadowColor = hiColor
        borderWidth = 4.5
        shadowBlur = 28
        symbolSize += 14
      } else if (isHi) {
        if (analysisMode.value === 'impact' && impactHops && impactHops.has(node.id)) {
          // ② 影响推演专用：按距源头跳数渐变（源头深红 → 远端浅橙），形成放射传播感
          const hop = impactHops.get(node.id) || 0
          const c = hopColor(hop, maxHop)
          bgColor = hexToRgba(c, 0.28)
          borderColor = c
          shadowColor = hexToRgba(c, 0.45)
          borderWidth = 3.5
          shadowBlur = 18
        } else {
          // ② 关联层：保留工序原色，仅描边提亮 + 呼应分析主题色
          borderColor = hiColor
          shadowColor = palette.shadow
          borderWidth = 3
          shadowBlur = 16
        }
      } else {
        // ③ 暗层：无关节点降透明度但仍清晰可读（给客户演示，工序名必须看得见）
        opacity = 0.38
        shadowBlur = 0
        labelColor = '#475569'
      }
    }

    return {
      id: node.id,
      name: node.name,
      type: node.type,
      description: node.description,
      workshop: node.workshop,
      duration: node.duration,
      nextStepIds: node.nextStepIds,
      category: node.category || cat,
      symbolSize,
      symbol: 'circle',
      itemStyle: {
        color: bgColor,
        borderColor,
        borderWidth,
        shadowBlur,
        shadowColor: isHi ? shadowColor : palette.shadow,
        shadowOffsetX: 1,
        shadowOffsetY: 3,
        opacity
      },
      label: {
        show: true,
        fontSize: isSpot ? 14 : (isHi ? 12 : (cat === '关键' ? 12 : 11)),
        fontWeight: isSpot || cat === '关键' ? 700 : 500,
        color: isSpot ? '#92400e' : labelColor,
        backgroundColor: 'rgba(255,255,255,0.75)',
        borderRadius: 6,
        padding: [2, 6]
      }
    }
  })

  // 缓存完整节点数据（含 label），供脉动动画复用
  lastNodeData = nodeData

  // 真实边：两端都位于高亮集内才算路径边
  const realEdges = []
  currentLinks.forEach(link => {
    const isHiLink = hasHighlight &&
      (analysisMode.value === 'critical' || analysisMode.value === 'impact' || analysisMode.value === 'product') &&
      highlightIds.value.has(link.source) && highlightIds.value.has(link.target)
    realEdges.push({
      source: link.source,
      target: link.target,
      lineStyle: {
        color: isHiLink ? hiColor : '#2176ff',
        width: isHiLink ? 4 : 2,
        curveness: 0.2,
        opacity: hasHighlight ? (isHiLink ? 1 : 0.08) : 0.8,
        type: 'curve'
      },
      symbol: ['none', 'arrow'],
      symbolSize: [0, 10]
    })
  })

  // 【产品追溯】补虚拟"跳过式"直连边：
  // 产品按工序列 P001→P002→...→P010→P012→... 排好序后，若相邻两道工序在真实 NEXT_STEP
  // 关系中并不直接相连（中间存在被跳过的节点，例如 PRD003 CTP 工艺跳过 P011），就补一条
  // hiColor 虚线直连，保证链路视觉上"不断线"。
  const bridgeEdges = []
  if (hasHighlight && analysisMode.value === 'product') {
    const local = [...highlightIds.value].sort((a, b) => numOfId(a) - numOfId(b))
    const linkSet = new Set(currentLinks.map(l => `${l.source}->${l.target}`))
    for (let i = 0; i < local.length - 1; i++) {
      const a = local[i]
      const b = local[i + 1]
      if (!linkSet.has(`${a}->${b}`)) {
        bridgeEdges.push({
          source: a,
          target: b,
          lineStyle: {
            color: hiColor,
            width: 3.5,
            curveness: -0.15,  // 反向微曲线，与真实 NEXT_STEP 的 0.2 曲线区分开
            opacity: 0.85,
            type: 'dashed'
          },
          symbol: ['none', 'arrow'],
          symbolSize: [0, 10]
        })
      }
    }
  }

  const linkData = [...realEdges, ...bridgeEdges]

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'node') {
          return `<div style="padding: 8px;">
            <div style="font-weight: bold; color: ${params.color}; margin-bottom: 4px;">${params.name}</div>
            <div style="font-size: 12px; color: #666;">类型: ${params.data.type}</div>
            <div style="font-size: 12px; color: #666;">车间: ${params.data.workshop}</div>
          </div>`
        } else if (params.dataType === 'edge') {
          return '工序流向'
        }
        return ''
      }
    },
    series: [{
      id: 'processGraph',
      type: 'graph',
      layout: 'force',
      data: nodeData,
      links: linkData,
      roam: true,
      draggable: true,
      force: {
        repulsion: 400,
        gravity: 0.1,
        edgeLength: [100, 200],
        friction: 0.6,
        layoutAnimation: true
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 5 },
        itemStyle: { shadowBlur: 15 }
      },
      selectedMode: 'single',
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: [0, 12]
    }]
  }

  if (fullRebuild) {
    // 初次/切产品线：整体重建，重新力导向布局
    chartInstance.setOption(option, { notMerge: true })
    bindChartEvents()
  } else {
    // 分析高亮：merge 模式只刷 data/links 样式，不传 force 布局配置 → 节点坐标不动、不抖动
    chartInstance.setOption({ series: [{ id: 'processGraph', data: nodeData, links: linkData }] })
  }
  // 播放进行中先不脉动（避免与逐节点重渲染打架），播放结束后再启动
  if (hasHighlight && !isPlaying) startPulse(); else stopPulse()
}

// ===== 播放式呈现：沿路径逐节点依次点亮（每 380ms 一个） =====
const startPlayback = (orderedIds) => {
  stopPlayback()
  if (!orderedIds.length) return
  isPlaying = true
  stopPulse()
  highlightIds.value = new Set()
  renderChart()
  orderedIds.forEach((id, i) => {
    playbackTimers.push(setTimeout(() => {
      if (isUnmounted) return
      highlightIds.value = new Set(orderedIds.slice(0, i + 1))
      if (i === orderedIds.length - 1) isPlaying = false
      renderChart()
    }, 380 * (i + 1)))
  })
}

const stopPlayback = () => {
  playbackTimers.forEach(clearTimeout)
  playbackTimers = []
  isPlaying = false
}

// ===== 脉动动画：仅聚光层（主角节点）发光呼吸 =====
// 关键：非聚光节点必须回传完整数据项（含 label），否则 ECharts merge 会把节点名字冲掉
const startPulse = () => {
  stopPulse()
  pulseTimer = setInterval(() => {
    if (!chartInstance || isUnmounted) return
    const hiColor = highlightColor.value
    pulseGrow = !pulseGrow
    // 以最近一次完整节点数据为基础（label/颜色/大小全都在），仅覆盖聚光节点的光晕
    const data = lastNodeData.map(item => {
      if (!spotlightIds.value.has(item.id)) {
        return item   // 原样回传，保留 label 等全部属性
      }
      return {
        ...item,
        itemStyle: {
          ...item.itemStyle,
          shadowBlur: pulseGrow ? 38 : 18,
          borderWidth: pulseGrow ? 5.5 : 3.5,
          borderColor: hiColor,
          shadowColor: hiColor
        }
      }
    })
    chartInstance.setOption({ series: [{ id: 'processGraph', data }] })
  }, 650)
}

const stopPulse = () => {
  if (pulseTimer) {
    clearInterval(pulseTimer)
    pulseTimer = null
  }
}

// ===== 图表事件绑定 =====
const bindChartEvents = () => {
  if (!chartInstance) return
  chartInstance.off('click')
  chartInstance.on('click', (params) => {
    if (params.dataType === 'node') {
      openNodeDetail(params.data)
    }
  })
}

// ===== 打开节点详情（拉取 Neo4j 全维度关联） =====
const openNodeDetail = async (nodeData) => {
  const prevSteps = []
  const nextSteps = []
  currentNodeMap.forEach((node) => {
    if (node.nextStepIds && node.nextStepIds.includes(nodeData.id)) {
      prevSteps.push(node.name)
    }
  })
  if (nodeData.nextStepIds) {
    nodeData.nextStepIds.forEach(nextId => {
      const nextNode = currentNodeMap.get(nextId)
      if (nextNode) nextSteps.push(nextNode.name)
    })
  }

  selectedNode.value = { ...nodeData, prevSteps, nextSteps }
  detailTab.value = 'base'
  nodeInsight.value = {}
  showNodeDetail.value = true

  // 异步拉取节点全维度详情（设备/风险/缺陷/产品，一次 OPTIONAL MATCH 聚合）
  try {
    const resp = await graphApi.getProcessNodeInsight(nodeData.id)
    if (resp && resp.code === 200 && resp.data) {
      nodeInsight.value = resp.data
    }
  } catch (e) {
    nodeInsight.value = {}
  }
}

const closeNodeDetail = () => {
  showNodeDetail.value = false
  selectedNode.value = null
  nodeInsight.value = {}
}

// ===== 分析 1：关键路径 / 瓶颈（Neo4j CPM 最长加权路径） =====
const runCriticalPath = () => {
  const ids = (kpi.value.criticalPathIds || []).filter(Boolean)
  const local = ids.filter(id => currentNodeMap.has(id))
  if (!local.length) return
  analysisMode.value = 'critical'
  highlightColor.value = '#f59e0b'
  impactHops = null
  // 聚光：瓶颈工序（全链路耗时最长的单点，优化的第一优先级）
  const bottleneckNode = [...currentNodeMap.values()].find(n => n.name === kpi.value.bottleneckName)
  spotlightIds.value = new Set(bottleneckNode ? [bottleneckNode.id] : [])
  analysisTitle.value = `🛣️ 关键路径（CPM 最长加权路径）`
  analysisDesc.value = `关键路径共 ${local.length} 道工序，总时长 ${kpi.value.criticalPathTotal}h；瓶颈工序「${kpi.value.bottleneckName}」单点 ${kpi.value.bottleneckDuration}h，占总工时 ${kpi.value.bottleneckShare}%。`
  analysisList.value = [
    { text: `瓶颈 = ${kpi.value.bottleneckName}（${kpi.value.bottleneckDuration}h，占 ${kpi.value.bottleneckShare}%）`, nodeId: bottleneckNode?.id },
    { text: `产线平衡率 ${kpi.value.balanceRate}%（越接近 100% 越均衡）` },
    { text: `节点大小 = 工序耗时占比，点击节点可查看设备/风险/缺陷/产品关联` }
  ]
  selectedRisk.value = ''
  selectedProduct.value = ''
  startPlayback(local)   // 沿路径逐节点点亮，终点停在瓶颈上
}

// ===== 分析 2：设备共用冲突（同一 Equipment 被两道工序 USES） =====
const runEquipmentConflict = () => {
  if (!equipmentConflicts.value.length) return
  const ids = new Set()
  const list = []
  equipmentConflicts.value.forEach(c => {
    ids.add(c.p1Id)
    ids.add(c.p2Id)
    list.push({ text: `${c.equipmentName}（OEE ${Math.round(Number(c.oee) * 100)}%）：${c.p1Name} ↔ ${c.p2Name}`, nodeId: c.p1Id })
  })
  const local = [...ids].filter(id => currentNodeMap.has(id))
  // 聚光：OEE 最低的共用设备所服务的工序（设备瓶颈 → 排产第一优先错峰对象）
  let worst = null
  equipmentConflicts.value.forEach(c => {
    if (!worst || Number(c.oee) < Number(worst.oee)) worst = c
  })
  const spot = new Set()
  if (worst) { spot.add(worst.p1Id); spot.add(worst.p2Id) }
  analysisMode.value = 'equipment'
  highlightIds.value = new Set(local)
  spotlightIds.value = spot
  highlightColor.value = '#8b5cf6'
  impactHops = null
  analysisTitle.value = `⚙️ 设备共用冲突（共 ${equipmentConflicts.value.length} 处）`
  analysisDesc.value = `同一台设备被多道工序共用，排产时可能产生排队等待。聚光节点 = OEE 最低的「${worst?.equipmentName}」（${Math.round(Number(worst?.oee || 0) * 100)}%）所服务工序，为产能瓶颈设备。`
  analysisList.value = list
  selectedRisk.value = ''
  selectedProduct.value = ''
  renderChart()
}

// ===== 分析 3：质量风险传播链（工序→风险→缺陷，3 跳多关系遍历） =====
const runRiskChain = () => {
  const risk = selectedRisk.value
  if (!risk) return
  const chains = riskChains.value.filter(c => c.riskName === risk)
  if (!chains.length) return
  const ids = new Set(chains.map(c => c.processId))
  const local = [...ids].filter(id => currentNodeMap.has(id))

  // 风险对应的工序不在当前产品线视图 → 自动切换到能包含它的产品线
  // 避免"所有点都亮了"（highlightIds 为空导致 hasHighlight=false，所有节点无对比地原样显示）
  if (local.length === 0) {
    const firstId = [...ids][0]
    const num = numOfId(firstId)
    let targetLine = 'both'
    if (num >= 1 && num <= 9) targetLine = 'cell'
    else if (num >= 10) targetLine = 'module'
    activeProductLine.value = targetLine
    // loadGraphData 会触发 renderChart，但分析模式尚未设好，所以在这里异步补跑高亮
    // 使用 watch(activeProductLine) 已经会调用 loadGraphData，等数据加载完再回调
    if (waitRunTimer) clearInterval(waitRunTimer)
    waitRunTimer = setInterval(() => {
      if (isUnmounted) { clearInterval(waitRunTimer); waitRunTimer = null; return }
      if (!isLoading.value && currentNodeMap.size > 0) {
        clearInterval(waitRunTimer)
        waitRunTimer = null
        applyRiskHighlight(chains, risk, ids)
      }
    }, 80)
    return
  }
  applyRiskHighlight(chains, risk, ids)
}

// runRiskChain 的实际高亮逻辑（抽出来供自动切换产品线后回调复用）
const applyRiskHighlight = (chains, risk, ids) => {
  const local = [...ids].filter(id => currentNodeMap.has(id))
  analysisMode.value = 'risk'
  highlightIds.value = new Set(local)
  // 风险源头工序本身就是聚光点（通常仅 1~2 个，天然稀缺）
  spotlightIds.value = new Set(local)
  highlightColor.value = '#ea580c'
  impactHops = null
  analysisTitle.value = `🛡 风险传播链：${risk}`
  analysisDesc.value = `${[...new Set(chains.map(c => c.processName))].join('、')} 的「${risk}」将沿 LEADS_TO 关系演化为下列缺陷，请提前加检。`
  analysisList.value = chains.map(c => ({
    text: `${c.processName} → ${c.riskName}（${(Number(c.rate) * 100).toFixed(1)}%）→ ${c.defectName}`,
    nodeId: c.processId
  }))
  selectedProduct.value = ''
  renderChart()
}

// ===== 分析 4：产品工艺追溯（Product-[:CONTAINS]->Process 反查） =====
const runProductTrace = () => {
  const pid = selectedProduct.value
  if (!pid) return
  const product = products.value.find(p => p.productId === pid)
  if (!product) return
  // 按工序序号排序（P001 → P014），供播放式依次点亮
  const local = (product.processIds || [])
    .filter(id => currentNodeMap.has(id))
    .sort((a, b) => numOfId(a) - numOfId(b))
  analysisMode.value = 'product'
  highlightColor.value = '#10b981'
  impactHops = null
  // 聚光：末道工序（成品下线交付点）
  const lastId = local[local.length - 1]
  spotlightIds.value = new Set(lastId ? [lastId] : [])
  analysisTitle.value = `📦 产品工艺追溯：${product.productName}`
  analysisDesc.value = `${product.productName}（${product.category}）共包含 ${local.length} 道关键工序（当前视图范围内），按工艺顺序逐站点亮，其余工序已淡出。`
  analysisList.value = local.map(id => {
    const n = currentNodeMap.get(id)
    return n ? { text: `${id} ${n.name} · ${n.duration}h · ${n.workshop}`, nodeId: id } : { text: id }
  })
  selectedRisk.value = ''
  startPlayback(local)   // 沿工艺路线逐站点亮，像流水线巡游
}

// ===== 分析 5：影响范围推演（NEXT_STEP 多跳下游遍历） =====
const runImpact = async (id) => {
  try {
    const resp = await graphApi.getProcessImpact(id)
    if (!resp || resp.code !== 200 || !resp.data) return
    const d = resp.data
    const downstreamIds = (d.downstream || []).map(x => x.id)
    // 前端 BFS 计算跳数（与后端多跳遍历互为印证），用于放射状渐变着色
    impactHops = computeHops(id)
    // 播放顺序：按跳数由近及远（源头 → 一跳 → 二跳 ...），传播感最强
    const ids = [id, ...downstreamIds].filter(x => currentNodeMap.has(x) && impactHops.has(x))
      .sort((a, b) => (impactHops.get(a) - impactHops.get(b)) || (numOfId(a) - numOfId(b)))
    analysisMode.value = 'impact'
    highlightColor.value = '#b91c1c'
    spotlightIds.value = new Set([id])   // 聚光：异常源头工序
    const srcName = d.source?.name || id
    analysisTitle.value = `🌐 影响范围推演：${srcName} 异常`
    analysisDesc.value = `沿 NEXT_STEP 多跳遍历，下游 ${downstreamIds.length} 道工序全部受波及（颜色越深 = 距源头越近）；受影响产品 ${ (d.affectedProducts || []).length } 款；链路累计风险率损失 ${d.riskLoss}%。`
    analysisList.value = [
      ...(d.affectedProducts || []).map(p => ({ text: `受影响产品：${p.name}（${p.category}）` })),
      ...ids.map(x => ({
        text: `第 ${impactHops.get(x)} 跳 · ${currentNodeMap.get(x)?.name || x}`,
        nodeId: x
      })),
      { text: `链路风险率损失合计：${d.riskLoss}%` }
    ]
    selectedRisk.value = ''
    selectedProduct.value = ''
    startPlayback(ids)   // 由源头向下游逐跳扩散点亮
  } catch (e) {
    // 静默失败，保持当前视图
  }
}

// ===== 点击左栏分析条目：联动图中聚光定位 =====
const focusFromList = (item) => {
  const nid = item?.nodeId
  if (!nid || !currentNodeMap.has(nid) || !highlightIds.value.has(nid)) return
  spotlightIds.value = new Set([nid])
  renderChart()
}

const runImpactFromModal = () => {
  const id = selectedNode.value?.id
  if (!id) return
  showNodeDetail.value = false
  runImpact(id)
}

// ===== 清除分析 =====
const clearAnalysis = () => {
  stopPlayback()
  analysisMode.value = 'none'
  highlightIds.value = new Set()
  spotlightIds.value = new Set()
  impactHops = null
  analysisTitle.value = ''
  analysisDesc.value = ''
  analysisList.value = []
  selectedRisk.value = ''
  selectedProduct.value = ''
  renderChart()
}

// ===== 搜索 / 视图控制 =====
const searchProcess = () => {
  if (!searchKeyword.value.trim() || !chartInstance) return
  chartInstance.dispatchAction({
    type: 'highlight',
    name: searchKeyword.value.trim()
  })
}

const switchProductLine = (line) => {
  activeProductLine.value = line
  loadGraphData()
}

const resetView = () => {
  if (chartInstance) {
    chartInstance.dispatchAction({ type: 'restore' })
    chartInstance.resize()
  }
}

const zoomIn = () => {
  if (chartInstance) chartInstance.dispatchAction({ type: 'zoom', zoom: 1.2 })
}

const zoomOut = () => {
  if (chartInstance) chartInstance.dispatchAction({ type: 'zoom', zoom: 0.8 })
}

const handleResize = () => {
  if (chartInstance) {
    try {
      chartInstance.resize()
    } catch (e) { /* 忽略 */ }
  }
}

watch(activeProductLine, () => loadGraphData())

onMounted(() => {
  isUnmounted = false
  loadGraphData()
  loadInsightOverview()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  isUnmounted = true
  stopPulse()
  stopPlayback()
  // 清理风险分析等待图数据加载的轮询定时器（组件销毁后不应继续访问 reactive 状态）
  if (waitRunTimer) { clearInterval(waitRunTimer); waitRunTimer = null }
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    try {
      chartInstance.dispose()
    } catch (e) { /* 忽略 */ }
    chartInstance = null
  }
})
</script>

<style scoped>
.process-graph-page {
  background: #f9fafb;
}

.page-header {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
}

.page-header h1 {
  color: #ffffff;
}

.page-header p {
  color: rgba(255, 255, 255, 0.8);
}

.chart-container {
  height: 600px;
  position: relative;
}

.loading-overlay,
.error-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  z-index: 5;
}

.loading-content,
.error-content {
  text-align: center;
}

/* ===== KPI 面板 ===== */
.kpi-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 16px 18px;
  box-shadow: 0 8px 24px rgba(13, 148, 136, 0.08);
  border: 1px solid #f1f5f9;
}

.kpi-value {
  font-size: 1.75rem;
  font-weight: 800;
  line-height: 1.2;
}

.kpi-unit {
  font-size: 0.9rem;
  font-weight: 600;
  margin-left: 2px;
}

.kpi-label {
  font-size: 0.8rem;
  color: #64748b;
  margin-top: 4px;
  font-weight: 500;
}

.kpi-sub {
  font-size: 0.72rem;
  color: #94a3b8;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ===== 智能图分析按钮 / 下拉 ===== */
.ana-btn {
  width: 100%;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 0.82rem;
  font-weight: 500;
  background: #f8fafc;
  color: #475569;
  border: 1px solid #e2e8f0;
  text-align: left;
  transition: all 0.2s ease;
  cursor: pointer;
}

.ana-btn:hover {
  border-color: #0d9488;
  color: #0d9488;
  background: #f0fdfa;
}

.ana-btn-active-amber {
  background: linear-gradient(135deg, #fffbeb, #fef3c7);
  border-color: #f59e0b;
  color: #b45309;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.2);
}

.ana-btn-active-purple {
  background: linear-gradient(135deg, #f5f3ff, #ede9fe);
  border-color: #8b5cf6;
  color: #6d28d9;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.2);
}

.ana-select {
  width: 100%;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 0.82rem;
  background: #f8fafc;
  color: #475569;
  border: 1px solid #e2e8f0;
  outline: none;
  cursor: pointer;
}

.ana-select:hover {
  border-color: #0d9488;
}

.ana-select-active-orange {
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  border-color: #ea580c;
  color: #9a3412;
}

.ana-select-active-emerald {
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  border-color: #10b981;
  color: #065f46;
}

.analysis-result {
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  max-height: 240px;
  overflow-y: auto;
}

/* ===== 弹窗 ===== */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.modal-content {
  background: #ffffff;
  border-radius: 16px;
  width: 90%;
  max-width: 520px;
  overflow: hidden;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}

.detail-modal {
  max-width: 520px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
  background: #f0fdf4;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #059669;
}

.modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: #e5e7eb;
  color: #374151;
}

/* ===== 弹窗 Tab ===== */
.modal-tabs {
  display: flex;
  border-bottom: 1px solid #f1f5f9;
  background: #fafafa;
  overflow-x: auto;
}

.modal-tab {
  flex: 1;
  padding: 10px 8px;
  font-size: 0.78rem;
  font-weight: 500;
  color: #64748b;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s ease;
}

.modal-tab:hover {
  color: #0d9488;
}

.modal-tab.active {
  color: #059669;
  border-bottom-color: #059669;
  background: #f0fdf4;
  font-weight: 700;
}

.detail-body {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f3f4f6;
}

.detail-label {
  color: #6b7280;
  font-size: 0.9rem;
}

.detail-value {
  color: #1f2937;
  font-weight: 500;
  font-size: 0.9rem;
  text-align: right;
  max-width: 65%;
}

.detail-section {
  margin-top: 20px;
}

.detail-section h4 {
  font-size: 0.95rem;
  color: #374151;
  margin-bottom: 10px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 4px 12px;
  background: #f0fdf4;
  color: #059669;
  border-radius: 20px;
  font-size: 0.85rem;
}

.tag-defect {
  background: #fef2f2;
  color: #b91c1c;
}

.tag-product {
  background: #eff6ff;
  color: #1d4ed8;
}

/* ===== 弹窗内关联卡片 ===== */
.insight-card {
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid;
  margin-bottom: 8px;
}

.empty-tip {
  padding: 24px;
  text-align: center;
  color: #94a3b8;
  font-size: 0.85rem;
}

/* ===== 弹窗底部影响推演按钮 ===== */
.modal-footer {
  padding: 14px 24px;
  border-top: 1px solid #f1f5f9;
  background: #fafafa;
}

.impact-btn {
  width: 100%;
  padding: 10px;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(135deg, #06b6d4, #0891b2);
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.impact-btn:hover {
  box-shadow: 0 6px 16px rgba(6, 182, 212, 0.35);
  transform: translateY(-1px);
}

@media (max-width: 1024px) {
  .chart-container {
    height: 500px;
  }
}

@media (max-width: 768px) {
  .chart-container {
    height: 400px;
  }
}
</style>
