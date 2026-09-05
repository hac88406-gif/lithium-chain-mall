<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 登录弹窗 -->
    <LoginModal 
      :visible="loginModalVisible"
      @close="handleLoginModalClose"
      @login="handleLoginSuccess"
    />

    <section class="applications-banner">
      <div class="banner-content">
        <h1>我的洽谈申请记录</h1>
        <p>查看您提交的商务合作申请状态</p>
      </div>
    </section>

    <section class="applications-section py-16">
      <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        <div v-if="!userInfo" class="text-center py-16">
          <div class="w-20 h-20 mx-auto mb-6 rounded-full bg-gray-200 flex items-center justify-center">
            <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
            </svg>
          </div>
          <h3 class="text-xl font-semibold text-gray-700 mb-4">请先登录</h3>
          <p class="text-gray-500 mb-6">登录后即可查看您的洽谈申请记录</p>
          <button 
            class="btn-primary"
            @click="openLoginModal"
          >立即登录</button>
        </div>

        <div v-else>
          <div class="flex items-center justify-between mb-8">
            <h2 class="text-2xl font-bold text-dark">申请列表</h2>
            <span class="text-gray-500">共 {{ applications.length }} 条记录</span>
          </div>

          <div class="space-y-4">
            <div 
              v-for="(app, index) in applications" 
              :key="index"
              class="application-card"
            >
              <div class="card-header">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-4">
                    <div class="application-type">{{ app.type === 'cooperation' ? '大客户洽谈' : '媒体采访' }}</div>
                    <span class="application-date">{{ app.date }}</span>
                  </div>
                  <span 
                    class="status-badge"
                    :class="getStatusClass(app.status)"
                  >{{ getStatusText(app.status) }}</span>
                </div>
              </div>
              <div class="card-body">
                <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div class="info-item">
                    <span class="info-label">企业/媒体名称</span>
                    <span class="info-value">{{ app.companyName }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">联系人</span>
                    <span class="info-value">{{ app.contactPerson }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">联系电话</span>
                    <span class="info-value">{{ app.phone }}</span>
                  </div>
                </div>
                <div v-if="app.remark || app.description" class="mt-4">
                  <span class="info-label block mb-2">需求描述</span>
                  <p class="text-gray-600 text-sm">{{ app.remark || app.description }}</p>
                </div>
              </div>
              <div class="card-footer">
                <button 
                  v-if="app.status === 'pending'"
                  class="btn-secondary"
                  @click="cancelApplication(index)"
                >
                  取消申请
                </button>
                <span v-else class="text-sm text-gray-400">
                  {{ getStatusDesc(app.status) }}
                </span>
              </div>
            </div>

            <div v-if="applications.length === 0" class="text-center py-16">
              <div class="w-20 h-20 mx-auto mb-6 rounded-full bg-gray-100 flex items-center justify-center">
                <svg class="w-10 h-10 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"/>
                </svg>
              </div>
              <h3 class="text-lg font-medium text-gray-600 mb-2">暂无申请记录</h3>
              <p class="text-gray-400 mb-6">您还没有提交过洽谈申请</p>
              <button 
                class="btn-primary"
                @click="goToCooperation"
              >去提交申请</button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import LoginModal from '../components/LoginModal.vue'

const router = useRouter()
const userInfo = ref(null)
const loginModalVisible = ref(false)

// 模拟申请数据
const applications = ref([
  {
    id: '1',
    type: 'cooperation',
    companyName: '苏州新能源科技有限公司',
    contactPerson: '张先生',
    phone: '138****8888',
    date: '2024-01-15 14:30',
    status: 'approved',
    remark: '希望采购大容量储能电池，用于工业园区储能项目，预计采购量500万。'
  },
  {
    id: '2',
    type: 'media',
    companyName: '科技日报',
    contactPerson: '李记者',
    phone: '139****9999',
    date: '2024-01-14 10:00',
    status: 'pending',
    description: '计划对贵公司进行深度采访，报道锂电池行业最新技术发展趋势。'
  },
  {
    id: '3',
    type: 'cooperation',
    companyName: '上海智能制造有限公司',
    contactPerson: '王经理',
    phone: '137****7777',
    date: '2024-01-10 09:15',
    status: 'processing',
    remark: '咨询锂电池整线解决方案，需要定制自动化生产线。'
  }
])

const getStatusText = (status) => {
  const statusMap = {
    pending: '待审核',
    processing: '处理中',
    approved: '已通过',
    rejected: '已拒绝',
    cancelled: '已取消'
  }
  return statusMap[status] || status
}

const getStatusClass = (status) => {
  const classMap = {
    pending: 'status-pending',
    processing: 'status-processing',
    approved: 'status-approved',
    rejected: 'status-rejected',
    cancelled: 'status-cancelled'
  }
  return classMap[status] || ''
}

const getStatusDesc = (status) => {
  const descMap = {
    processing: '我们正在处理您的申请，请耐心等待',
    approved: '申请已通过，工作人员将尽快联系您',
    rejected: '抱歉，您的申请未通过审核',
    cancelled: '申请已取消'
  }
  return descMap[status] || ''
}

const openLoginModal = () => {
  loginModalVisible.value = true
}

const handleLoginModalClose = () => {
  loginModalVisible.value = false
}

const handleLoginSuccess = (user) => {
  userInfo.value = user
  loginModalVisible.value = false
}

const cancelApplication = (index) => {
  if (confirm('确定要取消该申请吗？')) {
    applications.value[index].status = 'cancelled'
    alert('申请已取消')
  }
}

const goToCooperation = () => {
  router.push('/cooperation')
}

onMounted(() => {
  // 从localStorage读取登录状态
  const savedUser = localStorage.getItem('userInfo')
  if (savedUser) {
    try {
      userInfo.value = JSON.parse(savedUser)
    } catch (e) {
      console.error('Failed to parse user info:', e)
    }
  } else {
    // 未登录，显示登录弹窗
    loginModalVisible.value = true
  }
})
</script>

<style scoped>
.applications-banner {
  background: linear-gradient(135deg, #0d9488 0%, #0891b2 100%);
  padding: 40px 0;
  text-align: center;
  color: white;
}

.banner-content h1 {
  font-size: 2rem;
  font-weight: bold;
  margin-bottom: 8px;
}

.banner-content p {
  font-size: 1rem;
  opacity: 0.9;
}

.application-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f3f4f6;
}

.application-type {
  font-weight: 600;
  color: #0d9488;
  background: #f0fdf4;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 14px;
}

.application-date {
  font-size: 14px;
  color: #9ca3af;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

.status-pending {
  background: #fef3c7;
  color: #d97706;
}

.status-processing {
  background: #dbeafe;
  color: #2563eb;
}

.status-approved {
  background: #dcfce7;
  color: #16a34a;
}

.status-rejected {
  background: #fee2e2;
  color: #dc2626;
}

.status-cancelled {
  background: #f3f4f6;
  color: #6b7280;
}

.card-body {
  padding: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
}

.info-label {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 4px;
}

.info-value {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.card-footer {
  padding: 12px 20px;
  background: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.btn-primary {
  background: linear-gradient(135deg, #0d9488 0%, #0891b2 100%);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 150, 136, 0.3);
}

.btn-secondary {
  background: white;
  color: #6b7280;
  border: 1px solid #e5e7eb;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-secondary:hover {
  background: #f9fafb;
  color: #374151;
}

.text-dark {
  color: #1f2937;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}
</style>