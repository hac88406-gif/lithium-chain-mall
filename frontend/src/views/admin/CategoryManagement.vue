<template>
  <div class="category-management">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-xl font-semibold text-gray-800">商品分类管理</h2>
      <button @click="showAddModal = true" class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-secondary transition">
        + 添加分类
      </button>
    </div>
    
    <TableList 
      :data="categories" 
      :columns="columns"
      :total="total"
      :pagination="pagination"
      @edit="handleEdit"
      @delete="handleDelete"
      @search="handleSearch"
      @pagination-change="handlePaginationChange"
    />
    
    <div class="modal-overlay" v-if="showAddModal || showEditModal" @click="closeModal">
      <div class="modal-content" @click.stop>
        <h3 class="text-lg font-semibold mb-4">{{ showEditModal ? '编辑分类' : '添加分类' }}</h3>
        <form @submit.prevent="handleSubmit">
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">分类名称</label>
            <input 
              v-model="form.name" 
              type="text"
              required
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
              placeholder="请输入分类名称"
            />
          </div>
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">排序</label>
            <input 
              v-model.number="form.sort" 
              type="number"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
              placeholder="排序号（数字越小越靠前）"
            />
          </div>
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">状态</label>
            <select
              v-model="form.status"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none bg-white"
            >
              <option :value="1">启用（客户端可见）</option>
              <option :value="0">停用（客户端隐藏）</option>
            </select>
          </div>
          <div class="flex gap-3 justify-end">
            <button type="button" @click="closeModal" class="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50">
              取消
            </button>
            <button type="submit" class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-secondary">
              {{ showEditModal ? '保存' : '添加' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated } from 'vue'
import TableList from '../../components/admin/TableList.vue'
import { adminApi } from '../../api/index'

const categories = ref([])
const total = ref(0)
const pagination = reactive({ page: 1, size: 10 })
const keyword = ref('')

const columns = [
  { key: 'id', label: 'ID' },
  { key: 'name', label: '分类名称' },
  { key: 'sort', label: '排序' },
  { key: 'status', label: '状态', render: (v) => v === 1 ? '启用' : '停用' },
  { key: 'createTime', label: '创建时间' }
]

const showAddModal = ref(false)
const showEditModal = ref(false)
const form = reactive({
  id: null,
  name: '',
  sort: 0,
  status: 1
})

const fetchList = async () => {
  try {
    const res = await adminApi.getCategories({
      page: pagination.page,
      size: pagination.size,
      keyword: keyword.value || undefined
    })
    if (res?.code === 200) {
      categories.value = res.data?.records || []
      total.value = res.data?.total || 0
    } else {
      alert(res?.message || '加载分类列表失败')
    }
  } catch (e) {
    console.error('[CategoryManagement] fetchList error:', e)
    alert('加载分类列表失败，请检查网络')
  }
}

const handleSearch = (params) => {
  keyword.value = params?.keyword || ''
  pagination.page = 1
  fetchList()
}

const handlePaginationChange = (params) => {
  pagination.page = params.page
  pagination.size = params.size
  fetchList()
}

const handleEdit = (item) => {
  form.id = item.id
  form.name = item.name
  form.sort = item.sort ?? 0
  form.status = item.status ?? 1
  showEditModal.value = true
}

const handleDelete = async (item) => {
  if (!confirm(`确定删除分类"${item.name}"吗？删除后不可恢复。`)) return
  try {
    const res = await adminApi.deleteCategory(item.id)
    if (res?.code === 200) {
      alert('删除成功')
      fetchList()
    } else {
      alert(res?.message || '删除失败')
    }
  } catch (e) {
    alert('删除失败，请检查网络')
  }
}

const handleSubmit = async () => {
  if (!form.name.trim()) {
    alert('请输入分类名称')
    return
  }
  try {
    let res
    if (showEditModal.value) {
      res = await adminApi.updateCategory(form.id, {
        name: form.name.trim(),
        sort: Number(form.sort) || 0,
        status: form.status
      })
    } else {
      res = await adminApi.createCategory({
        name: form.name.trim(),
        sort: Number(form.sort) || 0,
        status: form.status
      })
    }
    if (res?.code === 200) {
      alert(showEditModal.value ? '保存成功' : '添加成功')
      closeModal()
      fetchList()
    } else {
      alert(res?.message || '操作失败')
    }
  } catch (e) {
    alert('操作失败，请检查网络')
  }
}

const closeModal = () => {
  showAddModal.value = false
  showEditModal.value = false
  form.id = null
  form.name = ''
  form.sort = 0
  form.status = 1
}

onMounted(fetchList)
onActivated(fetchList)
</script>

<style scoped>
.category-management {
  min-height: 100%;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 24px;
  border-radius: 12px;
  width: 400px;
}
</style>