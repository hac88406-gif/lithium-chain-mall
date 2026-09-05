<template>
  <div>
    <section class="tech-banner">
      <div class="banner-content">
        <h1>技术科技</h1>
        <p>专注锂电技术创新，引领新能源产业发展</p>
      </div>
    </section>

    <section class="tech-section py-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <h2 class="text-3xl font-bold text-dark mb-4">核心技术</h2>
          <p class="text-gray-500">掌握行业领先的锂电池研发与制造技术</p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <div class="tech-card">
            <div class="tech-icon">
              <svg class="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
              </svg>
            </div>
            <h3>正极材料技术</h3>
            <p>自主研发的高能量密度正极材料，提升电池能量密度和循环寿命。</p>
          </div>

          <div class="tech-card">
            <div class="tech-icon">
              <svg class="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
              </svg>
            </div>
            <h3>电池管理系统</h3>
            <p>智能BMS系统，实现电池状态实时监测与精准管理。</p>
          </div>

          <div class="tech-card">
            <div class="tech-icon">
              <svg class="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"/>
              </svg>
            </div>
            <h3>智能制造技术</h3>
            <p>全自动化生产线，实现高效、精准的电池制造流程。</p>
          </div>
        </div>
      </div>
    </section>

    <section class="projects-section py-16 bg-gray-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <h2 class="text-3xl font-bold text-dark mb-4">科技前沿项目</h2>
          <p class="text-gray-500">探索前沿技术，推动行业创新发展</p>
        </div>

        <div class="flex flex-wrap justify-center gap-8">
          <div class="project-card" v-for="(project, index) in projects" :key="index">
            <div class="project-image">
              <svg viewBox="0 0 300 180" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect width="300" height="180" fill="#e0f2f1" rx="8"/>
                <circle cx="150" cy="90" r="40" fill="#0d9488" opacity="0.3"/>
                <circle cx="150" cy="90" r="25" fill="#0d9488" opacity="0.5"/>
                <rect x="130" y="80" width="40" height="20" fill="white" rx="4"/>
                <rect x="110" y="110" width="80" height="8" fill="#0d9488" opacity="0.6" rx="4"/>
                <rect x="120" y="125" width="60" height="6" fill="#0d9488" opacity="0.4" rx="3"/>
              </svg>
            </div>
            <div class="project-content">
              <span class="project-tag">{{ project.tag }}</span>
              <h3 class="project-title">{{ project.title }}</h3>
              <p class="project-desc">{{ project.description }}</p>
              <div class="project-meta">
                <span class="project-date">{{ project.date }}</span>
                <span class="project-status">{{ project.status }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="process-section py-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <h2 class="text-3xl font-bold text-dark mb-4">生产工艺展示</h2>
          <p class="text-gray-500">
            可视化展示锂电池生产工艺流程
            <span v-if="processStats.nodeCount" class="ml-2 text-primary font-medium">
              （共 {{ processStats.nodeCount }} 道工序 / {{ processStats.relationshipCount }} 条流转关系）
            </span>
          </p>
          <p v-if="processLoading" class="text-primary mt-2 text-sm">工序数据源加载中...</p>
          <p v-else-if="processError" class="text-red-500 mt-2 text-sm">
            工序数据加载失败：{{ processError }}（请确认 Neo4j 已启动）
          </p>
        </div>

        <div class="process-graph">
          <div class="process-flow-wrapper">
            <div class="process-flow">
              <template v-if="mainProcessSteps && mainProcessSteps.length">
                <div
                  v-for="(step, index) in mainProcessSteps"
                  :key="step.id || index"
                  class="flow-step"
                >
                  <div :class="['step-card', { 'critical': isCriticalStep(step.type) }]" :title="step.description">
                    <div :class="['step-number', { 'critical': isCriticalStep(step.type) }]">
                      {{ index + 1 }}
                    </div>
                    <div class="step-name">{{ step.name }}</div>
                    <div v-if="getWorkshopName(step.workshop)" class="step-workshop">
                      {{ getWorkshopName(step.workshop) }}
                    </div>
                    <div v-if="step.duration" class="step-duration">预计 {{ step.duration }}h</div>
                  </div>
                  <div v-if="index < mainProcessSteps.length - 1" class="flow-arrow">
                    <svg viewBox="0 0 48 24" fill="none">
                      <path d="M4 12h40M36 6l6 6-6 6" stroke="#0d9488" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                  </div>
                </div>
              </template>
              <template v-else>
                <div class="flow-empty">暂无工序数据</div>
              </template>
            </div>
          </div>

          <div class="text-center mt-8">
            <button
              @click="goToProcessGraph"
              class="btn-graph"
              :disabled="processLoading"
            >
              <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
              </svg>
              查看完整生产工艺图谱
            </button>
          </div>
        </div>
      </div>
    </section>

    <section class="stats-section py-16 bg-gray-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <h2 class="text-3xl font-bold text-dark mb-4">技术实力</h2>
          <p class="text-gray-500">持续创新，引领行业发展</p>
        </div>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div class="stat-item text-center">
            <div class="stat-number">200+</div>
            <div class="stat-label">专利技术</div>
          </div>
          <div class="stat-item text-center">
            <div class="stat-number">50+</div>
            <div class="stat-label">研发人员</div>
          </div>
          <div class="stat-item text-center">
            <div class="stat-number">10+</div>
            <div class="stat-label">研发中心</div>
          </div>
          <div class="stat-item text-center">
            <div class="stat-number">5000+</div>
            <div class="stat-label">技术论文</div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index.js'

const router = useRouter()

// ===== 工序流程（全部来自后端 /api/client/process 真实 Neo4j 数据） =====
const processSteps = ref([])
const processLoading = ref(false)
const processError = ref('')
const processStats = ref({ nodeCount: 0, relationshipCount: 0 })

/**
 * 关键工序判定：以 type 枚举为依据，将 PREPROCESS / COATING / WINDING / FORMATION /
 * MODULE_ASSEMBLY / PACKAGING / PERFORMANCE_TEST 等核心节点标为关键工序。
 */
const CRITICAL_TYPES = new Set([
  'PREPROCESS', 'COATING', 'WINDING', 'FORMATION',
  'MODULE_ASSEMBLY', 'PACKAGING', 'PERFORMANCE_TEST'
])
const isCriticalStep = (type) => CRITICAL_TYPES.has(String(type || '').toUpperCase())

/**
 * 只展示前 4 个主要工序（原料预处理 → 涂布 → 辊压 → 分切）
 * 按 id 字典序取前 4，用户要求："只需要列出四个主要工艺名就行了"
 */
const mainProcessSteps = computed(() => {
  const list = Array.isArray(processSteps.value) ? processSteps.value : []
  return list.slice(0, 4)
})

/**
 * 车间字段兼容性处理：接口可能返回字符串（"涂布车间"）或 Workshop 对象（{name:"涂布车间",...}），
 * 截图里出现 {"name":"...","id":null} 就是被当作 JSON 原样拼出来了，统一转字符串。
 */
const getWorkshopName = (ws) => {
  if (!ws) return ''
  if (typeof ws === 'string') return ws
  if (typeof ws === 'object') return ws.name || ws.title || ''
  return String(ws)
}

const loadProcessChain = async () => {
  processLoading.value = true
  processError.value = ''
  try {
    // 图谱统计信息（节点数/关系统）
    try {
      const infoResp = await api.getProcessInfo()
      if (infoResp && infoResp.code === 200 && infoResp.data) {
        processStats.value = infoResp.data
      }
    } catch (e) {
      // 统计失败不阻塞主流程渲染
    }

    const resp = await api.getProcessChain()
    if (!resp || resp.code !== 200) {
      processError.value = (resp && resp.message) || '接口返回异常'
      processSteps.value = []
      return
    }
    const payload = resp.data || {}
    const nodes = Array.isArray(payload.nodes) ? payload.nodes : []

    // 以 id 顺序排序（P001 -> P014），保证前端横向流水线展示顺序稳定
    nodes.sort((a, b) => {
      const ai = String(a.id || '')
      const bi = String(b.id || '')
      return ai.localeCompare(bi)
    })

    processSteps.value = nodes
  } catch (e) {
    processError.value = e?.message || '请求失败'
    processSteps.value = []
  } finally {
    processLoading.value = false
  }
}

const goToProcessGraph = () => {
  router.push('/process-graph')
}

const projects = ref([
  {
    tag: '研发中',
    title: '固态电池技术研发',
    description: '致力于开发下一代固态电池技术，提升能量密度和安全性。',
    date: '2024年3月启动',
    status: '进行中'
  },
  {
    tag: '已完成',
    title: '钠离子电池产业化',
    description: '成功实现钠离子电池的规模化生产，成本较锂电池降低30%。',
    date: '2023年12月完成',
    status: '已投产'
  },
  {
    tag: '规划中',
    title: '氢燃料电池集成',
    description: '布局氢燃料电池技术，探索多元化新能源解决方案。',
    date: '预计2025年启动',
    status: '筹备中'
  }
])

onMounted(() => {
  loadProcessChain()
})
</script>

<style scoped>
.tech-banner {
  background: linear-gradient(135deg, #0d9488 0%, #0891b2 100%);
  padding: 60px 0;
  text-align: center;
  color: white;
}

.banner-content h1 {
  font-size: 2.5rem;
  font-weight: bold;
  margin-bottom: 16px;
}

.banner-content p {
  font-size: 1.25rem;
  opacity: 0.9;
}

.tech-section {
  background: white;
}

.tech-card {
  background: #f9fafb;
  padding: 32px;
  border-radius: 16px;
  text-align: center;
  transition: all 0.3s ease;
}

.tech-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.tech-icon {
  width: 80px;
  height: 80px;
  background: #e0f2f1;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  color: #0d9488;
}

.tech-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.tech-card p {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}

.process-section {
  background: white;
}

.process-graph {
  background: #f9fafb;
  border-radius: 16px;
  padding: 40px 32px;
}

/* 四个工艺一张横屏放满，不再需要横向滚动条（用户要求删除滑动条） */
.process-flow-wrapper {
  overflow: hidden;
}

.process-flow {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  width: 100%;
}

.flow-empty {
  min-width: 100%;
  text-align: center;
  padding: 40px 0;
  color: #9ca3af;
  font-size: 14px;
}

.flow-step {
  display: flex;
  align-items: center;
}

.step-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 20px;
  background: white;
  border-radius: 12px;
  border: 2px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.25s ease;
  min-width: 120px;
}

.step-card:hover {
  box-shadow: 0 8px 20px rgba(0, 128, 96, 0.18);
  transform: translateY(-2px);
}

.step-card.critical {
  border-color: #0d9488;
  background: linear-gradient(135deg, #f0fdfa 0%, #ffffff 100%);
}

.step-number {
  width: 36px;
  height: 36px;
  background: #e5e7eb;
  color: #6b7280;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
  transition: all 0.3s ease;
}

.step-number.critical {
  background: linear-gradient(135deg, #0d9488 0%, #0891b2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(0, 150, 136, 0.3);
}

.step-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  text-align: center;
  line-height: 1.4;
}

.step-workshop {
  margin-top: 6px;
  font-size: 12px;
  color: #4b5563;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 999px;
}

.step-duration {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
}

.flow-arrow {
  width: 48px;
  height: 24px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 4px;
}

@media (max-width: 1200px) {
  .process-flow {
    flex-wrap: wrap;
    justify-content: center;
    gap: 8px;
  }

  .flow-step {
    width: calc(50% - 8px);
    justify-content: center;
  }

  .step-card {
    min-width: 100%;
    padding: 14px 16px;
  }

  .flow-arrow {
    display: none;
  }
}

@media (max-width: 768px) {
  .process-flow {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .flow-step {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .step-card {
    flex-direction: row;
    min-width: auto;
    justify-content: flex-start;
    gap: 12px;
    padding: 12px 16px;
  }

  .step-number {
    margin-bottom: 0;
    flex-shrink: 0;
  }

  .step-name,
  .step-workshop,
  .step-duration {
    text-align: left;
  }

  .flow-arrow {
    display: flex;
    width: 32px;
    height: 20px;
    transform: rotate(90deg);
    margin: 8px 0;
  }
}

.btn-graph {
  display: inline-flex;
  align-items: center;
  padding: 12px 32px;
  background: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
  color: white;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.3);
}

.btn-graph:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-graph:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(249, 115, 22, 0.4);
}

.btn-graph:active:not(:disabled) {
  transform: translateY(0);
}

.stats-section {
  background: #f9fafb;
}

.stat-item {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.stat-number {
  font-size: 2.5rem;
  font-weight: bold;
  color: #0d9488;
  margin-bottom: 8px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.text-dark {
  color: #1f2937;
}

.text-primary {
  color: #0d9488;
}

.projects-section {
  background: #f9fafb;
}

.project-card {
  width: 300px;
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.3s ease;
}

.project-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
}

.project-image {
  width: 100%;
  height: 180px;
  overflow: hidden;
}

.project-image svg {
  width: 100%;
  height: 100%;
}

.project-content {
  padding: 20px;
}

.project-tag {
  display: inline-block;
  padding: 4px 12px;
  background: #e0f2f1;
  color: #0d9488;
  font-size: 12px;
  font-weight: 500;
  border-radius: 20px;
  margin-bottom: 12px;
}

.project-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.project-desc {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.project-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.project-date {
  font-size: 12px;
  color: #999;
}

.project-status {
  font-size: 12px;
  color: #0d9488;
  font-weight: 500;
}
</style>
