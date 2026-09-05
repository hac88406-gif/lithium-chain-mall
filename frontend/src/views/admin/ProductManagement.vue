<template>
  <div class="pm-root">
    <!-- ========= 头部操作栏 ========= -->
    <div class="pm-header">
      <div>
        <h2 class="pm-title">商品管理</h2>
        <p class="pm-sub">管理平台在售锂电商品，支持新增、编辑、上下架、图片上传</p>
      </div>
      <el-button type="primary" size="large" class="pm-add-btn" @click="openAdd">
        <el-icon style="margin-right:6px"><Plus /></el-icon>
        新增商品
      </el-button>
    </div>

    <!-- ========= 筛选卡 ========= -->
    <div class="pm-filter-card">
      <div class="pm-filter-row">
        <el-input
          v-model="filter.name"
          placeholder="搜索商品名称..."
          clearable
          class="pm-filter-input pm-input--lg"
          size="large"
          :prefix-icon="Search"
          @keyup.enter="onSearch"
        />
        <el-select
          v-model="filter.categoryId"
          placeholder="全部分类"
          clearable
          size="large"
          class="pm-filter-select"
        >
          <el-option
            v-for="c in categoryOptions"
            :key="c.id"
            :label="c.name"
            :value="c.id"
          />
        </el-select>
        <el-select
          v-model="filter.status"
          placeholder="全部状态"
          clearable
          size="large"
          class="pm-filter-select"
        >
          <el-option label="上架" :value="'1'" />
          <el-option label="下架" :value="'0'" />
        </el-select>
        <el-button size="large" type="primary" class="pm-filter-btn" @click="onSearch">
          查询
        </el-button>
        <el-button size="large" class="pm-filter-btn" @click="onReset">重置</el-button>
      </div>
    </div>

    <!-- ========= 商品列表 ========= -->
    <div class="pm-table-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        style="width: 100%"
        class="pm-table"
        :header-cell-style="tableHeaderStyle"
      >
        <el-table-column label="序号" width="80" align="center">
          <template #default="{ $index }">
            {{ (pagination.current - 1) * pagination.size + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="pm-product-cell">
              <div class="pm-product-thumb">
                <img :src="resolveImage(row.imageUrl)" :alt="row.name" />
              </div>
              <div class="pm-product-info">
                <div class="pm-product-name">{{ row.name }}</div>
                <div class="pm-product-desc">{{ truncate(row.description, 40) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120" align="center" prop="categoryName">
          <template #default="{ row }">
            <el-tag size="default" effect="light" class="pm-tag-category">{{ row.categoryName || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="价格(¥)" width="130" align="right" prop="price">
          <template #default="{ row }">
            <span class="pm-price">{{ formatMoney(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="100" align="center">
          <template #default="{ row }">
            <span :class="['pm-stock', row.stock < 50 ? 'pm-stock--low' : '']">
              {{ row.stock ?? 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="销量" width="100" align="center" prop="sales" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === '1'"
              inline-prompt
              active-text="上架"
              inactive-text="下架"
              @change="(val) => onToggleStatus(row, val)"
              style="--el-switch-on-color:#0d9488;--el-switch-off-color:#94a3b8"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === '1'"
              size="small"
              type="warning"
              link
              @click="onToggleStatus(row, false)"
            >下架</el-button>
            <el-button
              v-else
              size="small"
              type="success"
              link
              @click="onToggleStatus(row, true)"
            >上架</el-button>
            <el-popconfirm
              title="确认删除该商品？删除后客户端也将不再展示。"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="onDelete(row)"
            >
              <template #reference>
                <el-button size="small" type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pm-pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          background
          class="pm-pagination"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>

    <!-- ========= 新增 / 编辑 弹窗 ========= -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '新增商品'"
      width="720px"
      class="pm-dialog"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="90px"
        label-position="right"
        class="pm-form"
      >
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="商品名称" prop="name">
              <el-input
                v-model="form.name"
                placeholder="例：宁德时代 CATL-48100 磷酸铁锂电芯 100Ah"
                maxlength="80"
                show-word-limit
                size="large"
                class="pm-input"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品分类" prop="categoryId">
              <el-select
                v-model="form.categoryId"
                placeholder="请选择分类"
                size="large"
                class="w-full"
              >
                <el-option
                  v-for="c in categoryOptions"
                  :key="c.id"
                  :label="c.name"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品状态" prop="status">
              <el-radio-group v-model="form.status" size="large">
                <el-radio-button :value="1">上架销售</el-radio-button>
                <el-radio-button :value="0">暂不上架</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销售价(¥)" prop="price">
              <el-input-number
                v-model="form.price"
                :min="0"
                :precision="2"
                :step="10"
                size="large"
                controls-position="right"
                class="w-full"
                placeholder="例：680.00"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存数量" prop="stock">
              <el-input-number
                v-model="form.stock"
                :min="0"
                :precision="0"
                :step="10"
                size="large"
                controls-position="right"
                class="w-full"
                placeholder="例：500"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                placeholder="请输入商品详细描述（电压/容量/循环次数/适用场景/质保等）"
                class="pm-input"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品图片" prop="imageUrl">
              <el-upload
                class="pm-uploader"
                :show-file-list="false"
                :before-upload="beforeImageUpload"
                :http-request="handleImageUpload"
                accept="image/*"
                drag
              >
                <div v-if="form.imageUrl" class="pm-upload-preview">
                  <img :src="resolveImage(form.imageUrl)" alt="商品主图" />
                  <div class="pm-upload-mask">
                    <el-icon class="pm-upload-icon"><Refresh /></el-icon>
                    <span>点击重新上传</span>
                  </div>
                </div>
                <div v-else class="pm-upload-placeholder">
                  <el-icon class="pm-upload-icon"><Plus /></el-icon>
                  <div class="pm-upload-tip-1">点击或拖拽图片到此处上传</div>
                  <div class="pm-upload-tip-2">支持 JPG / PNG / WEBP，单张 ≤ 5MB，建议 1:1 方形</div>
                </div>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button size="large" @click="dialogVisible = false">取消</el-button>
        <el-button
          size="large"
          type="primary"
          class="pm-submit-btn"
          :loading="submitting"
          @click="onSubmit"
        >
          {{ isEdit ? '保存修改' : '创建商品' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Refresh } from '@element-plus/icons-vue'
import { adminApi, api as clientApi } from '../../api/index'

/* ===================== 响应式变量 ===================== */
const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const categoryOptions = ref([])
const filter = reactive({ name: '', categoryId: null, status: '' })
const pagination = reactive({ current: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  name: '',
  categoryId: null,
  status: 1,
  price: null,
  stock: 0,
  description: '',
  imageUrl: ''
})
const formRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [
    { required: true, message: '请输入销售价格', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (v == null || v < 0 ? cb(new Error('价格不能小于 0')) : cb()),
      trigger: 'blur'
    }
  ],
  stock: [
    { required: true, message: '请输入库存数量', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (v == null || v < 0 ? cb(new Error('库存不能小于 0')) : cb()),
      trigger: 'blur'
    }
  ],
  description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }]
}

/* ===================== 工具函数 ===================== */
const tableHeaderStyle = () => ({
  background: '#F8FAFC',
  color: '#334155',
  fontWeight: 600,
  fontSize: '13px'
})

const formatMoney = (v) => {
  if (v == null) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const formatDate = (d) => {
  if (!d) return '-'
  const t = new Date(d)
  if (Number.isNaN(t.getTime())) return String(d).slice(0, 16).replace('T', ' ')
  const pad = (n) => String(n).padStart(2, '0')
  return `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())} ${pad(t.getHours())}:${pad(t.getMinutes())}`
}

const truncate = (s, n) => {
  if (!s) return '-'
  return s.length > n ? s.slice(0, n) + '…' : s
}

const resolveImage = (src) => {
  if (!src) return 'data:image/svg+xml;utf8,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><rect width="100%" height="100%" fill="#F1F5F9"/><text x="50%" y="50%" fill="#94A3B8" font-family="Arial" font-size="14" text-anchor="middle" dominant-baseline="middle">无图片</text></svg>')
  if (src.startsWith('http') || src.startsWith('data:')) return src
  // 兼容 /xxx 相对路径：不加前缀直接使用
  return src
}

/* ===================== 数据加载 ===================== */
const loadCategories = async () => {
  try {
    // 管理端优先用 /admin/category/page，失败回退到公开 /client/categories
    let list = []
    try {
      const res = await adminApi.getCategories({ current: 1, size: 100 })
      if (res?.code === 200) {
        const data = res.data || {}
        list = data.records || data.list || []
      }
    } catch (_) { /* ignore */ }
    if (!list.length) {
      const res2 = await clientApi.getCategories()
      if (res2?.code === 200) list = res2.data || []
    }
    categoryOptions.value = list.map((c) => ({ id: c.id, name: c.name })).filter((c) => c.id)
  } catch (e) {
    console.error('[ProductManagement] 分类加载失败：', e)
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      name: filter.name || undefined,
      categoryId: filter.categoryId || undefined,
      status: filter.status || undefined
    }
    const res = await adminApi.getProducts(params)
    if (res?.code === 200) {
      const data = res.data || {}
      tableData.value = data.records || data.list || []
      pagination.total = Number(data.total) || 0
    } else {
      ElMessage.error(res?.message || '商品列表加载失败')
    }
  } catch (e) {
    console.error('[ProductManagement] 列表加载失败：', e)
    ElMessage.error('商品列表加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/* ===================== 筛选交互 ===================== */
const onSearch = () => {
  pagination.current = 1
  loadList()
}
const onReset = () => {
  filter.name = ''
  filter.categoryId = null
  filter.status = ''
  onSearch()
}

/* ===================== 增删改查操作 ===================== */
const resetForm = () => {
  form.id = null
  form.name = ''
  form.categoryId = null
  form.status = 1
  form.price = null
  form.stock = 0
  form.description = ''
  form.imageUrl = ''
  formRef.value?.clearValidate?.()
}

const openAdd = () => {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
}

const openEdit = async (row) => {
  resetForm()
  try {
    const res = await adminApi.getProduct(row.id)
    const data = res?.data || row
    form.id = data.id
    form.name = data.name
    form.categoryId = data.categoryId
    form.status = (data.status === '1' || data.status === 1) ? 1 : 0
    form.price = Number(data.price) || null
    form.stock = Number(data.stock) || 0
    form.description = data.description || ''
    form.imageUrl = data.imageUrl || data.image || ''
    isEdit.value = true
    dialogVisible.value = true
  } catch (e) {
    console.error('[ProductManagement] 详情加载失败：', e)
    // 降级：直接用列表行数据填表单
    form.id = row.id
    form.name = row.name
    form.categoryId = row.categoryId
    form.status = (row.status === '1' || row.status === 1) ? 1 : 0
    form.price = Number(row.price) || null
    form.stock = Number(row.stock) || 0
    form.description = row.description || ''
    form.imageUrl = row.imageUrl || row.image || ''
    isEdit.value = true
    dialogVisible.value = true
  }
}

const onSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (_) {
    return
  }
  submitting.value = true
  try {
    const payload = {
      id: form.id || undefined,
      name: form.name.trim(),
      categoryId: form.categoryId,
      status: form.status,
      price: form.price,
      stock: form.stock,
      description: form.description.trim(),
      imageUrl: form.imageUrl || undefined
    }
    let res
    if (isEdit.value) {
      res = await adminApi.updateProduct(payload)
    } else {
      res = await adminApi.createProduct(payload)
    }
    if (res?.code === 200) {
      ElMessage.success(isEdit.value ? '商品已更新' : '商品创建成功')
      dialogVisible.value = false
      loadList()
    } else {
      ElMessage.error(res?.message || '保存失败')
    }
  } catch (e) {
    console.error('[ProductManagement] 保存失败：', e)
    ElMessage.error(e?.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const onToggleStatus = async (row, val) => {
  const act = val ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确认将「${row.name}」${act}？`, `${act}确认`, {
      confirmButtonText: `立即${act}`,
      cancelButtonText: '取消',
      type: val ? 'success' : 'warning'
    })
  } catch (_) {
    // 取消时把 switch 恢复
    row.status = val ? '0' : '1'
    return
  }
  try {
    const res = val
      ? await adminApi.onShelfProduct(row.id)
      : await adminApi.offShelfProduct(row.id)
    if (res?.code === 200) {
      ElMessage.success(`${act}成功`)
      row.status = val ? '1' : '0'
    } else {
      row.status = val ? '0' : '1'
      ElMessage.error(res?.message || `${act}失败`)
    }
  } catch (e) {
    row.status = val ? '0' : '1'
    ElMessage.error(`${act}失败，请稍后重试`)
  }
}

const onDelete = async (row) => {
  try {
    const res = await adminApi.deleteProduct(row.id)
    if (res?.code === 200) {
      ElMessage.success('已删除')
      loadList()
    } else {
      ElMessage.error(res?.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败，请稍后重试')
  }
}

/* ===================== 图片上传 ===================== */
const beforeImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 <= 5
  if (!isImage) {
    ElMessage.error('仅支持上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleImageUpload = async (upload) => {
  try {
    const res = await adminApi.uploadImage(upload.file)
    if (res?.code === 200 && res.data) {
      form.imageUrl = res.data
      upload.onSuccess(res)
      ElMessage.success('图片上传成功')
    } else {
      ElMessage.error(res?.message || '图片上传失败')
      upload.onError(new Error(res?.message || '上传失败'))
    }
  } catch (e) {
    console.error('[ProductManagement] 图片上传失败：', e)
    ElMessage.error(e?.response?.data?.message || '图片上传失败')
    upload.onError(e)
  }
}

/* ===================== 生命周期 ===================== */
onMounted(async () => {
  await loadCategories()
  loadList()
})
</script>

<style scoped>
/* ========== A 档 翡翠绿 主题变量 ========== */
.pm-root {
  --brand: #0d9488;
  --brand-600: #0f766e;
  --cyan: #06b6d4;
  --amber: #f59e0b;
  --bg: #F8FAFC;
  --border: #E2E8F0;
  --text: #1E293B;
  --sub: #64748B;
  --radius: 14px;
  --shadow: 0 8px 24px rgba(13,148,136,.08);

  min-height: 100%;
  padding: 32px 40px 48px;
  background: var(--bg);
}

/* ========== 头部 ========== */
.pm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.pm-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: 0.2px;
  margin: 0 0 4px;
}
.pm-sub {
  margin: 0;
  font-size: 13px;
  color: var(--sub);
}
.pm-add-btn {
  background: linear-gradient(135deg, var(--brand), var(--cyan));
  border: none;
  border-radius: 14px;
  padding: 0 22px;
  height: 44px;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(13,148,136,.22);
  transition: all .2s ease;
}
.pm-add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(13,148,136,.3);
}

/* ========== 筛选卡 / 列表卡 共用软阴影 & 14px 圆角 ========== */
.pm-filter-card,
.pm-table-card {
  background: #ffffff;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 24px 26px;
  transition: box-shadow .25s ease, transform .25s ease;
}
.pm-filter-card { margin-bottom: 24px; }

.pm-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: center;
}
.pm-filter-input { min-width: 280px; }
.pm-filter-select { min-width: 160px; }
.pm-filter-btn {
  border-radius: 12px !important;
  height: 40px !important;
  padding: 0 22px !important;
}
/* Element Plus 组件统一 14px 圆角 */
.pm-input :deep(.el-input__wrapper),
.pm-input--lg :deep(.el-input__wrapper),
.pm-filter-select :deep(.el-select__wrapper),
.pm-dialog :deep(.el-input__wrapper),
.pm-dialog :deep(.el-textarea__inner),
.pm-dialog :deep(.el-select__wrapper),
.pm-dialog :deep(.el-input-number),
.pm-dialog :deep(.el-radio-button__inner),
.pm-pagination :deep(.el-pager li),
.pm-pagination :deep(.btn-prev),
.pm-pagination :deep(.btn-next),
.pm-pagination :deep(.el-pagination__sizes .el-select__wrapper) {
  border-radius: 12px !important;
}
.pm-dialog :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-top-left-radius: 12px !important;
  border-bottom-left-radius: 12px !important;
}
.pm-dialog :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-top-right-radius: 12px !important;
  border-bottom-right-radius: 12px !important;
}
.pm-dialog :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--brand);
  border-color: var(--brand);
}

/* ========== 表格 ========== */
.pm-table {
  border-radius: 12px;
  overflow: hidden;
}
.pm-table :deep(.el-table__row) {
  transition: background .15s ease;
}
.pm-table :deep(.el-table__row:hover > td) {
  background: #F0FDFA !important;
}

.pm-product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.pm-product-thumb {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border);
  background: var(--bg);
  flex-shrink: 0;
}
.pm-product-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pm-product-info {
  min-width: 0;
}
.pm-product-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 320px;
}
.pm-product-desc {
  font-size: 12px;
  color: var(--sub);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pm-tag-category {
  background: linear-gradient(135deg, rgba(13,148,136,.08), rgba(6,182,212,.08)) !important;
  color: var(--brand) !important;
  border: 1px solid rgba(13,148,136,.18) !important;
  border-radius: 10px !important;
  padding: 0 10px !important;
  font-weight: 500;
}
.pm-price {
  color: #EF4444;
  font-weight: 700;
  font-size: 15px;
  letter-spacing: 0.3px;
}
.pm-stock {
  font-weight: 600;
  color: var(--text);
}
.pm-stock--low {
  color: var(--amber);
}

/* ========== 分页 ========== */
.pm-pagination-wrap {
  margin-top: 22px;
  display: flex;
  justify-content: flex-end;
}
.pm-pagination :deep(.is-active) {
  background: var(--brand) !important;
  color: #fff !important;
  border-color: var(--brand) !important;
}

/* ========== 弹窗 ========== */
.pm-dialog :deep(.el-dialog) {
  border-radius: 18px !important;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(15,118,110,.2) !important;
}
.pm-dialog :deep(.el-dialog__header) {
  padding: 20px 28px;
  border-bottom: 1px solid #F1F5F9;
  background: linear-gradient(180deg, #F0FDFA, #ffffff);
}
.pm-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
}
.pm-dialog :deep(.el-dialog__body) {
  padding: 24px 28px 0;
}
.pm-dialog :deep(.el-dialog__footer) {
  padding: 18px 28px 24px;
  border-top: 1px solid #F1F5F9;
}
.pm-submit-btn {
  background: linear-gradient(135deg, var(--brand), var(--cyan)) !important;
  border: none !important;
  border-radius: 12px !important;
  padding: 0 22px;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(13,148,136,.22);
}
.pm-submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(13,148,136,.3);
}

/* ========== 图片上传 ========== */
.pm-uploader {
  width: 100%;
}
.pm-uploader :deep(.el-upload-dragger) {
  width: 100%;
  padding: 0 !important;
  border-radius: 14px !important;
  border: 2px dashed #CBD5E1 !important;
  background: #F8FAFC;
  overflow: hidden;
  transition: all .2s ease;
}
.pm-uploader :deep(.el-upload-dragger:hover) {
  border-color: var(--brand) !important;
  background: #F0FDFA;
}
.pm-upload-preview {
  position: relative;
  width: 100%;
  height: 220px;
}
.pm-upload-preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
}
.pm-upload-mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #fff;
  font-size: 13px;
  opacity: 0;
  transition: opacity .2s ease;
}
.pm-upload-preview:hover .pm-upload-mask { opacity: 1; }
.pm-upload-icon { font-size: 26px; }

.pm-upload-placeholder {
  width: 100%;
  padding: 40px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--sub);
}
.pm-upload-placeholder .pm-upload-icon {
  font-size: 36px;
  color: var(--brand);
  background: rgba(13,148,136,.08);
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
}
.pm-upload-tip-1 {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}
.pm-upload-tip-2 {
  font-size: 12px;
  color: #94A3B8;
}

/* ========== 响应式 ========== */
@media (max-width: 900px) {
  .pm-root { padding: 20px 16px 32px; }
  .pm-filter-input { min-width: 100%; }
  .pm-filter-select { min-width: calc(50% - 7px); }
  .pm-header { flex-direction: column; gap: 16px; align-items: flex-start; }
}
</style>
