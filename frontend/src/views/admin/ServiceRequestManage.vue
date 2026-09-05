<template>
  <div class="category-management">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-xl font-semibold text-gray-800">人工客服请求</h2>
      <div class="flex gap-2 flex-wrap">
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
      :keyword-placeholder="'搜索姓名/电话/问题'"
      :show-edit="true"
      :show-delete="true"
      @edit="openDetail"
      @delete="handleDelete"
      @search="handleSearch"
      @pagination-change="handlePaginationChange"
    />

    <!-- ========== 详情弹窗 ========== -->
    <div v-if="detail" class="modal-overlay" @click.self="closeDetail">
      <div class="modal-content modal-big" @click.stop>
        <!-- 标题 + 状态标签 -->
        <div class="flex items-center justify-between mb-4 modal-header">
          <h3 class="text-lg font-semibold">
            客服请求 #{{ detail.id }}
            <span :class="['ml-2 px-2 py-0.5 rounded text-xs', statusBadgeClass(detail.status)]">
              {{ statusLabel(detail.status) }}
            </span>
            <span :class="['ml-2 px-2 py-0.5 rounded text-xs',
              detail.source === 'ai_transfer' ? 'bg-purple-100 text-purple-700' : 'bg-gray-100 text-gray-600']">
              {{ sourceLabel(detail.source) }}
            </span>
          </h3>
          <button @click="closeDetail" class="text-gray-400 hover:text-gray-600 text-xl">×</button>
        </div>

        <!-- 客户基本信息 -->
        <div class="info-grid">
          <div class="info-cell"><span class="info-label">客户</span><span class="info-value">{{ detail.name || '未登录访客' }}</span></div>
          <div class="info-cell"><span class="info-label">电话</span><span class="info-value">{{ detail.phone || '-' }}</span></div>
          <div class="info-cell"><span class="info-label">邮箱</span><span class="info-value">{{ detail.email || '-' }}</span></div>
          <div class="info-cell"><span class="info-label">来源</span><span class="info-value">{{ sourceLabel(detail.source) }}</span></div>
          <div class="info-cell"><span class="info-label">提交时间</span><span class="info-value">{{ fmtTime(detail.createTime) }}</span></div>
          <div class="info-cell"><span class="info-label">未读</span><span class="info-value">{{ detail.adminUnreadCount || 0 }}</span></div>
        </div>

        <!-- AI 对话历史（默认收起） -->
        <div
          v-if="detail.source === 'ai_transfer' && parsedHistory.length"
          class="ai-history-wrap mb-3"
          :class="{ 'is-expanded': aiHistoryExpanded }"
        >
          <button class="ai-history-toggle" @click="aiHistoryExpanded = !aiHistoryExpanded">
            <span class="ai-history-chevron">{{ aiHistoryExpanded ? '▾' : '▸' }}</span>
            <span class="ai-history-title">🤖 AI 对话历史（只读 · 已同步给客服）</span>
            <span class="ai-history-count">共 {{ parsedHistory.length }} 条</span>
          </button>
          <transition name="slide">
            <div v-show="aiHistoryExpanded" class="ai-history-box">
              <div v-for="(m, i) in parsedHistory" :key="i" class="ai-history-item">
                <span :class="['ai-history-role', m.role === 'user' ? 'user' : 'ai']">
                  {{ m.role === 'user' ? '👤 用户' : '🤖 AI' }}
                </span>
                <span class="ai-history-content">{{ m.content || '' }}</span>
              </div>
            </div>
          </transition>
        </div>

        <!-- 双向聊天消息区 -->
        <div class="chat-section">
          <div class="chat-section-title">💬 实时对话（{{ messages.length }} 条）</div>
          <div class="chat-area" ref="chatAreaRef">
            <div
              v-for="m in messages"
              :key="m.id"
              class="chat-msg"
              :class="{ 'is-admin': m.senderType === 'admin', 'is-system': m.senderType === 'system' }"
            >
              <span class="msg-avatar">
                {{ m.senderType === 'admin' ? '👩‍💼' : (m.senderType === 'system' ? '💬' : '👤') }}
              </span>
              <div class="msg-body">
                <div class="msg-content">{{ m.content }}</div>
                <div v-if="m.createTime" class="msg-time">{{ fmtTime(m.createTime) }}</div>
              </div>
            </div>
            <div v-if="messages.length === 0" class="text-center text-gray-400 py-8 text-sm">
              暂无对话消息
            </div>
          </div>
        </div>

        <!-- 客户评价展示区（状态 2/3 才会有；如果有评价就显示） -->
        <div v-if="review" class="review-display">
          <div class="review-display-title">⭐ 客户评价</div>
          <div class="review-stars">
            <span v-for="i in 5" :key="i" class="star" :class="{ filled: review.rating >= i }">★</span>
            <span class="review-time">{{ fmtTime(review.createTime) }}</span>
          </div>
          <p v-if="review.content" class="review-content">{{ review.content }}</p>
          <p v-else class="review-content review-empty">（客户未填写文字评价）</p>
        </div>

        <!-- 回复区：管理员只能发消息，无权手动完结状态 -->
        <div class="reply-section">
          <div v-if="detail.status === 2" class="reply-tip warning">
            ⏱️ 会话已超时关闭，客户还可提交评价；管理员无法继续回复。
          </div>
          <div v-else-if="detail.status === 3" class="reply-tip info">
            ✅ 会话已完成，感谢您的服务 💚
          </div>
          <div v-else class="reply-tip">
            💡 管理员仅可发送聊天消息，工单状态由客户或超时自动决定。
          </div>

          <textarea
            v-model="replyText"
            rows="3"
            class="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary outline-none text-sm"
            :placeholder="replyDisabled ? '会话已关闭，无法回复' : '输入回复内容...（会实时发送给客户）'"
            :disabled="replyDisabled"
          ></textarea>
          <div class="flex justify-end gap-3 mt-3">
            <button @click="closeDetail" class="px-4 py-2 border rounded-lg text-gray-700 hover:bg-gray-50 text-sm">关闭</button>
            <button
              @click="submitReply"
              :disabled="!replyText.trim() || replyDisabled"
              class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-secondary text-sm disabled:opacity-50"
            >发送回复</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onActivated, onUnmounted, onDeactivated, nextTick } from 'vue'
