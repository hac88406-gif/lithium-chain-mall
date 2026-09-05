<template>
  <div class="order-management">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-xl font-semibold text-gray-800">订单管理</h2>
    </div>
    
    <div class="filter-bar flex gap-4 mb-4">
      <select v-model="filterStatus" class="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary">
        <option value="">全部状态</option>
        <option value="pending">待付款</option>
        <option value="paid">待发货</option>
        <option value="shipped">已发货</option>
        <option value="completed">已完成</option>
        <option value="cancelled">已取消</option>
      </select>
    </div>
    
    <TableList 
      :data="orders" 
      :columns="columns"
      :total="total"
      :pagination="pagination"
      :show-delete="false"
      :search-placeholder="searchPlaceholder"
      :loading="loading"
      @edit="handleEdit"
      @search="handleSearch"
      @pagination-change="handlePaginationChange"
    >
      <template #cell-status="{ item }">
        <span :class="getStatusClass(item.status)">{{ item.status }}</span>
      </template>
      <template #actions="{ item }">
        <button 
          v-if="item.status === '待发货'"
          @click="handleShip(item)"
          class="px-3 py-1 text-sm bg-green-500 text-white rounded hover:bg-green-600 transition"
        >
          发货
        </button>
      </template>
    </TableList>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated, watch } from 'vue'
import { adminApi } from '../../api/index'
import TableList from '../../components/admin/TableList.vue'

// ========== 状态：后端原始订单（不写 Mock，完全接真实接口） ==========
const orders = ref([])
const total = ref(0)
const loading = ref(false)
const pagination = reactive({ page: 1, size: 10 })
const filterStatus = ref('')
const searchKeyword = ref('')
const searchPlaceholder = '搜索订单号或客户名称'

const columns = [
  { key: 'id', label: '订单号' },
  { key: 'customer', label: '客户' },
  { key: 'product', label: '商品' },
  { key: 'amount', label: '金额(元)' },
  { key: 'status', label: '状态' },
  { key: 'createTime', label: '创建时间' }
]

// ========== 字段映射：后端 OrderVO → 前端展示格式 ==========
function mapOrder (vo) {
  // 多商品订单：取第一个商品名；如果有多个加 "+N"
  let product = ''
  if (Array.isArray(vo.items) && vo.items.length > 0) {
    product = vo.items[0].productName
    if (vo.items.length > 1) product += ` +${vo.items.length - 1}`
  }
  // createTime 后端给的是 ISO 字符串 "2026-09-02T13:54:57"，换成 "2026-09-02 13:54"
  let t = vo.createTime || ''
  t = t.replace('T', ' ').slice(0, 16)
  return {
    id: vo.orderNo,
    rawId: vo.id,
    customer: vo.username,
    product,
    amount: Number(vo.totalAmount || 0),
    status: vo.statusName || vo.status,
    createTime: t,
    _statusRaw: vo.status // 原始英文状态，内部判断用
  }
}

// ========== 拉取列表 ==========
async function fetchOrders () {
  loading.value = true
  try {
    const params = {
      current: pagination.page,
      size: pagination.size
    }
    if (filterStatus.value) params.status = filterStatus.value
    const resp = await adminApi.getOrders(params)
    const data = resp?.data || resp
    if (data?.records) {
      orders.value = data.records.map(mapOrder)
      total.value = Number(data.total || 0)
    } else {
      orders.value = []
      total.value = 0
    }
  } catch (e) {
    console.error('[OrderManagement] fetch failed:', e)
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// ========== 联动 ==========
onMounted(fetchOrders)
// keep-alive 缓存后从其他菜单切回来走 onActivated，需重新拉数据
onActivated(fetchOrders)
watch(filterStatus, () => { pagination.page = 1; fetchOrders() })

// ========== 状态 badge 样式 ==========
const getStatusClass = (status) => {
  const classes = {
    '待付款': 'px-2 py-1 bg-yellow-100 text-yellow-700 rounded text-sm',
    '待发货': 'px-2 py-1 bg-blue-100 text-blue-700 rounded text-sm',
    '已发货': 'px-2 py-1 bg-purple-100 text-purple-700 rounded text-sm',
    '已完成': 'px-2 py-1 bg-green-100 text-green-700 rounded text-sm',
    '已取消': 'px-2 py-1 bg-gray-100 text-gray-500 rounded text-sm'
  }
  return classes[status] || 'px-2 py-1 bg-gray-100 text-gray-600 rounded text-sm'
}

// ========== 交互 ==========
const handleSearch = (params) => {
  searchKeyword.value = params?.keyword || ''
  pagination.page = params?.page || 1
  pagination.size = params?.size || 10
  fetchOrders()
}

const handlePaginationChange = (params) => {
  pagination.page = params.page
  pagination.size = params.size
  fetchOrders()
}

const handleEdit = (item) => {
  alert(`订单详情：${item.id}`)
}

const handleShip = (item) => {
  // 后端目前暂无发货接口，先保留交互占位
  alert(`订单 ${item.id} 标记为发货（后端待补接口）`)
}
</script>

<style scoped>
.order-management {
  min-height: 100%;
}
</style>
