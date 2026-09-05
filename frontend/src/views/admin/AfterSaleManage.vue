<template>
  <div class="after-sale-manage">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-xl font-semibold text-gray-800">售后管理</h2>
    </div>

    <!-- 状态筛选 -->
    <div class="flex gap-4 mb-4">
      <select v-model="filterStatus" class="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary" @change="handleFilterChange">
        <option value="">全部状态</option>
        <option :value="0">待审核</option>
        <option :value="1">已同意</option>
        <option :value="2">已拒绝</option>
      </select>
    </div>

    <!-- 售后列表表格 -->
    <div class="bg-white rounded-xl shadow overflow-hidden">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 text-gray-600">
          <tr>
            <th class="px-4 py-3 text-left">售后编号</th>
            <th class="px-4 py-3 text-left">订单号</th>
            <th class="px-4 py-3 text-left">用户</th>
            <th class="px-4 py-3 text-left">类型</th>
            <th class="px-4 py-3 text-left">状态</th>
            <th class="px-4 py-3 text-left">申请时间</th>
            <th class="px-4 py-3 text-left">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="isLoading">
            <td colspan="7" class="px-4 py-10 text-center text-gray-400">加载中...</td>
          </tr>
          <tr v-else-if="list.length === 0">
            <td colspan="7" class="px-4 py-10 text-center text-gray-400">暂无售后记录</td>
          </tr>
          <tr v-for="item in list" :key="item.id" class="border-t border-gray-100 hover:bg-gray-50">
            <td class="px-4 py-3">#{{ item.id }}</td>
            <td class="px-4 py-3">{{ item.orderNo || item.orderId }}</td>
            <td class="px-4 py-3">{{ item.nickname || item.username || ('用户' + item.userId) }}</td>
            <td class="px-4 py-3">{{ typeText(item.afterSaleType) }}</td>
            <td class="px-4 py-3">
              <span :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
            </td>
            <td class="px-4 py-3">{{ formatTime(item.createTime) }}</td>
            <td class="px-4 py-3">
              <!-- 审核按钮：仅待审核状态显示，并绑定权限指令 -->
              <button
                v-if="item.status === 0"
                v-permission="'sys:aftersale:review'"
                class="px-3 py-1 text-sm bg-green-600 text-white rounded hover:bg-green-700 transition"
                @click="openReviewModal(item)"
              >审核</button>
              <button
                v-else
                class="px-3 py-1 text-sm text-gray-500 hover:text-gray-700"
                @click="showDetail(item)"
              >详情</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div class="flex justify-end items-center gap-3 mt-4" v-if="total > pagination.size">
      <button class="px-3 py-1 border rounded disabled:opacity-40" :disabled="pagination.page <= 1" @click="changePage(pagination.page - 1)">上一页</button>
      <span class="text-sm text-gray-600">{{ pagination.page }} / {{ totalPages }}</span>
      <button class="px-3 py-1 border rounded disabled:opacity-40" :disabled="pagination.page >= totalPages" @click="changePage(pagination.page + 1)">下一页</button>
    </div>

    <!-- 审核弹窗 -->
    <div v-if="reviewModalVisible" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="reviewModalVisible = false">
      <div class="bg-white rounded-xl p-6 w-[640px] max-w-[92vw] max-h-[92vh] overflow-y-auto">
        <h3 class="text-lg font-semibold mb-4">售后审核</h3>
        <div class="mb-3 text-sm text-gray-600 space-y-1">
          <p>售后编号：#{{ reviewTarget && reviewTarget.id }}</p>
          <p>订单号：{{ reviewTarget && (reviewTarget.orderNo || reviewTarget.orderId) }}</p>
          <p>类型：{{ typeText(reviewTarget?.afterSaleType) }}</p>
          <p>申请理由：{{ reviewTarget?.reason || '（未填写）' }}</p>
          <!-- 凭证图片 -->
          <div v-if="parseEvidence(reviewTarget?.evidence).length" class="pt-3">
            <p class="mb-2 font-medium">凭证图片（{{ parseEvidence(reviewTarget?.evidence).length }} 张，点击放大查看）：</p>
            <div class="flex flex-wrap gap-3">
              <img
                v-for="(url, idx) in parseEvidence(reviewTarget?.evidence)"
                :key="idx"
                :src="url"
                :alt="'凭证' + (idx+1)"
                class="w-32 h-32 object-cover rounded-lg border border-gray-200 cursor-zoom-in hover:ring-2 hover:ring-green-500 transition shadow-sm hover:shadow-md"
                @click="openLightbox(parseEvidence(reviewTarget?.evidence), idx)"
              />
            </div>
          </div>
        </div>
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-2">审核结果</label>
          <select v-model.number="reviewForm.status" class="w-full px-4 py-2 border border-gray-300 rounded-lg">
            <option :value="1">同意</option>
            <option :value="2">拒绝</option>
          </select>
        </div>
        <div class="mb-4" v-if="reviewForm.status === 2">
          <label class="block text-sm font-medium text-gray-700 mb-2">拒绝原因<span class="text-red-500">*</span></label>
          <textarea
            v-model="reviewForm.rejectReason"
            rows="3"
            maxlength="512"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg"
            placeholder="拒绝时必须填写拒绝原因"
          ></textarea>
        </div>
        <div class="flex justify-end gap-3">
          <button class="px-4 py-2 border rounded-lg text-gray-600 hover:bg-gray-50" @click="reviewModalVisible = false">取消</button>
          <button
            v-permission="'sys:aftersale:review'"
            class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-60"
            :disabled="reviewSubmitting"
            @click="submitReview"
          >{{ reviewSubmitting ? '提交中...' : '确认审核' }}</button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="detailTarget" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="detailTarget = null">
      <div class="bg-white rounded-xl p-6 w-[640px] max-w-[92vw] max-h-[92vh] overflow-y-auto">
        <h3 class="text-lg font-semibold mb-4">售后详情</h3>
        <div class="text-sm text-gray-600 space-y-2">
          <p>售后编号：#{{ detailTarget.id }}</p>
          <p>订单号：{{ detailTarget.orderNo || detailTarget.orderId }}</p>
          <p>用户：{{ detailTarget.nickname || detailTarget.username || ('用户' + detailTarget.userId) }}</p>
          <p>类型：{{ typeText(detailTarget.afterSaleType) }}</p>
          <p>状态：{{ statusText(detailTarget.status) }}</p>
          <p>申请理由：{{ detailTarget.reason || '（未填写）' }}</p>
          <!-- 凭证图片 -->
          <div v-if="parseEvidence(detailTarget.evidence).length" class="pt-3">
            <p class="mb-2 font-medium">凭证图片（{{ parseEvidence(detailTarget.evidence).length }} 张，点击放大查看）：</p>
            <div class="flex flex-wrap gap-3">
              <img
                v-for="(url, idx) in parseEvidence(detailTarget.evidence)"
                :key="idx"
                :src="url"
                :alt="'凭证' + (idx+1)"
                class="w-32 h-32 object-cover rounded-lg border border-gray-200 cursor-zoom-in hover:ring-2 hover:ring-green-500 transition shadow-sm hover:shadow-md"
                @click="openLightbox(parseEvidence(detailTarget.evidence), idx)"
              />
            </div>
          </div>
          <p v-if="detailTarget.status === 2">拒绝原因：{{ detailTarget.rejectReason || '（未填写）' }}</p>
          <p>申请时间：{{ formatTime(detailTarget.createTime) }}</p>
        </div>
        <div class="flex justify-end mt-4">
          <button class="px-4 py-2 border rounded-lg text-gray-600 hover:bg-gray-50" @click="detailTarget = null">关闭</button>
        </div>
      </div>
    </div>

    <!-- 全屏图片预览（lightbox） -->
    <div v-if="lightbox.visible" class="fixed inset-0 bg-black/90 z-[100] flex items-center justify-center" @click.self="closeLightbox">
      <button class="absolute top-4 right-4 text-white text-3xl w-10 h-10 rounded-full hover:bg-white/20 flex items-center justify-center" @click="closeLightbox" title="关闭">×</button>
      <button v-if="lightbox.urls.length > 1" class="absolute left-4 top-1/2 -translate-y-1/2 text-white text-4xl w-12 h-12 rounded-full hover:bg-white/20 flex items-center justify-center" @click.stop="prevLightbox" title="上一张">‹</button>
      <button v-if="lightbox.urls.length > 1" class="absolute right-4 top-1/2 -translate-y-1/2 text-white text-4xl w-12 h-12 rounded-full hover:bg-white/20 flex items-center justify-center" @click.stop="nextLightbox" title="下一张">›</button>
      <img :src="lightbox.urls[lightbox.index]" class="max-w-[92vw] max-h-[88vh] object-contain" @click.stop />
      <p v-if="lightbox.urls.length > 1" class="absolute bottom-6 left-1/2 -translate-x-1/2 text-white/80 text-sm bg-black/40 px-3 py-1 rounded-full">
        {{ lightbox.index + 1 }} / {{ lightbox.urls.length }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onActivated } from 'vue'