import TableList from '../../components/admin/TableList.vue'
import { adminApi } from '../../api/index'

const list = ref([])
const total = ref(0)
const keyword = ref('')
const currentStatus = ref(0) // 0=全部, 1=🔴有未读, 2=活跃中(0+1), 3=超时关闭(2), 4=已归档(3)
const pagination = reactive({ page: 1, size: 10 })

/**
 * 状态标签（4 档）：
 *   0=全部（查询时不带 status 参数，page 里不筛）
 *   1=🔴 有未读（adminUnreadOnly=1）
 *   2=活跃中（status IN 0,1）—— page 查询 status=0 先查；再单独查 1 然后合并？
 *     实际上 MyBatis-Plus LambdaQueryWrapper 原生 .in 条件可以传递，但参数传单个 status，
 *     所以这里前端约定：当 currentStatus.value===2 时传 status=null 但追加 statusList=[0,1] 不现实，
 *     干脆 page 查询 status=0 然后 + status=1 做两次合并？嫌麻烦就给后端加个 statusList 参数。
 *     —— 简化：直接让前端传 statusList 数组，后端支持 List<Integer> status。
 */
const statusTabs = [
  { value: 0, label: '全部' },
  { value: 1, label: '🔴 有未读', flag: 'unread' },
  { value: 2, label: '活跃中' },
  { value: 3, label: '超时关闭' },
  { value: 4, label: '已完成' },
]

// 数据库语义（方案 A：保留原始编号）：
//   0=待应答  1=活跃中  2=超时关闭  3=已完成
const statusLabel = (s) => ['待应答', '活跃中', '超时关闭', '已完成'][s] || '未知'
const statusBadgeClass = (s) => [
  'bg-yellow-100 text-yellow-700',   // 0
  'bg-blue-100 text-blue-700',       // 1
  'bg-orange-100 text-orange-700',   // 2
  'bg-green-100 text-green-700'      // 3
][s] || 'bg-gray-200 text-gray-600'

