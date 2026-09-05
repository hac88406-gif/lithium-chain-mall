<template>
  <div class="carousel-management">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-xl font-semibold text-gray-800">首页轮播管理</h2>
      <button @click="showAddModal = true" class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-secondary transition">
        + 添加轮播
      </button>
    </div>
    
    <div class="grid grid-cols-4 gap-4">
      <div 
        v-for="item in carousels" 
        :key="item.id" 
        class="carousel-card bg-white rounded-xl shadow-sm overflow-hidden"
      >
        <div class="carousel-image h-40 bg-gray-200 flex items-center justify-center">
          <span class="text-4xl">🖼️</span>
        </div>
        <div class="p-4">
          <h4 class="font-medium text-gray-800 truncate">{{ item.title }}</h4>
          <p class="text-sm text-gray-500 mt-1">{{ item.link }}</p>
          <div class="flex gap-2 mt-3">
            <button 
              @click="handleEdit(item)"
              class="flex-1 px-2 py-1 text-sm bg-blue-500 text-white rounded hover:bg-blue-600"
            >
              编辑
            </button>
            <button 
              @click="handleDelete(item)"
              class="flex-1 px-2 py-1 text-sm bg-red-500 text-white rounded hover:bg-red-600"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <div class="modal-overlay" v-if="showAddModal || showEditModal" @click="closeModal">
      <div class="modal-content" @click.stop>
        <h3 class="text-lg font-semibold mb-4">{{ showEditModal ? '编辑轮播' : '添加轮播' }}</h3>
        <form @submit.prevent="handleSubmit">
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">标题</label>
            <input 
              v-model="form.title" 
              type="text"
              required
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
              placeholder="请输入轮播标题"
            />
          </div>
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">链接地址</label>
            <input 
              v-model="form.link" 
              type="url"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
              placeholder="请输入跳转链接"
            />
          </div>
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">排序</label>
            <input 
              v-model.number="form.sort" 
              type="number"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
              placeholder="排序号"
            />
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
import { adminApi } from '../../api'

// ===================== 数据状态（真实后端：/api/admin/carousels）=====================
const carousels = ref([])
const loading = ref(false)
const showAddModal = ref(false)
const showEditModal = ref(false)
const form = reactive({
  id: null,
  title: '',
  image: '',     // 轮播图片URL（必填，schema.sql carousel.image NOT NULL）
  link: '',      // 点击跳转链接
  sort: 0,
  status: 1,
})

// ===================== 列表拉取 =====================
const fetchList = async () => {
  loading.value = true
  try {
    const res = await adminApi.getCarousels({ current: 1, size: 100 })
    // Result<T> 包装：{ code: 200, data: { records:[...], total } }
    const pageData = res?.code === 200 ? res.data : (res?.data?.records ? res.data : { records: [] })
    const list = pageData.records || []
    // 按 sort 升序 + ID 倒序（与后端 Wrapper 一致，展示时再保证一次稳定）
    list.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0) || (b.id ?? 0) - (a.id ?? 0))
    carousels.value = list
  } catch (e) {
    console.error('[CarouselManagement] fetchList failed:', e)
    ElMessage.error(e?.message || '获取轮播列表失败，请检查后端服务')
    carousels.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)

// ===================== 新增 / 编辑 / 删除 =====================
const handleEdit = (item) => {
  Object.assign(form, {
    id: item.id,
    title: item.title || '',
    image: item.image || '',
    link: item.link || '',
    sort: item.sort ?? 0,
    status: typeof item.status === 'number' ? item.status : 1,
  })
  showEditModal.value = true
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除轮播 "${item.title || '(无标题)'}" 吗？删除后前台轮播图即时生效。`,
      '确认删除', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  } catch { return }
  try {
    await adminApi.deleteCarousel(item.id)
    ElMessage.success('已删除')
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

const handleSubmit = async () => {
  if (!form.title.trim()) { ElMessage.warning('请输入轮播标题'); return }
  if (!form.image.trim()) { ElMessage.warning('请填写轮播图片URL'); return }
  try {
    const payload = { ...form }
    if (showEditModal.value) {
      await adminApi.updateCarousel(form.id, payload)
      ElMessage.success('轮播已更新，前台约30分钟内生效')
    } else {
      await adminApi.createCarousel(payload)
      ElMessage.success('轮播已创建')
    }
    closeModal()
    fetchList()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  }
}

const closeModal = () => {
  showAddModal.value = false
  showEditModal.value = false
  form.id = null
  form.title = ''
  form.image = ''
  form.link = ''
  form.sort = 0
  form.status = 1
}
</script>

<style scoped>
.carousel-management {
  min-height: 100%;
}

.carousel-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.carousel-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(13, 148, 136, 0.10);
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
  width: 440px;
}
</style>