import apiModule from '../../api/index.js'
// 售后列表/审核走管理端接口（/api/admin/afterSale/*），必须使用 adminApi；
// 之前误用客户端 api 对象（apiModule.api），其上没有 getAfterSales/reviewAfterSale，
// 调用直接抛 TypeError，页面永远停在“暂无售后记录”。
const api = apiModule.adminApi

const list = ref([])
const total = ref(0)
const isLoading = ref(false)
const filterStatus = ref('')
const pagination = reactive({ page: 1, size: 10 })

// 审核弹窗状态
const reviewModalVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewTarget = ref(null)
const reviewForm = reactive({ status: 1, rejectReason: '' })

// 详情弹窗状态
const detailTarget = ref(null)

// 全屏图片预览（lightbox）
const lightbox = reactive({ visible: false, urls: [], index: 0 })
const openLightbox = (urls, index = 0) => {
  lightbox.urls = urls || []
  lightbox.index = Math.max(0, Math.min(index, lightbox.urls.length - 1))
  lightbox.visible = true
}
const closeLightbox = () => { lightbox.visible = false; lightbox.urls = []; lightbox.index = 0 }
const prevLightbox = () => {
  if (lightbox.urls.length <= 1) return
  lightbox.index = (lightbox.index - 1 + lightbox.urls.length) % lightbox.urls.length
}
const nextLightbox = () => {
  if (lightbox.urls.length <= 1) return
  lightbox.index = (lightbox.index + 1) % lightbox.urls.length
}