const sourceLabel = (s) => {
  if (s === 'ai_transfer') return '🤖 AI转人工'
  if (s === 'contact') return '📝 表单提交'
  return s || '-'
}

/**
 * 时间格式化
 */
const fmtTime = (t) => {
  if (!t) return '-'
  const str = String(t).replace('T', ' ')
  const m = str.match(/^(\d{4})-(\d{2})-(\d{2})\s*(\d{2}):(\d{2})(?::\d{2})?$/)
  if (!m) return str
  const [_, y, mo, d, h, mi] = m
  const sameYear = Number(y) === new Date().getFullYear()
  return sameYear ? `${mo}-${d} ${h}:${mi}` : `${y}-${mo}-${d} ${h}:${mi}`
}

const columns = [
  { key: 'id', label: 'ID', width: 70 },
  { key: 'source', label: '来源', render: (v) => sourceLabel(v) },
  { key: 'name', label: '客户' },
  { key: 'phone', label: '电话' },
  { key: 'question', label: '问题分类' },
  { key: 'adminUnreadCount', label: '未读', width: 70 },
  { key: 'status', label: '状态', render: (v) => statusLabel(v) },
  { key: 'createTime', label: '提交时间', render: (v) => fmtTime(v) },
]

// ===== 详情弹窗 =====
const detail = ref(null)
const messages = ref([])
const review = ref(null)          // 客户评价（详情里拉取）
const replyText = ref('')
const chatAreaRef = ref(null)
const aiHistoryExpanded = ref(false)

const parsedHistory = computed(() => {
  if (!detail.value?.chatHistory) return []
  try { return JSON.parse(detail.value.chatHistory) } catch { return [] }
})

/** 2 或 3 时禁用回复 */
const replyDisabled = computed(() => detail.value && (detail.value.status === 2 || detail.value.status === 3))

