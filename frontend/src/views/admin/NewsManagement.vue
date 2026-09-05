<template>
  <div class="news-management">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-xl font-semibold text-gray-800">资讯管理</h2>
      <button @click="showAddModal = true" class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-secondary transition">
        + 添加资讯
      </button>
    </div>
    
    <TableList 
      :data="newsList" 
      :columns="columns"
      :total="total"
      :pagination="pagination"
      @edit="handleEdit"
      @delete="handleDelete"
      @search="handleSearch"
      @pagination-change="handlePaginationChange"
    >
      <template #cell-status="{ item }">
        <span :class="getStatusClass(item.status)">{{ item.status }}</span>
      </template>
    </TableList>
    
    <div class="modal-overlay" v-if="showAddModal || showEditModal" @click="closeModal">
      <div class="modal-content large" @click.stop>
        <h3 class="text-lg font-semibold mb-4">{{ showEditModal ? '编辑资讯' : '添加资讯' }}</h3>
        <form @submit.prevent="handleSubmit">
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">标题</label>
            <input 
              v-model="form.title" 
              type="text"
              required
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
              placeholder="请输入资讯标题"
            />
          </div>
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">分类</label>
            <select 
              v-model="form.category"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
            >
              <option value="">请选择分类</option>
              <option value="行业动态">行业动态</option>
              <option value="公司新闻">公司新闻</option>
              <option value="技术资讯">技术资讯</option>
            </select>
          </div>
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">摘要</label>
            <textarea 
              v-model="form.summary" 
              rows="3"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
              placeholder="请输入资讯摘要"
            ></textarea>
          </div>
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">内容</label>
            <textarea 
              v-model="form.content" 
              rows="6"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
              placeholder="请输入资讯内容"
            ></textarea>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import TableList from '../../components/admin/TableList.vue'
import { adminApi } from '../../api'

// ===================== 数据状态（真实后端返回） =====================
const newsList = ref([])
const total = ref(0)
const pagination = reactive({ page: 1, size: 10 })
const searchKeyword = ref('')
const searchCategory = ref('')

// 分类下拉选项：与 schema.sql news.category 枚举种子对齐
const categoryOptions = [
  { label: '全部分类', value: '' },
  { label: '行业动态', value: '行业动态' },
  { label: '公司新闻', value: '公司新闻' },
  { label: '技术资讯', value: '技术资讯' },
]
const statusOptions = [
  { label: '全部状态', value: null },
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
]

const columns = [
  { key: 'id', label: 'ID', width: 70 },
  { key: 'title', label: '标题', minWidth: 240 },
  { key: 'category', label: '分类', width: 120 },
  { key: 'author', label: '作者/来源', width: 110 },
  { key: 'viewCount', label: '浏览量', width: 90 },
  {
    key: 'status', label: '状态', width: 90,
    render: (row) => row.status === 1
      ? '<span class="pill pill-green">已发布</span>'
      : '<span class="pill pill-gray">草稿</span>'
  },
  {
    key: 'createTime', label: '创建时间', width: 180,
    render: (row) => (row.createTime || '').replace('T', ' ').substring(0, 19)
  },
]

// ===================== 模态 =====================
const showAddModal = ref(false)
const showEditModal = ref(false)
const loading = ref(false)
const form = reactive({
  id: null,
  title: '',
  category: '行业动态',
  summary: '',
  content: '',
  coverImage: '',
  author: '',
  status: 1,
  sort: 0,
})

// ===================== 工具方法 =====================
const statusText = (status) => status === 1 ? '已发布' : '草稿'

// ===================== 列表拉取 =====================
const fetchList = async () => {
  loading.value = true
  try {
    const res = await adminApi.getNews({
      current: pagination.page,
      size: pagination.size,
      keyword: searchKeyword.value.trim() || undefined,
      category: searchCategory.value || undefined,
    })
    // Response 结构：{ code: 200, data: { records, total, pages, current, size } }
    // Result<T> 包装时 data 里面就是 IPage<News>
    const pageData = res?.code === 200 ? res.data : (res?.data?.records ? res.data : { records: [], total: 0 })
    const records = pageData.records || []
    const totalCount = typeof pageData.total === 'number' ? pageData.total : records.length
    newsList.value = records
    total.value = totalCount
  } catch (e) {
    console.error('[NewsManagement] fetchList failed:', e)
    ElMessage.error(e?.message || '获取资讯列表失败，请检查后端服务')
    newsList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)

// ===================== 搜索 / 分页 =====================
const handleSearch = (params) => {
  pagination.page = params.page
  pagination.size = params.size
  fetchList()
}

const handlePaginationChange = (params) => {
  pagination.page = params.page
  pagination.size = params.size
  fetchList()
}

const onKeywordOrCategoryChange = () => {
  pagination.page = 1
  fetchList()
}

// ===================== 新增 / 编辑 =====================
const openAdd = () => {
  resetForm()
  showAddModal.value = true
}

const handleEdit = (item) => {
  Object.assign(form, {
    id: item.id,
    title: item.title || '',
    category: item.category || '行业动态',
    summary: item.summary || '',
    content: item.content || '',
    coverImage: item.coverImage || '',
    author: item.author || '',
    status: typeof item.status === 'number' ? item.status : (item.status === '已发布' ? 1 : 0),
    sort: item.sort ?? 0,
  })
  showEditModal.value = true
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除资讯 "${item.title}" 吗？删除后不可恢复。`, '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
  } catch { return }
  try {
    await adminApi.deleteNews(item.id)
    ElMessage.success('已删除')
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

const handleSubmit = async () => {
  if (!form.title.trim()) { ElMessage.warning('请输入资讯标题'); return }
  if (!form.category.trim()) { ElMessage.warning('请选择分类'); return }

  try {
    const payload = { ...form }
    // 后端兼容：字符串 "已发布"/"草稿" 转数字
    if (typeof payload.status === 'string') payload.status = payload.status === '已发布' ? 1 : 0
    if (showEditModal.value) {
      await adminApi.updateNews(form.id, payload)
      ElMessage.success('资讯已更新')
    } else {
      await adminApi.createNews(payload)
      ElMessage.success('资讯已创建')
    }
    closeModal()
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  }
}

const resetForm = () => {
  form.id = null
  form.title = ''
  form.category = '行业动态'
  form.summary = ''
  form.content = ''
  form.coverImage = ''
  form.author = ''
  form.status = 1
  form.sort = 0
}

const closeModal = () => {
  showAddModal.value = false
  showEditModal.value = false
  resetForm()
}
</script>

<style scoped>
.news-management {
  min-height: 100%;
}

.pill {
  display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 12px; line-height: 18px;
}
.pill-green { background: rgba(13,148,136,.10); color: #0d9488; }
.pill-gray  { background: rgba(100,116,139,.10); color: #475569; }

.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 24px;
  border-radius: 12px;
  width: 560px;
}

.modal-content.large {
  width: 680px;
}
</style>