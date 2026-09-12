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
        <h1>我的申请记录</h1>
        <p>查看您提交的商务合作申请与客服工单状态</p>
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
          <p class="text-gray-500 mb-6">登录后即可查看您的申请记录</p>
          <button class="btn-primary" @click="openLoginModal">立即登录</button>
        </div>

        <div v-else>
          <!-- Tab 切换：洽谈申请 / 客服工单 -->
          <div class="flex items-center justify-between mb-6">
            <div class="flex gap-2">
              <button
                class="tab-btn"
                :class="{ 'tab-btn-active': activeTab === 'cooperation' }"
                @click="activeTab = 'cooperation'"
              >洽谈申请（{{ cooperations.length }}）</button>
              <button
                class="tab-btn"
                :class="{ 'tab-btn-active': activeTab === 'service' }"
                @click="activeTab = 'service'"
              >客服工单（{{ serviceRequests.length }}）</button>
            </div>
            <span class="text-gray-500 text-sm">
              共 {{ activeTab === 'cooperation' ? cooperations.length : serviceRequests.length }} 条记录
            </span>
          </div>

          <!-- 洽谈申请列表（真实接口：/client/cooperation/mine） -->
          <div v-if="activeTab === 'cooperation'" class="space-y-4">
            <div v-for="(app, index) in cooperations" :key="'c' + index" class="application-card">
              <div class="card-header">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-4">
                    <div class="application-type">{{ app.type === 'media' ? '媒体采访' : '大客户洽谈' }}</div>
                    <span class="application-date">{{ fmtTime(app.createTime) }}</span>
                  </div>
                  <span class="status-badge" :class="getCoopStatusClass(app.status)">{{ getCoopStatusText(app.status) }}</span>
                </div>
              </div>
              <div class="card-body">
                <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div class="info-item">
                    <span class="info-label">企业/媒体名称</span>
                    <span class="info-value">{{ app.companyName || app.mediaTitle || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">联系人</span>
                    <span class="info-value">{{ app.contactPerson || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">联系电话</span>
                    <span class="info-value">{{ app.phone || '-' }}</span>
                  </div>
                </div>
                <div v-if="app.intention || app.remark" class="mt-4">
                  <span class="info-label block mb-2">需求描述</span>
                  <p class="text-gray-600 text-sm">{{ app.intention || app.remark }}</p>
                </div>
                <div v-if="app.reply" class="mt-4 reply-box">
                  <span class="info-label block mb-2">平台回复</span>
                  <p class="text-gray-600 text-sm">{{ app.reply }}</p>
                </div>
              </div>
            </div>

            <div v-if="loading" class="text-center py-16 text-gray-400">加载中...</div>
            <div v-else-if="cooperations.length === 0" class="text-center py-16">
              <h3 class="text-lg font-medium text-gray-600 mb-2">暂无洽谈申请</h3>
              <p class="text-gray-400 mb-6">您还没有提交过洽谈申请</p>
              <button class="btn-primary" @click="goToCooperation">去提交申请</button>
            </div>
          </div>

          <!-- 客服工单列表（真实接口：/client/service-request/mine） -->
          <div v-else class="space-y-4">
            <div v-for="(req, index) in serviceRequests" :key="'s' + index" class="application-card">
              <div class="card-header">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-4">
                    <div class="application-type type-service">客服工单</div>
                    <span class="application-date">{{ fmtTime(req.createTime) }}</span>
                  </div>
                  <span class="status-badge" :class="getSvcStatusClass(req.status)">{{ getSvcStatusText(req.status) }}</span>
                </div>
              </div>
              <div class="card-body">
                <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div class="info-item">
                    <span class="info-label">问题类型</span>
                    <span class="info-value">{{ req.question || '在线咨询' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">联系人</span>
                    <span class="info-value">{{ req.name || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">联系电话</span>
                    <span class="info-value">{{ req.phone || '-' }}</span>
                  </div>
                </div>
                <div v-if="req.content" class="mt-4">
                  <span class="info-label block mb-2">问题描述</span>
                  <p class="text-gray-600 text-sm">{{ req.content }}</p>
                </div>
                <div v-if="req.reply" class="mt-4 reply-box">
                  <span class="info-label block mb-2">平台回复</span>
                  <p class="text-gray-600 text-sm">{{ req.reply }}</p>
                </div>
              </div>
            </div>

            <div v-if="loading" class="text-center py-16 text-gray-400">加载中...</div>
            <div v-else-if="serviceRequests.length === 0" class="text-center py-16">
              <h3 class="text-lg font-medium text-gray-600 mb-2">暂无客服工单</h3>
              <p class="text-gray-400 mb-6">您还没有提交过客服工单</p>
              <button class="btn-primary" @click="goContact">去咨询客服</button>
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
import { api } from '../api/index'

const router = useRouter()
const userInfo = ref(null)
const loginModalVisible = ref(false)
const activeTab = ref('cooperation')
const loading = ref(false)

// 真实数据：洽谈申请 + 客服工单
const cooperations = ref([])
const serviceRequests = ref([])

/** LocalDateTime 字符串截断展示 */
const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

// ===== 商务合作申请状态（后端 BusinessCooperation.status：0=待处理 1=已跟进 2=已关闭） =====
const getCoopStatusText = (s) => ({ 0: '待处理', 1: '已跟进', 2: '已关闭' })[s] ?? '未知'
const getCoopStatusClass = (s) => ({ 0: 'status-pending', 1: 'status-processing', 2: 'status-closed' })[s] || ''

// ===== 客服工单状态（后端 CustomerServiceRequest.status：0=待处理 1=处理中 2=已关闭 3=已评价） =====
const getSvcStatusText = (s) => ({ 0: '待处理', 1: '处理中', 2: '已关闭', 3: '已评价' })[s] ?? '未知'
const getSvcStatusClass = (s) => ({ 0: 'status-pending', 1: 'status-processing', 2: 'status-closed', 3: 'status-approved' })[s] || ''

const fetchData = async () => {
  if (!userInfo.value) return
  loading.value = true
  try {
    const [coopRes, svcRes] = await Promise.all([
      api.getMyCooperations(),
      api.getMyServiceRequests()
    ])
    cooperations.value = coopRes?.code === 200 ? (coopRes.data || []) : []
    serviceRequests.value = svcRes?.code === 200 ? (svcRes.data || []) : []
  } catch (e) {
    console.error('[MyApplications] fetchData error:', e)
    cooperations.value = []
    serviceRequests.value = []
  } finally {
    loading.value = false
  }
}

const openLoginModal = () => { loginModalVisible.value = true }

const handleLoginModalClose = () => { loginModalVisible.value = false }

const handleLoginSuccess = (user) => {
  userInfo.value = user
  loginModalVisible.value = false
  fetchData()
}

const goToCooperation = () => router.push('/cooperation')
const goContact = () => router.push('/contact')

onMounted(() => {
  const savedUser = localStorage.getItem('userInfo')
  if (savedUser) {
    try {
      userInfo.value = JSON.parse(savedUser)
      fetchData()
    } catch (e) {
      console.error('Failed to parse user info:', e)
    }
  } else {
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

.tab-btn {
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  background: white;
  color: #6b7280;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  transition: all .3s ease;
}

.tab-btn-active {
  background: linear-gradient(135deg, #0d9488 0%, #0891b2 100%);
  color: white;
  border-color: transparent;
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

.type-service {
  color: #2563eb;
  background: #dbeafe;
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

.status-closed {
  background: #f3f4f6;
  color: #6b7280;
}

.card-body {
  padding: 20px;
}

.reply-box {
  background: #f8fafc;
  border-left: 3px solid #0d9488;
  padding: 12px 16px;
  border-radius: 8px;
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