const fetchList = async () => {
  try {
    const params = { page: pagination.page, size: pagination.size }
    const tab = statusTabs[currentStatus.value]
    if (tab?.flag === 'unread') {
      params.adminUnreadOnly = 1
    } else if (currentStatus.value === 2) {
      // 活跃中：status IN (0, 1)
      // 注意：axios 默认把数组序列化成 statusList[]=0&statusList[]=1（带方括号），
      // Spring 不认识 statusList[] 这个参数名会导致绑定失败、筛选全失效。
      // 改传逗号字符串 "0,1"，Spring 会自动按逗号拆分成 List<Integer>。
      params.statusList = '0,1'
    } else if (currentStatus.value === 3) {
      params.status = 2   // 超时关闭
    } else if (currentStatus.value === 4) {
      params.status = 3   // 已完成
    }
    if (keyword.value) params.keyword = keyword.value
    const res = await adminApi.getServiceRequests(params)
    if (res?.code === 200) {
      list.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch (e) { console.error(e) }
}

// ===== 列表页 10 秒轻量轮询 =====
let listPollTimer = null
const startListPoll = () => {
  stopListPoll()
  listPollTimer = setInterval(() => fetchList(), 10_000)
}
const stopListPoll = () => {
  if (listPollTimer) { clearInterval(listPollTimer); listPollTimer = null }
}

const handleSearch = ({ keyword: kw }) => {
  keyword.value = kw; pagination.page = 1; fetchList()
}
const handlePaginationChange = (p) => {
  pagination.page = p.page; pagination.size = p.size; fetchList()
}
const handleDelete = async (row) => {
  if (!confirm(`确定删除 #${row.id} 的客服请求？（会同时删除 ${row.adminUnreadCount || 0} 条消息）`)) return
  const res = await adminApi.deleteServiceRequest(row.id)
  if (res?.code === 200) { alert('已删除'); fetchList() }
}

const openDetail = async (row) => {
  detail.value = row
  replyText.value = ''
  messages.value = []
  review.value = null
  aiHistoryExpanded.value = false

  // 1) 重新拉详情（确保 fillUserInfo 生效）
  try {
    const dRes = await adminApi.getServiceRequestDetail(row.id)
    if (dRes?.code === 200 && dRes.data) detail.value = dRes.data
  } catch { /* 用列表行兜底 */ }

  // 2) 拉完整消息
  try {
    const res = await adminApi.getServiceMessages(row.id, 0)
    if (res?.code === 200) messages.value = res.data || []
  } catch { /* ignore */ }

  // 3) 拉评价
  try {
    const rRes = await adminApi.getServiceReview(row.id)
    if (rRes?.code === 200 && rRes.data) review.value = rRes.data
  } catch { /* ignore */ }

  // 4) 标记已读
  adminApi.markServiceRead(row.id).catch(() => {}).then(fetchList)

  nextTick(scrollChatToBottom)
  startDetailPoll(row.id)
}

// ===== 详情弹窗 3 秒状态轮询（只拉 status + review，不拉 messages）=====
let detailPollTimer = null
const startDetailPoll = (reqId) => {
  stopDetailPoll()
  detailPollTimer = setInterval(async () => {
    try {
      const dRes = await adminApi.getServiceRequestDetail(reqId)
      if (dRes?.code === 200 && dRes.data) {
        const newStatus = dRes.data.status
        const oldStatus = detail.value?.status
        if (oldStatus !== newStatus) {
          detail.value.status = newStatus
          // 状态变化 → 立刻同步列表（只改当前行 status，不整页拉取，快！）
          const row = list.value.find(r => r.id === reqId)
          if (row) row.status = newStatus
          fetchList()  // 同时整页刷新（让 row 从旧 tab 移到新 tab）

          // 2→3 时顺手拉评价
          if (oldStatus === 2 && newStatus === 3) {
            try {
              const rRes = await adminApi.getServiceReview(reqId)
              if (rRes?.code === 200 && rRes.data) review.value = rRes.data
            } catch {}
          }
        }
      }
    } catch { /* ignore */ }
  }, 3_000)
}
const stopDetailPoll = () => {
  if (detailPollTimer) { clearInterval(detailPollTimer); detailPollTimer = null }
}

const closeDetail = () => {
  stopDetailPoll()
  detail.value = null
  messages.value = []
  review.value = null
}

const scrollChatToBottom = () => {
  if (chatAreaRef.value) chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight
}

const submitReply = async () => {
  const text = replyText.value.trim()
  if (!text || !detail.value?.id) return
  const res = await adminApi.replyServiceRequest(detail.value.id, {
    content: text,
    replyBy: 'admin'
    // 注意：不再传 status 字段——管理员无权手动修改工单状态
  })
  if (res?.code === 200) {
    replyText.value = ''
    const msgRes = await adminApi.getServiceMessages(detail.value.id, 0)
    if (msgRes?.code === 200) messages.value = msgRes.data || []
    // 自动刷新工单状态（管理端自动升级 0→1）
    detail.value.status = (detail.value.status ?? 0) === 0 ? 1 : detail.value.status
    fetchList()
    nextTick(scrollChatToBottom)
  } else {
    alert(res?.message || '发送失败')
  }
}

onMounted(() => { fetchList(); startListPoll() })
onActivated(() => { fetchList(); startListPoll() })
onUnmounted(() => { stopListPoll(); stopDetailPoll() })
onDeactivated(() => { stopListPoll(); stopDetailPoll() })
</script>

<style scoped>
/* ===== 弹窗遮罩 ===== */
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000; padding: 16px;
}

.modal-content.modal-big {
  width: 100%; max-width: 960px; height: 92vh; max-height: none;
  background: white; padding: 24px; border-radius: 12px;
  display: flex; flex-direction: column; overflow: hidden;
}
.modal-header { flex-shrink: 0; }

/* 客户信息 */
.info-grid {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 8px 16px; padding: 14px 16px;
  background: #f9fafb; border-radius: 10px;
  margin-bottom: 16px; flex-shrink: 0;
}
.info-cell { display: flex; align-items: baseline; gap: 6px; font-size: 13px; }
.info-label { color: #9ca3af; flex-shrink: 0; min-width: 52px; }
.info-value { color: #1f2937; font-weight: 500; word-break: break-all; }

/* AI 历史 */
.ai-history-wrap {
  background: #faf5ff; border: 1px solid #e9d5ff;
  border-radius: 10px; overflow: hidden; flex-shrink: 0;
}
.ai-history-toggle {
  width: 100%; display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: transparent; border: none;
  cursor: pointer; font-size: 12px; color: #6b7280; text-align: left;
}
.ai-history-toggle:hover { background: #f3e8ff; }
.ai-history-chevron { font-size: 10px; color: #a855f7; width: 12px; text-align: center; flex-shrink: 0; }
.ai-history-title { font-weight: 500; color: #6b21a8; flex: 1; }
.ai-history-count { color: #a855f7; font-size: 11px; }
.ai-history-box {
  padding: 4px 12px 10px; background: #faf5ff;
  border-top: 1px dashed #e9d5ff;
  max-height: 260px; overflow-y: auto;
}
.ai-history-item { font-size: 13px; line-height: 1.6; margin-bottom: 6px; }
.ai-history-role { font-weight: 600; margin-right: 6px; }
.ai-history-role.user { color: #7e22ce; }
.ai-history-role.ai   { color: #6b7280; }
.ai-history-content { color: #374151; }

.slide-enter-active, .slide-leave-active { transition: max-height .25s ease, opacity .2s; overflow: hidden; }
.slide-enter-from, .slide-leave-to { max-height: 0 !important; opacity: 0; }

/* 消息区 */
.chat-section {
  flex: 1; display: flex; flex-direction: column;
  min-height: 0; margin-bottom: 12px;
}
.chat-section-title { font-size: 12px; font-weight: 500; color: #6b7280; margin-bottom: 8px; flex-shrink: 0; }
.chat-area {
  flex: 1; background: #f9fafb; border: 1px solid #e5e7eb;
  border-radius: 10px; padding: 16px; overflow-y: auto;
  display: flex; flex-direction: column;
}

/* 消息气泡 */
.chat-msg { display: flex; gap: 10px; margin-bottom: 14px; align-items: flex-start; }
.chat-msg.is-admin { flex-direction: row-reverse; }
.chat-msg.is-admin .msg-body { align-items: flex-end; }
.chat-msg.is-system .msg-body { align-items: center; }
.chat-msg.is-system .msg-content { background: #fef3c7; color: #92400e; font-style: italic; font-size: 12px; }

.msg-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: white; border: 1px solid #e5e7eb;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; flex-shrink: 0;
}
.chat-msg.is-admin  .msg-avatar { background: #eff6ff; }
.chat-msg.is-system .msg-avatar { background: #fef3c7; }

.msg-body { display: flex; flex-direction: column; max-width: 75%; gap: 3px; }
.msg-content {
  padding: 10px 14px; border-radius: 12px; font-size: 13px; line-height: 1.55;
  word-break: break-word; background: white; border: 1px solid #e5e7eb; color: #1f2937;
}
.chat-msg.is-admin .msg-content { background: #0d9488; color: white; border-color: #0d9488; }
.msg-time { font-size: 11px; color: #9ca3af; padding: 0 4px; }

/* ===== 客户评价展示区 ===== */
.review-display {
  flex-shrink: 0;
  border: 1px solid #fde68a; background: #fffbeb;
  border-radius: 10px; padding: 14px 16px;
  margin-bottom: 12px;
}
.review-display-title { font-size: 13px; font-weight: 600; color: #92400e; margin-bottom: 6px; }
.review-stars { display: flex; align-items: center; gap: 4px; margin-bottom: 6px; }
.review-stars .star { font-size: 18px; color: #e5e7eb; }
.review-stars .star.filled { color: #f59e0b; }
.review-time { font-size: 11px; color: #9ca3af; margin-left: 8px; }
.review-content { font-size: 13px; color: #374151; margin: 0; line-height: 1.5; }
.review-empty { color: #9ca3af; font-style: italic; font-size: 12px; }

/* ===== 回复区 ===== */
.reply-section {
  flex-shrink: 0; border-top: 1px solid #e5e7eb; padding-top: 14px;
}
.reply-tip { font-size: 12px; color: #6b7280; margin-bottom: 8px; padding: 6px 10px; background: #f3f4f6; border-radius: 6px; }
.reply-tip.warning { background: #fef2f2; color: #991b1b; }
.reply-tip.info    { background: #ecfdf5; color: #065f46; }

textarea:disabled { background: #f3f4f6; cursor: not-allowed; }
</style>
