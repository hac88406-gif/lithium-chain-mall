<template>
  <div class="user-management">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-xl font-semibold text-gray-800">买家用户管理</h2>
    </div>

    <TableList
      :data="users"
      :columns="columns"
      :total="total"
      :loading="loading"
      :pagination="pagination"
      :show-edit="false"
      :show-delete="false"
      :search-placeholder="'搜索用户名/昵称/手机号'"
      @search="handleSearch"
      @pagination-change="handlePaginationChange"
    >
      <!-- 状态列：1=正常（绿） 0=禁用（红） -->
      <template #cell-status="{ item }">
        <span :class="item.status === 1 ? 'badge badge-active' : 'badge badge-disabled'">
          {{ item.status === 1 ? '正常' : '禁用' }}
        </span>
      </template>

      <!-- 注册时间列：LocalDateTime 字符串截断展示 -->
      <template #cell-createTime="{ item }">
        {{ fmtTime(item.createTime) }}
      </template>

      <!-- 操作列：编辑（昵称/状态）+ 重置密码 -->
      <template #actions="{ item }">
        <button class="btn-action btn-edit" @click="openEdit(item)">编辑</button>
        <button class="btn-action btn-pwd" @click="openResetPwd(item)">重置密码</button>
      </template>
    </TableList>

    <!-- 编辑弹窗 -->
    <div class="modal-overlay" v-if="showEditModal" @click="closeModals">
      <div class="modal-content" @click.stop>
        <h3 class="text-lg font-semibold mb-4">编辑用户（{{ editForm.username }}）</h3>
        <form @submit.prevent="handleEditSubmit">
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">昵称</label>
            <input
              v-model="editForm.nickname"
              type="text"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
              placeholder="请输入昵称"
            />
          </div>
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">账号状态</label>
            <select
              v-model="editForm.status"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none bg-white"
            >
              <option :value="1">正常（可登录）</option>
              <option :value="0">禁用（禁止登录）</option>
            </select>
          </div>
          <div class="flex gap-3 justify-end">
            <button type="button" @click="closeModals" class="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50">取消</button>
            <button type="submit" class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-secondary">保存</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 重置密码弹窗 -->
    <div class="modal-overlay" v-if="showPwdModal" @click="closeModals">
      <div class="modal-content" @click.stop>
        <h3 class="text-lg font-semibold mb-4">重置密码（{{ pwdUser?.username }}）</h3>
        <form @submit.prevent="handlePwdSubmit">
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">新密码</label>
            <input
              v-model="pwdForm.password"
              type="password"
              minlength="6"
              maxlength="20"
              required
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
              placeholder="6~20 位新密码"
            />
          </div>
          <div class="flex gap-3 justify-end">
            <button type="button" @click="closeModals" class="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50">取消</button>
            <button type="submit" class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-secondary">重置</button>
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

const users = ref([])
const total = ref(0)
const loading = ref(false)
const pagination = reactive({ page: 1, size: 10 })
const keyword = ref('')

const columns = [
  { key: 'id', label: 'ID' },
  { key: 'username', label: '用户名' },
  { key: 'nickname', label: '昵称' },
  { key: 'phone', label: '手机号' },
  { key: 'email', label: '邮箱' },
  { key: 'status', label: '状态' },
  { key: 'createTime', label: '注册时间' }
]

const showEditModal = ref(false)
const showPwdModal = ref(false)
const editForm = reactive({ id: null, username: '', nickname: '', status: 1 })
const pwdUser = ref(null)
const pwdForm = reactive({ password: '' })

/** LocalDateTime 序列化为 "2026-09-11T10:30:00"，统一截断展示 */
const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

const fetchList = async () => {
  loading.value = true
  try {
    const res = await adminApi.getUsers({
      page: pagination.page,
      size: pagination.size,
      keyword: keyword.value || undefined
    })
    if (res?.code === 200) {
      users.value = res.data?.records || []
      total.value = res.data?.total || 0
    } else {
      alert(res?.message || '加载用户列表失败')
    }
  } catch (e) {
    console.error('[UserManagement] fetchList error:', e)
    alert('加载用户列表失败，请检查网络')
  } finally {
    loading.value = false
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

const openEdit = (item) => {
  editForm.id = item.id
  editForm.username = item.username || ''
  editForm.nickname = item.nickname || ''
  editForm.status = item.status === 1 ? 1 : 0
  showEditModal.value = true
}

const handleEditSubmit = async () => {
  if (!editForm.nickname.trim()) {
    alert('请输入昵称')
    return
  }
  try {
    const res = await adminApi.updateUser(editForm.id, {
      nickname: editForm.nickname.trim(),
      status: editForm.status
    })
    if (res?.code === 200) {
      alert('保存成功')
      closeModals()
      fetchList()
    } else {
      alert(res?.message || '保存失败')
    }
  } catch (e) {
    alert('保存失败，请检查网络')
  }
}

const openResetPwd = (item) => {
  pwdUser.value = item
  pwdForm.password = ''
  showPwdModal.value = true
}

const handlePwdSubmit = async () => {
  if (!pwdForm.password || pwdForm.password.length < 6 || pwdForm.password.length > 20) {
    alert('密码长度需为 6~20 位')
    return
  }
  try {
    const res = await adminApi.resetUserPassword(pwdUser.value.id, { password: pwdForm.password })
    if (res?.code === 200) {
      alert(res.message || '密码重置成功')
      closeModals()
    } else {
      alert(res?.message || '重置失败')
    }
  } catch (e) {
    alert('重置失败，请检查网络')
  }
}

const closeModals = () => {
  showEditModal.value = false
  showPwdModal.value = false
}

onMounted(fetchList)
onActivated(fetchList)
</script>

<style scoped>
.user-management {
  min-height: 100%;
}

.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 500;
}

.badge-active {
  background: #dcfce7;
  color: #16a34a;
}

.badge-disabled {
  background: #fee2e2;
  color: #dc2626;
}

.btn-action {
  padding: 4px 12px;
  font-size: 13px;
  border-radius: 8px;
  transition: all .2s ease;
}

.btn-edit {
  background: #dbeafe;
  color: #2563eb;
  margin-right: 8px;
}

.btn-edit:hover {
  background: #bfdbfe;
}

.btn-pwd {
  background: #fef3c7;
  color: #d97706;
}

.btn-pwd:hover {
  background: #fde68a;
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