const totalPages = computed(() => Math.ceil(total.value / pagination.size) || 1)

const typeText = (type) => ({ 1: '退款', 2: '退货' }[type] || '未知')
const statusText = (status) => ({ 0: '待审核', 1: '已同意', 2: '已拒绝' }[status] || '未知')
const statusClass = (status) => ({
  0: 'px-2 py-1 rounded-full text-xs bg-yellow-100 text-yellow-700',
  1: 'px-2 py-1 rounded-full text-xs bg-green-100 text-green-700',
  2: 'px-2 py-1 rounded-full text-xs bg-red-100 text-red-700'
}[status] || '')

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

/**
 * 把后端 evidence 字段（逗号分隔URL字符串）拆成数组，空值返回 []
 */
const parseEvidence = (evidence) => {
  if (!evidence || typeof evidence !== 'string') return []
  return evidence.split(',').map(s => s.trim()).filter(Boolean)
}

/**
 * 加载售后分页列表（关联订单号、用户信息）
 */
const loadList = async () => {
  isLoading.value = true
  try {
    const params = { pageNum: pagination.page, pageSize: pagination.size }
    // 状态筛选：空字符串表示全部，不传 status
    if (filterStatus.value !== '') {
      params.status = filterStatus.value
    }
    const res = await api.getAfterSales(params)
    if (res.code === 200) {
      // MyBatis-Plus 分页对象：records 为数据列表，total 为总条数
      list.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      alert(res.message || '加载售后列表失败')
    }
  } catch (e) {
    console.error('Failed to load after-sale list:', e)
    alert('加载售后列表失败')
  } finally {
    isLoading.value = false
  }
}

const handleFilterChange = () => {
  pagination.page = 1
  loadList()
}

const changePage = (page) => {
  pagination.page = page
  loadList()
}

/**
 * 打开审核弹窗（仅待审核状态可进入）
 */
const openReviewModal = (item) => {
  reviewTarget.value = item
  reviewForm.status = 1
  reviewForm.rejectReason = ''
  reviewModalVisible.value = true
}

/**
 * 提交审核：仅修改售后单业务状态，不做库存回补、不做真实退款
 */
const submitReview = async () => {
  // 拒绝时拒绝原因必填（后端同样校验）
  if (reviewForm.status === 2 && (!reviewForm.rejectReason || !reviewForm.rejectReason.trim())) {
    alert('拒绝时必须填写拒绝原因')
    return
  }
  reviewSubmitting.value = true
  try {
    const res = await api.reviewAfterSale({
      afterSaleId: reviewTarget.value.id,
      status: reviewForm.status,
      rejectReason: reviewForm.status === 2 ? reviewForm.rejectReason.trim() : null
    })
    if (res.code === 200) {
      alert('审核完成')
      reviewModalVisible.value = false
      loadList()
    } else {
      alert(res.message || '审核失败')
    }
  } catch (e) {
    console.error('Failed to review after-sale:', e)
    alert(e.response && e.response.data && e.response.data.message || '审核失败，请重试')
  } finally {
    reviewSubmitting.value = false
  }
}

/**
 * 查看详情（已审核的售后单）
 */
const showDetail = (item) => {
  detailTarget.value = item
}

onMounted(() => {
  loadList()
})
// keep-alive 缓存后切回该页面时重新拉数据（解决首次进入空白、刷新才有数据的问题）
onActivated(() => {
  loadList()
})
</script>

<style scoped>
.after-sale-manage {
  color: #374151;
}
</style>
