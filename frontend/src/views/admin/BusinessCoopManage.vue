<template>
  <div class="category-management">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-xl font-semibold text-gray-800">商务合作申请</h2>
      <div class="flex gap-2">
        <button
          v-for="s in statusTabs"
          :key="s.value"
          @click="currentStatus = s.value; pagination.page = 1; fetchList()"
          :class="['px-4 py-2 rounded-lg text-sm font-medium transition',
            currentStatus === s.value ? 'bg-primary text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
        >{{ s.label }}</button>
      </div>
    </div>

    <TableList
      :data="list"
      :columns="columns"
      :total="total"
      :pagination="pagination"
      :keyword-placeholder="'搜索企业/联系人/电话'"
      :show-edit="true"
      :show-delete="true"
      @edit="openDetail"
      @delete="handleDelete"
      @search="handleSearch"
      @pagination-change="handlePaginationChange"
    />

    <div v-if="detail" class="modal-overlay" @click.self="closeDetail">
      <div class="modal-content max-w-2xl max-h-[80vh] overflow-y-auto" @click.stop>
        <h3 class="text-lg font-semibold mb-4">合作申请 #{{ detail.id }}
          <span :class="['ml-2 px-2 py-0.5 rounded text-xs',
            detail.status === 0 ? 'bg-yellow-100 text-yellow-700' :
            detail.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-200 text-gray-600']">
            {{ statusLabel(detail.status) }}
          </span>
        </h3>

        <div class="grid grid-cols-2 gap-3 text-sm mb-4">
          <div><span class="text-gray-500">类型：</span>{{ detail.type === 'media' ? '媒体采访' : '商务合作' }}</div>
          <div><span class="text-gray-500">提交时间：</span>{{ detail.createTime }}</div>
          <div v-if="detail.companyName"><span class="text-gray-500">企业/媒体：</span>{{ detail.companyName }}</div>
          <div v-if="detail.contactPerson"><span class="text-gray-500">联系人：</span>{{ detail.contactPerson }}</div>
          <div v-if="detail.phone"><span class="text-gray-500">电话：</span>{{ detail.phone }}</div>
          <div v-if="detail.email"><span class="text-gray-500">邮箱：</span>{{ detail.email }}</div>
          <div v-if="detail.requirementType"><span class="text-gray-500">需求类型：</span>{{ detail.requirementType }}</div>
          <div v-if="detail.budget"><span class="text-gray-500">预算：</span>{{ detail.budget }}</div>
          <div v-if="detail.intention"><span class="text-gray-500">合作意向：</span>{{ detail.intention }}</div>
          <div v-if="detail.deliveryCycle"><span class="text-gray-500">交付周期：</span>{{ detail.deliveryCycle }}</div>
          <div v-if="detail.companyAddress" class="col-span-2"><span class="text-gray-500">地址：</span>{{ detail.companyAddress }}</div>
          <div v-if="detail.remark" class="col-span-2"><span class="text-gray-500">备注：</span>{{ detail.remark }}</div>
          <div v-if="detail.mediaTitle" class="col-span-2"><span class="text-gray-500">采访标题：</span>{{ detail.mediaTitle }}</div>
          <div v-if="detail.mediaFormat"><span class="text-gray-500">形式：</span>{{ detail.mediaFormat }}</div>
          <div v-if="detail.mediaDate"><span class="text-gray-500">期望日期：</span>{{ detail.mediaDate }}</div>
        </div>

        <div v-if="detail.reply" class="bg-blue-50 p-3 rounded-lg text-sm mb-4">
          <div class="text-blue-700 font-medium mb-1">管理员回复（{{ detail.replyBy || 'admin' }} · {{ detail.replyTime }}）</div>
          <div class="text-blue-800 whitespace-pre-wrap">{{ detail.reply }}</div>
        </div>

        <div class="border-t pt-4">
          <label class="block text-sm font-medium text-gray-700 mb-2">更新状态 + 回复</label>
          <select v-model="replyForm.status" class="w-full px-3 py-2 border rounded-lg mb-3 bg-white">
            <option :value="0">待处理</option>
            <option :value="1">已跟进</option>
            <option :value="2">已关闭</option>
          </select>
          <textarea
            v-model="replyForm.reply"
            rows="3"
            class="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary outline-none"
            placeholder="回复内容（可选）"
          ></textarea>
          <div class="flex justify-end gap-3 mt-4">
            <button @click="closeDetail" class="px-4 py-2 border rounded-lg text-gray-700 hover:bg-gray-50">取消</button>
            <button @click="submitReply" class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-secondary">保存</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated } from 'vue'
import TableList from '../../components/admin/TableList.vue'
import { adminApi } from '../../api/index'

const list = ref([])
const total = ref(0)
const keyword = ref('')
const currentStatus = ref(0) // 0=全部, 1=待处理, 2=已跟进, 3=已关闭
const pagination = reactive({ page: 1, size: 10 })

const statusTabs = [
  { value: 0, label: '全部' },
  { value: 1, label: '待处理' },
  { value: 2, label: '已跟进' },
  { value: 3, label: '已关闭' },
]

const statusLabel = (s) => [undefined, '待处理', '已跟进', '已关闭'][s] || '未知'

const columns = [
  { key: 'id', label: 'ID', width: 70 },
  { key: 'type', label: '类型', render: (v) => v === 'media' ? '媒体' : '合作' },
  { key: 'companyName', label: '企业/媒体' },
  { key: 'contactPerson', label: '联系人' },
  { key: 'phone', label: '电话' },
  { key: 'status', label: '状态', render: (v) => statusLabel(v) },
  { key: 'createTime', label: '提交时间' },
]

const detail = ref(null)
const replyForm = reactive({ status: 1, reply: '' })

const fetchList = async () => {
  try {
    const params = { page: pagination.page, size: pagination.size }
    if (currentStatus.value > 0) params.status = currentStatus.value - 1
    if (keyword.value) params.keyword = keyword.value
    const res = await adminApi.getCooperations(params)
    if (res?.code === 200) {
      list.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch (e) { console.error(e) }
}

const handleSearch = ({ keyword: kw }) => {
  keyword.value = kw; pagination.page = 1; fetchList()
}

const handlePaginationChange = (p) => {
  pagination.page = p.page; pagination.size = p.size; fetchList()
}

const handleDelete = async (row) => {
  if (!confirm(`确定删除 #${row.id} 的合作申请？`)) return
  const res = await adminApi.deleteCooperation(row.id)
  if (res?.code === 200) { alert('已删除'); fetchList() }
}

const openDetail = async (row) => {
  // 拉完整数据（列表里可能字段不全）
  try {
    const res = await adminApi.getCooperationDetail(row.id)
    if (res?.code === 200) detail.value = res.data
    else detail.value = row
  } catch { detail.value = row }
  replyForm.status = detail.value.status ?? 0
  replyForm.reply = ''
}
const closeDetail = () => { detail.value = null }

const submitReply = async () => {
  const res = await adminApi.replyCooperation(detail.value.id, {
    status: replyForm.status,
    reply: replyForm.reply.trim(),
    replyBy: 'admin'
  })
  if (res?.code === 200) {
    alert('保存成功')
    closeDetail()
    fetchList()
  } else {
    alert(res?.message || '保存失败')
  }
}

onMounted(fetchList)
onActivated(fetchList)
</script>

<style scoped>
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center;
  z-index: 1000;
}
.modal-content {
  background: white; padding: 24px; border-radius: 12px; width: 600px;
}
</style>
