<template>
  <div class="timeline-section py-16">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="text-center mb-12">
        <h2 class="text-3xl font-bold text-dark mb-4">企业发展历程</h2>
        <p class="text-gray-500">见证绿链锂电的成长之路</p>
      </div>

      <div 
        ref="scrollContainer"
        class="year-scroll-box"
        @scroll="handleScroll"
      >
        <div class="year-track">
          <div 
            v-for="(item, index) in timelineData" 
            :key="index" 
            class="year-item"
            :class="{ active: activeIndex === index }"
          >
            <span class="year-number">{{ item.year }}</span>
            <span class="year-dot"></span>
            <span class="year-event">{{ item.event }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const timelineData = ref([
  { year: '2017', event: '锂电项目立项，组建研发小组' },
  { year: '2018', event: '公司正式成立' },
  { year: '2019', event: '研发中心投入运营' },
  { year: '2020', event: '首条锂电池自动化产线投产' },
  { year: '2021', event: '获得高新技术企业认证' },
  { year: '2022', event: '新厂区奠基开工' },
  { year: '2023', event: '年产规模突破500万只' },
  { year: '2024', event: '二期扩产项目启动建设' },
  { year: '2025', event: '搭建海外销售渠道，拓展海外市场' },
  { year: '2026', event: '全厂智能制造升级规划落地' },
  { year: '2027', event: '新型储能电池专用产线建成' }
])

const scrollContainer = ref(null)
const activeIndex = ref(0)

const handleScroll = () => {
  if (!scrollContainer.value) return

  const container = scrollContainer.value
  const totalItems = timelineData.value.length
  
  // 计算可滚动范围
  const scrollRange = container.scrollWidth - container.clientWidth
  
  // 如果没有滚动空间，默认选中第一个
  if (scrollRange <= 0) {
    activeIndex.value = 0
    return
  }

  // 边界判定：滚动到最左侧
  if (container.scrollLeft <= 0) {
    activeIndex.value = 0
    return
  }

  // 边界判定：滚动到最右侧
  if (container.scrollLeft >= scrollRange - 1) {
    activeIndex.value = totalItems - 1
    return
  }

  // 基于滚动位置百分比计算激活节点
  // 将滚动范围平均分配给所有节点间隔
  const scrollPercent = container.scrollLeft / scrollRange
  
  // 计算应该激活的节点索引
  // 使用线性映射：滚动0% → 节点0，滚动100% → 节点n-1
  const targetIndex = Math.round(scrollPercent * (totalItems - 1))
  
  // 确保索引在有效范围内
  activeIndex.value = Math.max(0, Math.min(targetIndex, totalItems - 1))
}

onMounted(() => {
  handleScroll()
  
  // 降低防抖延迟，提高响应速度
  let debounceTimer = null
  const debouncedScroll = () => {
    clearTimeout(debounceTimer)
    debounceTimer = setTimeout(handleScroll, 8)
  }
  scrollContainer.value?.addEventListener('scroll', debouncedScroll)
})

onUnmounted(() => {
})
</script>

<style scoped>
.timeline-section {
  background: #ffffff;
}

.text-dark {
  color: #1f2937;
}

.year-scroll-box {
  width: 90%;
  height: 160px;
  margin: 0 auto;
  overflow-x: auto;
  overflow-y: hidden;
  background: #f8fcfb;
  border-radius: 16px;
  border: 1px solid #e8f5f3;
  position: relative;
}

.year-track {
  display: flex;
  align-items: center;
  gap: 60px;
  padding: 25px 35px;
  min-width: max-content;
}

.year-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 110px;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.year-number {
  font-size: 1.5rem;
  font-weight: 700;
  color: #94a3b8;
  line-height: 1.3;
  transition: color 0.3s ease;
}

.year-item.active .year-number {
  color: #059669;
}

.year-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #cbd5e1;
  margin: 10px 0;
  transition: all 0.3s ease;
}

.year-item.active .year-dot {
  width: 16px;
  height: 16px;
  background: #059669;
  box-shadow: 0 0 12px rgba(5, 150, 105, 0.5), 0 0 24px rgba(5, 150, 105, 0.3);
}

.year-event {
  font-size: 0.85rem;
  color: #94a3b8;
  text-align: center;
  white-space: nowrap;
  line-height: 1.4;
  transition: color 0.3s ease;
}

.year-item.active .year-event {
  color: #059669;
  font-weight: 500;
}

.year-scroll-box::-webkit-scrollbar {
  height: 6px;
}

.year-scroll-box::-webkit-scrollbar-track {
  background: #e8f5f3;
  border-radius: 3px;
  margin: 0 25px;
}

.year-scroll-box::-webkit-scrollbar-thumb {
  background: linear-gradient(90deg, #059669, #10b981);
  border-radius: 3px;
}

.year-scroll-box::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(90deg, #047857, #059669);
}

.year-scroll-box {
  scrollbar-width: thin;
  scrollbar-color: #059669 #e8f5f3;
}

.year-scroll-box:hover {
  cursor: grab;
}

.year-scroll-box:active {
  cursor: grabbing;
}
</style>