<template>
  <div class="orders-page py-12">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="page-header mb-8">
        <h1 class="page-title">我的订单</h1>
        <p class="page-subtitle">查看订单状态、进行支付或取消操作</p>
      </div>

      <div v-if="!isLoggedIn" class="empty-state py-20">
        <div class="empty-icon">
          <svg class="w-20 h-20 mx-auto text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
          </svg>
        </div>
        <h3 class="text-xl font-semibold text-gray-600 mt-6 mb-3">请先登录</h3>
        <p class="text-gray-500 mb-6">登录后即可查看您的订单</p>
        <button class="btn-primary" @click="openLoginModal">立即登录</button>
      </div>

      <template v-else>
        <div class="tabs mb-6">
          <div 
            v-for="tab in tabs" 
            :key="tab.value"
            class="tab"
            :class="{ active: currentTab === tab.value }"
            @click="switchTab(tab.value)"
          >
            {{ tab.label }}
          </div>
        </div>

        <div v-if="isLoading" class="text-center py-20">
          <div class="inline-block w-10 h-10 border-4 border-green-500 border-t-transparent rounded-full animate-spin"></div>
        </div>

        <div v-else-if="filteredOrders.length === 0" class="empty-state py-20">
          <div class="empty-icon">
            <svg class="w-20 h-20 mx-auto text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
            </svg>
          </div>
          <h3 class="text-xl font-semibold text-gray-600 mt-6 mb-3">暂无订单</h3>
          <p class="text-gray-500 mb-6">快去挑选商品下单吧</p>
          <router-link to="/products" class="btn-primary">去购物</router-link>
        </div>

        <div v-else class="orders-list">
          <div v-for="order in filteredOrders" :key="order.id" class="order-card">
            <div class="order-header">
              <div class="order-info">
                <span class="order-no">订单号：{{ order.orderNo }}</span>
                <span class="order-time">{{ formatTime(order.createTime) }}</span>
              </div>
              <span class="order-status" :class="getStatusClass(order.status)">
                {{ getStatusText(order.status) }}
              </span>
            </div>

            <div class="order-body" v-if="order.items">
              <div v-for="item in order.items" :key="item.id" class="order-product">
                <div class="product-img">
                  <img :src="getImageUrl(item.image)" :alt="item.name" />
                </div>
                <div class="product-info">
                  <h4 class="product-name">{{ item.name }}</h4>
                  <span class="product-spec" v-if="item.spec">{{ item.spec }}</span>
                </div>
                <div class="product-price">¥{{ item.price }}</div>
                <div class="product-qty">×{{ item.quantity }}</div>
              </div>
            </div>
            <div v-else class="order-body" @click="loadOrderDetail(order)">
              <div class="click-hint">点击查看订单详情</div>
            </div>

            <div class="order-footer">
              <div class="order-total">
                共 {{ order.totalQuantity }} 件商品，合计：
                <span class="total-price">¥{{ order.totalAmount }}</span>
              </div>
              <div class="order-actions">
                <!-- 规则1：待付款 → 立即付款 + 取消订单 + 订单详情 -->
                <button 
                  v-if="order.status === 'pending'" 
                  class="btn-pay"
                  @click="payOrder(order.id)"
                >立即付款</button>
                <button 
                  v-if="order.status === 'pending'" 
                  class="btn-cancel"
                  @click="cancelOrder(order.id)"
                >取消订单</button>
                <!-- 规则3：已发货(待收货) → 确认收货 -->
                <button
                  v-if="order.status === 'shipped'"
                  class="btn-confirm"
                  @click="confirmReceive(order.id)"
                >确认收货</button>
                <!-- 申请售后：已付款/已发货/已完成，且没有进行中/已同意的售后（被拒绝的可以重新申请） -->
                <button
                  v-if="(order.status === 'paid' || order.status === 'shipped' || order.status === 'completed') && (!order.afterSaleId || order.afterSaleStatus === 2)"
                  class="btn-aftersale"
                  @click="openAfterSaleModal(order)"
                >申请售后</button>
                <!-- 售后状态标记：不同审核结果显示不同文案 -->
                <span v-if="order.afterSaleStatus === 0" class="aftersale-tag aftersale-pending">售后审核中</span>
                <span v-if="order.afterSaleStatus === 1" class="aftersale-tag aftersale-approved">售后已同意</span>
                <span v-if="order.afterSaleStatus === 2" class="aftersale-tag aftersale-rejected clickable" @click="showRejectReason(order)">
                  已拒绝<small v-if="order.afterSaleRejectReason" class="ml-1">（点击查看）</small>
                </span>
                <!-- 规则1~5：所有状态都保留订单详情 -->
                <button class="btn-detail" @click="loadOrderDetail(order)">订单详情</button>
              </div>
            </div>

            <div v-if="order.showDetail" class="order-detail">
              <div class="detail-section">
                <h4>订单商品</h4>
                <div v-if="order.items">
                  <div v-for="item in order.items" :key="item.id" class="detail-item">
                    <span>{{ item.name }}</span>
                    <span>¥{{ item.price }} × {{ item.quantity }}</span>
                  </div>
                </div>
                <div v-else class="text-gray-500">加载中...</div>
              </div>
              <div class="detail-section" v-if="order.addressName || order.addressDetail">
                <h4>收货地址</h4>
                <p>{{ order.addressName }} {{ order.addressPhone }}</p>
                <p>{{ order.addressDetail }}</p>
              </div>
              <div class="detail-section" v-if="order.remark">
                <h4>备注</h4>
                <p>{{ order.remark }}</p>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 申请售后弹窗 -->
    <div v-if="afterSaleModalVisible" class="modal-overlay" @click.self="closeAfterSaleModal">
      <div class="modal-box">
        <h3 class="modal-title">申请售后</h3>
        <p class="modal-order-info">
          订单号：<strong>{{ afterSaleOrder && afterSaleOrder.orderNo }}</strong>
        </p>
        <div class="modal-field">
          <label class="modal-label">售后类型</label>
          <select v-model="afterSaleForm.afterSaleType" class="modal-input">
            <option :value="1">退款</option>
            <option :value="2">退货</option>
          </select>
        </div>
        <div class="modal-field">
          <label class="modal-label">申请理由</label>
          <textarea
            v-model="afterSaleForm.reason"
            class="modal-input"
            rows="4"
            maxlength="512"
            placeholder="请填写申请理由（最多512字）"
          ></textarea>
        </div>
        <div class="modal-field">
          <label class="modal-label">
            凭证图片（可选，最多 9 张）
          </label>
          <div class="evidence-uploader">
            <!-- 已上传缩略图 -->
            <div v-for="(url, idx) in afterSaleForm.evidence" :key="idx" class="evidence-item">
              <img :src="url" class="evidence-thumb" @click="previewEvidence(url)" />
              <button type="button" class="evidence-remove" @click="removeEvidence(idx)" title="移除">×</button>
            </div>
            <!-- 上传按钮 -->
            <label v-if="afterSaleForm.evidence.length < 9" class="evidence-add">
              <span v-if="evidenceUploading" class="text-xs">上传中...</span>
              <span v-else class="evidence-add-icon">+</span>
              <input type="file" accept="image/*" multiple :disabled="evidenceUploading" @change="onEvidenceChange" hidden />
            </label>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn-modal-cancel" @click="closeAfterSaleModal">取消</button>
          <button class="btn-modal-submit" :disabled="afterSaleSubmitting" @click="submitAfterSale">
            {{ afterSaleSubmitting ? '提交中...' : '提交申请' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 查看拒绝理由弹窗 -->
    <div v-if="rejectReasonModalVisible" class="modal-overlay" @click.self="closeRejectReasonModal">
      <div class="modal-box reject-modal">
        <h3 class="modal-title reject-title">
          <span class="reject-icon">✕</span> 售后申请已被拒绝
        </h3>
        <div class="reject-body">
          <div class="reject-section">
            <div class="reject-label">商家拒绝理由</div>
            <div class="reject-reason-text">{{ currentRejectInfo.reason }}</div>
          </div>
          <div class="reject-section">
            <div class="reject-label">您的申请信息</div>
            <div class="reject-apply-info">
              <div>售后类型：{{ currentRejectInfo.applyType }}</div>
              <div v-if="currentRejectInfo.applyTime">申请时间：{{ currentRejectInfo.applyTime }}</div>
              <div class="reject-apply-reason">申请理由：{{ currentRejectInfo.applyReason }}</div>
            </div>
          </div>
          <p class="reject-tip">如需进一步帮助，可重新提交售后申请或联系人工客服。</p>
        </div>
        <div class="modal-actions">
          <button class="btn-modal-submit" @click="closeRejectReasonModal">我知道了</button>
        </div>
      </div>
    </div>

    <LoginModal
      v-if="loginModalVisible"
      @close="loginModalVisible = false"
      @login="handleLoginSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index'
import LoginModal from '../components/LoginModal.vue'

const router = useRouter()
const orders = ref([])
const currentTab = ref('all')
const isLoading = ref(false)
const loginModalVisible = ref(false)
const userInfo = ref(null)

const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const tabs = [
  { label: '全部订单', value: 'all' },
  { label: '待付款', value: 'pending' },
  { label: '已付款', value: 'paid' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' }
]

const filteredOrders = computed(() => {
  if (currentTab.value === 'all') return orders.value
  return orders.value.filter(o => o.status === currentTab.value)
})

const getImageUrl = (path) => {
  if (!path) return 'https://via.placeholder.com/100x100?text=No+Image'
  if (path.startsWith('http')) return path
  return path
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const getStatusText = (status) => {
  const map = {
    pending: '待付款',
    paid: '已付款',
    shipped: '已发货',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
}

const getStatusClass = (status) => {
  const map = {
    pending: 'status-pending',
    paid: 'status-paid',
    shipped: 'status-shipped',
    completed: 'status-completed',
    cancelled: 'status-cancelled'
  }
  return map[status] || ''
}

const loadOrders = async () => {
  if (!isLoggedIn.value) return
  isLoading.value = true
  try {
    const params = {}
    if (currentTab.value !== 'all') {
      params.status = currentTab.value
    }
    const res = await api.getOrderList(params)
    if (res.code === 200) {
      orders.value = res.data || []
      // 加载售后记录标记（用于控制"申请售后"按钮显隐）
      await loadAfterSaleMarks()
    }
  } catch (e) {
    console.error('Failed to load orders:', e)
  } finally {
    isLoading.value = false
  }
}

// ===== 售后相关 =====
const afterSaleModalVisible = ref(false)
const afterSaleOrder = ref(null)
const afterSaleSubmitting = ref(false)
const afterSaleForm = ref({ afterSaleType: 1, reason: '', evidence: [] })
const evidenceUploading = ref(false)

// ===== 查看拒绝理由弹窗 =====
const rejectReasonModalVisible = ref(false)
const currentRejectInfo = ref({ reason: '', applyReason: '', applyType: '', applyTime: '' })

/**
 * 拉取我的售后列表（按申请时间倒序），把最新一条售后标记塞到对应订单上：
 *   afterSaleId       → 售后单ID（用于区分是否有售后）
 *   afterSaleStatus   → 0待审核 / 1已同意 / 2已拒绝
 *   afterSaleRejectReason → 拒绝理由（仅 status=2 时有值）
 * 模板据此渲染"售后审核中 / 售后已同意 / 已拒绝"标签，并决定是否显示申请售后按钮。
 */
const loadAfterSaleMarks = async () => {
  try {
    const res = await api.getMyAfterSales()
    if (res.code === 200) {
      for (const order of orders.value) {
        // 清空旧标记，每次加载订单都从售后列表里重新匹配最新的一条
        order.afterSaleId = null
        order.afterSaleStatus = null
        order.afterSaleRejectReason = null
        order.afterSaleFull = null   // 完整售后记录（用于弹窗查看详情）
      }
      // 后端已按 create_time DESC 排好序，第一条就是该订单最新的售后记录
      for (const item of res.data || []) {
        const order = orders.value.find(o => o.id === item.orderId)
        if (order && !order.afterSaleId) {
          order.afterSaleId = item.id
          order.afterSaleStatus = item.status
          order.afterSaleRejectReason = item.rejectReason || null
          order.afterSaleFull = item
        }
      }
    }
  } catch (e) {
    console.error('Failed to load after-sale marks:', e)
  }
}

const openAfterSaleModal = (order) => {
  afterSaleOrder.value = order
  afterSaleForm.value = { afterSaleType: 1, reason: '', evidence: [] }
  afterSaleModalVisible.value = true
}

const closeAfterSaleModal = () => {
  afterSaleModalVisible.value = false
  afterSaleOrder.value = null
}

/** 点击"已拒绝"标签：弹出弹窗显示拒绝理由 + 申请信息 */
const showRejectReason = (order) => {
  const full = order.afterSaleFull || {}
  const typeMap = { 1: '退款', 2: '退货' }
  currentRejectInfo.value = {
    reason: order.afterSaleRejectReason || '商家未填写拒绝理由',
    applyReason: full.reason || '(未填写申请理由)',
    applyType: typeMap[full.afterSaleType] || '售后',
    applyTime: full.createTime ? formatTime(full.createTime) : ''
  }
  rejectReasonModalVisible.value = true
}
const closeRejectReasonModal = () => {
  rejectReasonModalVisible.value = false
}

// ===== 凭证图片上传 =====
const onEvidenceChange = async (e) => {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  // 最多 9 张，减去已有
  const remaining = 9 - afterSaleForm.value.evidence.length
  const toUpload = files.slice(0, remaining)
  if (files.length > remaining) {
    alert(`最多上传 9 张，已自动截取前 ${remaining} 张`)
  }
  evidenceUploading.value = true
  for (const file of toUpload) {
    try {
      const res = await api.uploadAttachment(file)
      // 响应拦截器已返回后端 body：{ code, message, data }，data 为图片 URL 字符串
      const url = res?.data?.url || res?.data
      if (res?.code === 200 && url) {
        afterSaleForm.value.evidence.push(url)
      } else {
        alert(`图片上传失败：${res?.message || '未知错误'}`)
      }
    } catch (err) {
      console.error('upload evidence failed:', err)
      const msg = err?.response?.data?.message || err?.message || '网络错误'
      alert(`图片上传失败：${msg}`)
    }
  }
  evidenceUploading.value = false
  // 清空 input，否则同一文件二次选择不会触发 change
  e.target.value = ''
}

const removeEvidence = (idx) => {
  afterSaleForm.value.evidence.splice(idx, 1)
}

const previewEvidence = (url) => {
  // 简易预览：新窗口打开
  window.open(url, '_blank')
}

const submitAfterSale = async () => {
  if (!afterSaleForm.value.reason || !afterSaleForm.value.reason.trim()) {
    alert('请填写申请理由')
    return
  }
  afterSaleSubmitting.value = true
  try {
    const res = await api.applyAfterSale({
      orderId: afterSaleOrder.value.id,
      afterSaleType: afterSaleForm.value.afterSaleType,
      reason: afterSaleForm.value.reason.trim(),
      evidence: afterSaleForm.value.evidence.join(',') || null
    })
    if (res.code === 200) {
      alert('售后申请提交成功，请等待审核')
      closeAfterSaleModal()
      // 刷新页面数据（重新加载订单与售后标记）
      loadOrders()
    } else {
      alert(res.message || '提交失败')
    }
  } catch (e) {
    console.error('Failed to submit after-sale:', e)
    alert(e.response && e.response.data && e.response.data.message || '提交失败，请重试')
  } finally {
    afterSaleSubmitting.value = false
  }
}

const switchTab = (tab) => {
  currentTab.value = tab
  loadOrders()
}

const loadOrderDetail = async (order) => {
  if (order.showDetail) {
    order.showDetail = false
    return
  }
  
  order.showDetail = true
  
  if (order.items && order.items.length > 0) return
  
  try {
    const res = await api.getOrderById(order.id)
    if (res.code === 200) {
      order.items = res.data.items || []
      if (res.data.order) {
        order.remark = res.data.order.remark
        order.addressName = res.data.order.addressName
        order.addressPhone = res.data.order.addressPhone
        order.addressDetail = res.data.order.addressDetail
      }
    }
  } catch (e) {
    console.error('Failed to load order detail:', e)
    order.showDetail = false
  }
}

const payOrder = async (id) => {
  if (!confirm('确认支付该订单？')) return
  try {
    // 第 1 步：发起预下单（真实支付打通），返回支付信息，订单仍为 pending
    const res = await api.payOrder(id)
    if (res.code !== 200) {
      alert(res.message || '发起支付失败')
      return
    }
    const payInfo = res.data || {}
    if (!payInfo.paymentNo) {
      alert('支付流水创建失败，请重试')
      return
    }
    // 第 2 步：直接走模拟网关验签回调（本地演示，跳过原生弹窗）
    const payRes = await api.simulatePay(payInfo.paymentNo)
    loadOrders()
  } catch (e) {
    console.error('Failed to pay order:', e)
    alert('支付失败，请重试')
  }
}

const cancelOrder = async (id) => {
  if (!confirm('确定要取消该订单吗？')) return
  try {
    const res = await api.cancelOrder(id)
    if (res.code === 200) {
      alert('订单已取消')
      loadOrders()
    } else {
      alert(res.message || '取消失败')
    }
  } catch (e) {
    console.error('Failed to cancel order:', e)
    alert('取消失败，请重试')
  }
}

const confirmReceive = async (id) => {
  if (!confirm('确认已收到商品？')) return
  try {
    const res = await api.confirmReceive(id)
    if (res.code === 200) {
      alert('已确认收货')
      loadOrders()
    } else {
      alert(res.message || '操作失败')
    }
  } catch (e) {
    console.error('Failed to confirm receive:', e)
    const msg = e?.response?.data?.message || e?.message || '操作失败，请重试'
    alert(msg)
  }
}

const openLoginModal = () => {
  loginModalVisible.value = true
}

const handleLoginSuccess = (user) => {
  userInfo.value = user
  loginModalVisible.value = false
  loadOrders()
}

onMounted(() => {
  const savedUser = localStorage.getItem('userInfo')
  if (savedUser) {
    try {
      userInfo.value = JSON.parse(savedUser)
    } catch (e) {}
  }
  loadOrders()
})
</script>

<style scoped>
.orders-page {
  background: #f9fafb;
  min-height: calc(100vh - 64px);
}

.page-header {
  text-align: center;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
}

.page-subtitle {
  color: #6b7280;
  margin-top: 8px;
}

.empty-state {
  text-align: center;
}

.btn-primary {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  color: white;
  border: none;
  padding: 12px 32px;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  display: inline-block;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(5, 150, 105, 0.3);
}

.tabs {
  display: flex;
  gap: 8px;
  background: white;
  border-radius: 12px;
  padding: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.tab {
  flex: 1;
  text-align: center;
  padding: 10px 16px;
  border-radius: 8px;
  cursor: pointer;
  color: #6b7280;
  font-size: 0.95rem;
  transition: all 0.2s ease;
}

.tab:hover {
  color: #059669;
}

.tab.active {
  background: #059669;
  color: white;
  font-weight: 500;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f9fafb;
  border-bottom: 1px solid #f3f4f6;
}

.order-info {
  display: flex;
  gap: 16px;
  align-items: center;
}

.order-no {
  color: #374151;
  font-weight: 500;
}

.order-time {
  color: #9ca3af;
  font-size: 0.85rem;
}

.order-status {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 500;
}

.status-pending {
  background: #fef3c7;
  color: #d97706;
}

.status-paid {
  background: #dbeafe;
  color: #2563eb;
}

.status-shipped {
  background: #e0e7ff;
  color: #6366f1;
}

.status-completed {
  background: #dcfce7;
  color: #16a34a;
}

.status-cancelled {
  background: #f3f4f6;
  color: #6b7280;
}

.order-body {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-product {
  display: grid;
  grid-template-columns: 60px 1fr 80px 60px;
  gap: 12px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f9fafb;
}

.order-product:last-child {
  border-bottom: none;
}

.product-img {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
}

.product-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  font-size: 0.95rem;
  color: #1f2937;
  font-weight: 500;
}

.product-spec {
  font-size: 0.85rem;
  color: #9ca3af;
}

.product-price {
  text-align: center;
  color: #059669;
  font-weight: 500;
}

.product-qty {
  text-align: center;
  color: #6b7280;
}

.click-hint {
  text-align: center;
  color: #9ca3af;
  font-size: 0.9rem;
  padding: 8px;
  cursor: pointer;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-top: 1px solid #f3f4f6;
}

.order-total {
  color: #6b7280;
}

.total-price {
  color: #dc2626;
  font-size: 1.2rem;
  font-weight: 700;
  margin-left: 4px;
}

.order-actions {
  display: flex;
  gap: 12px;
}

.order-actions button {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-pay {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  color: white;
  border: none;
}

.btn-pay:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3);
}

.btn-cancel {
  background: white;
  color: #6b7280;
  border: 1px solid #d1d5db;
}

.btn-cancel:hover {
  background: #f9fafb;
}

.btn-confirm {
  background: #059669;
  color: white;
  border: none;
}

.btn-confirm:hover {
  background: #047857;
}

.btn-detail {
  background: white;
  color: #059669;
  border: 1px solid #059669;
}

.btn-detail:hover {
  background: #f0fdf4;
}

/* 申请售后按钮 */
.btn-aftersale {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fcd34d;
}

.btn-aftersale:hover {
  background: #fde68a;
}

/* 售后状态标记（基础样式，颜色由状态变体覆盖） */
.aftersale-tag {
  align-self: center;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 0.8rem;
  white-space: nowrap;
}
.aftersale-tag.aftersale-pending {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fcd34d;
}
.aftersale-tag.aftersale-approved {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #86efac;
}
.aftersale-tag.aftersale-rejected {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fca5a5;
}
/* 售后标签可点击时的指针光标 + hover 效果 */
.aftersale-tag.clickable {
  cursor: pointer;
  transition: all 0.15s ease;
}
.aftersale-tag.clickable:hover {
  filter: brightness(0.92);
  box-shadow: 0 1px 4px rgba(239, 68, 68, 0.25);
}

/* 售后申请弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-box {
  width: 90%;
  max-width: 440px;
  background: white;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.modal-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 8px;
}

.modal-order-info {
  font-size: 0.9rem;
  color: #6b7280;
  margin-bottom: 16px;
}

.modal-field {
  margin-bottom: 14px;
}

.modal-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}

.modal-input {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 0.9rem;
  outline: none;
  resize: vertical;
  box-sizing: border-box;
}

.modal-input:focus {
  border-color: #059669;
  box-shadow: 0 0 0 2px rgba(5, 150, 105, 0.15);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.btn-modal-cancel {
  padding: 8px 18px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  background: white;
  color: #6b7280;
  cursor: pointer;
}

.btn-modal-cancel:hover {
  background: #f9fafb;
}

.btn-modal-submit {
  padding: 8px 18px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  color: white;
  cursor: pointer;
}

.btn-modal-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ===== 拒绝理由弹窗专属样式 ===== */
.reject-modal {
  max-width: 440px;
}
.reject-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #991b1b;
  border-bottom: 1px solid #fecaca;
  padding-bottom: 12px;
}
.reject-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px; height: 24px;
  background: #ef4444;
  color: white;
  border-radius: 50%;
  font-size: 14px;
  font-weight: bold;
}
.reject-body {
  padding: 16px 4px 4px;
}
.reject-section {
  margin-bottom: 16px;
}
.reject-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
  margin-bottom: 6px;
}
.reject-reason-text {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 12px;
  color: #991b1b;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}
.reject-apply-info {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  font-size: 13px;
  color: #374151;
  line-height: 1.8;
}
.reject-apply-reason {
  color: #6b7280;
  line-height: 1.6;
}
.reject-tip {
  font-size: 12px;
  color: #9ca3af;
  margin: 4px 0 0;
}

/* 凭证图片上传区 */
.evidence-uploader {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.evidence-item {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.evidence-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: zoom-in;
  display: block;
}

.evidence-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border: none;
  cursor: pointer;
  font-size: 14px;
  line-height: 20px;
  text-align: center;
  padding: 0;
}

.evidence-add {
  width: 72px;
  height: 72px;
  border: 1.5px dashed #d1d5db;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #9ca3af;
  transition: all 0.2s;
}

.evidence-add:hover {
  border-color: #059669;
  color: #059669;
  background: #f0fdf4;
}

.evidence-add-icon {
  font-size: 22px;
  line-height: 1;
}

.order-detail {
  padding: 16px 20px;
  background: #f9fafb;
  border-top: 1px solid #f3f4f6;
}

.detail-section {
  margin-bottom: 12px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-section h4 {
  font-size: 0.9rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.detail-section p {
  color: #6b7280;
  font-size: 0.9rem;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  color: #6b7280;
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .tabs {
    flex-wrap: wrap;
  }
  
  .order-footer {
    flex-direction: column;
    gap: 12px;
  }
  
  .order-actions {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .order-product {
    grid-template-columns: 50px 1fr;
  }
  
  .product-price, .product-qty {
    grid-column: 2;
    text-align: left;
  }
}
</style>