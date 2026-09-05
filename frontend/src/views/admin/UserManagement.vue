<template>
  <div class="user-management">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-xl font-semibold text-gray-800">买家用户管理</h2>
    </div>
    
    <TableList 
      :data="users" 
      :columns="columns"
      :total="total"
      :pagination="pagination"
      :show-delete="false"
      :search-placeholder="searchPlaceholder"
      @edit="handleEdit"
      @search="handleSearch"
      @pagination-change="handlePaginationChange"
    >
      <template #cell-status="{ item }">
        <span :class="getStatusClass(item.status)">{{ item.status }}</span>
      </template>
    </TableList>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import TableList from '../../components/admin/TableList.vue'

const users = ref([
  { id: 1, username: 'zhangsan', phone: '138****1234', email: 'zhangsan@example.com', status: '正常', createTime: '2024-01-01' },
  { id: 2, username: 'lisi', phone: '139****5678', email: 'lisi@example.com', status: '正常', createTime: '2024-01-02' },
  { id: 3, username: 'wangwu', phone: '137****9012', email: 'wangwu@example.com', status: '封禁', createTime: '2024-01-03' },
  { id: 4, username: 'zhaoliu', phone: '136****3456', email: 'zhaoliu@example.com', status: '正常', createTime: '2024-01-04' },
  { id: 5, username: 'qianqi', phone: '135****7890', email: 'qianqi@example.com', status: '正常', createTime: '2024-01-05' }
])

const total = ref(5)
const pagination = reactive({ page: 1, size: 10 })
const searchPlaceholder = '搜索用户名或手机号'

const columns = [
  { key: 'id', label: 'ID' },
  { key: 'username', label: '用户名' },
  { key: 'phone', label: '手机号' },
  { key: 'email', label: '邮箱' },
  { key: 'status', label: '状态' },
  { key: 'createTime', label: '注册时间' }
]

const getStatusClass = (status) => {
  return status === '正常' 
    ? 'px-2 py-1 bg-green-100 text-green-700 rounded text-sm' 
    : 'px-2 py-1 bg-red-100 text-red-700 rounded text-sm'
}

const handleSearch = (params) => {
  pagination.page = params.page
  pagination.size = params.size
}

const handlePaginationChange = (params) => {
  pagination.page = params.page
  pagination.size = params.size
}

const handleEdit = (item) => {
  alert(`查看用户详情: ${item.username}`)
}
</script>

<style scoped>
.user-management {
  min-height: 100%;
}
</style>