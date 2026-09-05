<template>
  <div class="floating-chat">
    <!-- 悬浮按钮 -->
    <transition name="chat-btn">
      <div v-if="!isOpen" class="chat-fab" @click="toggle">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
        </svg>
        <span class="fab-badge">AI</span>
      </div>
    </transition>

    <!-- 聊天面板 -->
    <transition name="chat-panel">
      <div v-if="isOpen" class="chat-panel">
        <!-- 头部 -->
        <div class="chat-header">
          <div class="chat-title">
            <span class="chat-avatar">{{ aiMode ? '🤖' : '👤' }}</span>
            <div class="chat-title-text">
              <strong>{{ aiMode ? '绿链智能客服' : '人工客服小绿' }}</strong>
              <span class="chat-sub">
                <template v-if="aiMode">AI 在线 · 7×24h</template>
                <template v-else-if="status === 2">本次咨询已超时关闭 · 可评价或开启新咨询</template>
                <template v-else-if="status === 3">本次咨询已完成 · 感谢联系 💛</template>
                <template v-else>人工处理中 · 工作日 9:00-18:00</template>
              </span>
            </div>
          </div>
          <!-- 关闭按钮（原来的"切回AI"按钮已按需求删除） -->
          <button class="chat-close" @click="toggle">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <!-- AI 模式：快捷问题 + 转人工按钮 -->
        <div v-if="aiMode" class="chat-shortcuts">
          <button class="shortcut-btn" @click="sendQuick('产品咨询')">💚 产品咨询</button>
          <button class="shortcut-btn" @click="sendQuick('售后政策')">🔄 售后政策</button>
          <button class="shortcut-btn" @click="transferToHuman">👤 转人工</button>
        </div>

        <!-- 人工模式：历史折叠提示 -->
        <div v-else-if="messages.length === 0" class="chat-history-tip">
          💡 您之前与 AI 的对话已作为上下文同步给客服，客服会尽快响应。
        </div>

        <!-- 消息列表 -->
        <div class="chat-messages" ref="messagesRef">
          <div v-if="messages.length === 0" class="chat-welcome">
            <p>您好！我是绿链锂电 AI 客服 👋</p>
            <p class="chat-welcome-tip">可以帮您解答产品、售后、物流等问题~</p>
          </div>

          <template v-for="(msg, idx) in messagesWithDateSeparators" :key="idx">
            <!-- 日期分隔条（不同日期的消息之间自动插入） -->
            <div v-if="msg.isDateDivider" class="date-divider">
              <div class="date-divider-line"></div>
              <span class="date-divider-label">{{ msg.label }}</span>
              <div class="date-divider-line"></div>
            </div>
            <!-- 会话分隔条（新工单开始时手动插入） -->
            <div v-else-if="msg.isSessionDivider" class="session-divider">
              <div class="divider-line"></div>
              <span class="divider-label">{{ msg.label }}</span>
              <div class="divider-line"></div>
            </div>
            <!-- 普通消息 -->
            <div v-else class="chat-msg" :class="msg.role">
              <div class="msg-avatar">
                {{ msg.role === 'user' ? '👤' : (msg.senderType === 'system' ? '💬' : (aiMode ? '🤖' : '👩‍💼')) }}
              </div>
              <div class="msg-wrap">
                <!-- 对方消息气泡 → 时间在气泡下方，支持关键词点击跳转 -->
                <template v-if="msg.role !== 'user'">
                  <div class="msg-bubble assistant-bubble" @click="onBubbleClick">
                    <span v-if="msg.loading" class="loading-dots">
                      <span></span><span></span><span></span>
                    </span>
                    <span v-else v-html="replaceNavKeywords(msg.content)"></span>
                    <div v-if="msg.hasReviewEntry" class="review-entry" @click="showReviewModal = true">
                      💚 你对小绿满意吗？<span class="review-entry-link">请点此评价</span>
                    </div>
                    <div v-if="msg.thanksReview" class="review-entry thanks">
                      ✅ 已评价，感谢你的反馈
                    </div>
                  </div>
                  <span v-if="msg.createTime" class="msg-time msg-time-below">{{ formatTime(msg.createTime) }}</span>
                </template>
                <!-- 用户自己的绿色气泡 → 时间在气泡外侧右下角 -->
                <template v-else>
                  <div class="msg-bubble">
                    <span>{{ msg.content }}</span>
                  </div>
                  <span v-if="msg.createTime" class="msg-time msg-time-below msg-time-user">{{ formatTime(msg.createTime) }}</span>
                </template>
              </div>
            </div>
          </template>
        </div>

        <!-- 输入区 -->
        <div class="chat-input-area">
          <!-- 麦克风按钮（Web Speech API，仅 Chrome/Edge 等支持 webkitSpeechRecognition 的浏览器显示） -->
          <button
            v-if="speechSupported"
            class="mic-btn"
            :class="{ recording: isRecording }"
            @click="toggleSpeech"
            :title="isRecording ? '停止录音' : '语音输入'"
          >
            <!-- 麦克风 SVG -->
            <svg v-if="!isRecording" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 1a3 3 0 00-3 3v8a3 3 0 006 0V4a3 3 0 00-3-3z"/>
              <path d="M19 10v2a7 7 0 01-14 0v-2"/>
              <line x1="12" y1="19" x2="12" y2="23"/>
              <line x1="8" y1="23" x2="16" y2="23"/>
            </svg>
            <!-- 录音中：呼吸动画小圆 -->
            <span v-else class="mic-recording-dots">
              <span></span><span></span><span></span>
            </span>
          </button>

          <textarea
            v-model="inputText"
            :placeholder="inputPlaceholder"
            rows="1"
            @keydown.enter.exact.prevent="sendMessage"
            ref="inputRef"
          ></textarea>
          <button
            class="send-btn"
            :disabled="!inputText.trim() || isSending"
            @click="sendMessage"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
            </svg>
          </button>
        </div>
      </div>
    </transition>

    <!-- ===== 评价弹窗：会话超时关闭（status=2）后自动弹出，客户提交后 status→3 ===== -->
    <transition name="chat-panel">
      <div v-if="showReviewModal" class="review-modal" @click.self="showReviewModal = false">
        <div class="review-box">
          <div class="review-title">
            <span class="review-emoji">⏱️</span>
            <div>
              <strong>会话已超时关闭</strong>
              <p class="review-sub">检测到长时间未回复，会话已自动关闭。请评价本次服务 💛</p>
            </div>
          </div>

          <!-- 星级 -->
          <div class="review-rating">
            <button
              v-for="i in 5"
              :key="i"
              class="star-btn"
              :class="{ active: reviewRating >= i }"
              @click="reviewRating = i"
            >★</button>
          </div>

          <!-- 文字（可选） -->
          <textarea
            v-model="reviewContent"
            class="review-text"
            rows="3"
            maxlength="500"
            placeholder="说点什么吧（可选，最多 500 字）..."
          ></textarea>

          <div class="review-actions">
            <button class="review-skip" @click="showReviewModal = false">稍后再说</button>
            <button class="review-submit" :disabled="reviewRating === 0" @click="doSubmitReview">
              提交评价
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index'

const router = useRouter()

// ===== 导航关键词映射表 =====
// 长关键词放前面，避免被短的先匹配（如"我的订单"不能被"订单"先吃掉）
const NAV_KEYWORDS = [
  { words: ['我的订单'], path: '/my-orders' },
  { words: ['申请售后', '售后申请', '申请退款', '退款申请', '退货申请'], path: '/my-orders' },
  { words: ['订单'], path: '/my-orders' },
  { words: ['购物车'], path: '/cart' },
  { words: ['商品详情', '产品详情'], path: '/products' },
  { words: ['商品', '产品', '商城'], path: '/products' },
  { words: ['联系我们', '客服热线', '客服电话'], path: '/contact' },
  { words: ['技术科技'], path: '/cooperation/technology' },
  { words: ['资料下载'], path: '/cooperation/download' },
  { words: ['商务合作'], path: '/cooperation' },
  { words: ['关于我们'], path: '/about' },
  { words: ['首页'], path: '/' }
]

/**
 * 把文本中的导航关键词替换成可点击的 <a data-path="xxx"> 标签
 * 只有助手消息（assistant）才会触发这个转换
 * @param {string} text 原始文本
 * @returns {string} 替换后的 HTML 字符串
 */
const replaceNavKeywords = (text) => {
  if (!text) return ''
  // 按优先级从长到短排序，确保"我的订单"在"订单"之前匹配
  const sorted = [...NAV_KEYWORDS].sort((a, b) => {
    const maxA = Math.max(...a.words.map(w => w.length))
    const maxB = Math.max(...b.words.map(w => w.length))
    return maxB - maxA
  })
  let result = text
  for (const item of sorted) {
    for (const word of item.words) {
      // 用 replaceAll 替换所有出现的地方，包裹成可点击链接
      // 加一个特殊标记 \u0001 防止已被包裹的内容被二次包裹
      const escaped = word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      const regex = new RegExp(`(^|[^\u0001])(${escaped})`, 'g')
      result = result.replace(regex, `$1<a class="chat-nav-link" data-path="${item.path}">\u0001$2\u0001</a>`)
    }
  }
  // 清理标记字符
  return result.replace(/[\u0001]/g, '')
}

/**
 * 点击导航链接：关闭聊天面板 + router.push 跳转
 * @param {string} path 目标路由
 */
const handleNavClick = (path) => {
  isOpen.value = false
  // 延迟一帧让关闭动画先渲染，再跳转
  nextTick(() => {
    router.push(path).catch(() => {})
  })
}

/**
 * 助手消息气泡的事件委托：只处理点击在 .chat-nav-link 上的情况
 */
const onBubbleClick = (e) => {
  const target = e.target
  if (target && target.classList && target.classList.contains('chat-nav-link')) {
    const path = target.getAttribute('data-path')
    if (path) {
      e.preventDefault()
      e.stopPropagation()
      handleNavClick(path)
    }
  }
}

// ===== 常量 =====
const SESSION_KEY = 'coze_session_id'
const SERVICE_REQ_KEY = 'service_request_id'       // 当前活跃工单ID
const SERVICE_REQ_IDS_KEY = 'service_request_ids'   // 所有历史工单ID列表（JSON数组，永久保存）
const POLL_INTERVAL = 2000
const IDLE_TIMEOUT_MS = 10_000   // 闲置超时阈值（秒 × 1000）

// ===== 工具函数 =====
/** 安全解析后端时间：Java LocalDateTime JSON = "2026-09-02T21:22:33"（无时区），补 Z 避免被当 UTC */
const safeParseDate = (isoStr) => {
  if (!isoStr) return null
  // 去掉可能的毫秒部分再补本地时区，避免某些浏览器把无时区字符串当 UTC
  const normalized = isoStr.includes('T')
    ? isoStr.replace('T', ' ').replace(/\..*$/, '')   // "2026-09-02 21:22:33"
    : isoStr
  const d = new Date(normalized.replace(/-/g, '/'))    // Safari 兼容 "2026/09/02 21:22:33"
  return isNaN(d.getTime()) ? null : d
}
/** 生成本地时间字符串（YYYY-MM-DDTHH:mm:ss），不带时区后缀，避免 UTC 转本地时被二次偏移 8 小时 */
const nowLocal = () => {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
/** 消息时间戳：统一 HH:mm（和管理后台一致） */
const formatTime = (isoStr) => {
  const d = safeParseDate(isoStr)
  if (!d) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}
/** 日期分隔条用的标签：今天 / 昨天 / M月D日 / YYYY年M月D日 */
const formatDateLabel = (isoStr) => {
  const d = safeParseDate(isoStr)
  if (!d) return ''
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  const isYesterday = new Date(now - 86400000).toDateString() === d.toDateString()
  if (isToday) return '今天'
  if (isYesterday) return '昨天'
  const isThisYear = d.getFullYear() === now.getFullYear()
  if (isThisYear) return `${d.getMonth() + 1}月${d.getDate()}日`
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}
/** 比较两条消息是否同一天（用于自动插入日期分隔） */
const isSameDay = (a, b) => {
  if (!a || !b) return true
  const da = safeParseDate(a)
  const db = safeParseDate(b)
  if (!da || !db) return true
  return da.toDateString() === db.toDateString()
}
/** 从 localStorage 读取所有历史工单ID */
const getSavedRequestIds = () => {
  try {
    const raw = localStorage.getItem(SERVICE_REQ_IDS_KEY)
    if (!raw) {
      // 兼容旧版本：只有 SERVICE_REQ_KEY
      const oldId = localStorage.getItem(SERVICE_REQ_KEY)
      if (oldId) return [Number(oldId)]
      return []
    }
    return JSON.parse(raw).map(Number)
  } catch { return [] }
}
/** 把工单ID保存到历史列表（去重，最新的排末尾） */
const saveRequestId = (id) => {
  if (!id) return
  const list = getSavedRequestIds().filter(x => x !== id)
  list.push(id)
  localStorage.setItem(SERVICE_REQ_IDS_KEY, JSON.stringify(list))
  localStorage.setItem(SERVICE_REQ_KEY, String(id))  // 同时更新"当前活跃"标记
}

// ===== 日期自动分隔：在消息列表中，当日期变化时自动插入日期分隔条 =====
const messagesWithDateSeparators = computed(() => {
  const result = []
  let lastDateKey = null   // 上一条消息的日期 key（YYYY-MM-DD）
  for (const msg of messages.value) {
    // 会话分隔条本身也是分隔符，重置 lastDateKey
    if (msg.isSessionDivider) {
      result.push(msg)
      lastDateKey = null
      continue
    }
    // 如果有 createTime，检查是否需要插入日期分隔
    if (msg.createTime) {
      const d = safeParseDate(msg.createTime)
      if (d) {
        const dateKey = `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`
        if (lastDateKey !== null && dateKey !== lastDateKey) {
          result.push({
            isDateDivider: true,
            label: formatDateLabel(msg.createTime),
            createTime: msg.createTime
          })
        }
        lastDateKey = dateKey
      }
    }
    result.push(msg)
  }
  return result
})

// ===== UI 状态 =====
const isOpen = ref(false)
const inputText = ref('')
const isSending = ref(false)
const messages = ref([])
const messagesRef = ref(null)
const inputRef = ref(null)
let sessionId = ''

// ===== AI 转人工状态 =====
const aiMode = ref(true)
const serviceRequestId = ref(null)
const status = ref(0)   // 0=待应答 1=活跃中 2=超时关闭(待评价) 3=归档完成
let pollTimer = null
let postReplyTimer = null   // AI 回复后延迟弹窗引导转人工
let lastMsgId = 0

// ===== 闲置计时 =====
let idleTimer = null
const resetIdleTimer = () => {
  if (!aiMode.value && status.value < 2) {
    startIdleTimer()
  }
}
const startIdleTimer = () => {
  stopIdleTimer()
  idleTimer = setTimeout(onIdleTimeout, IDLE_TIMEOUT_MS)
}
const stopIdleTimer = () => {
  if (idleTimer) { clearTimeout(idleTimer); idleTimer = null }
}
/** 超时触发：通知后端置 status=2 → 弹出评价表单 → 停止轮询 */
const onIdleTimeout = async () => {
  if (!serviceRequestId.value || status.value >= 2) return
  // 前端先停轮询、禁用输入
  status.value = 2
  stopPolling()
  try {
    await api.timeoutCloseServiceRequest(serviceRequestId.value)
  } catch { /* ignore */ }
  // 追加一条带"评价入口"的系统提示（不自动弹窗，用户点击才弹）
  messages.value.push({
    role: 'assistant', senderType: 'system',
    content: '⏱️ 检测长时间未回复，会话已自动关闭。如有问题请重新发起咨询。',
    hasReviewEntry: true,
    createTime: nowLocal()
  })
  scrollToBottom()
}

// ===== 输入区占位 / 禁用控制 =====
const inputPlaceholder = computed(() => {
  if (aiMode.value) return '输入您的问题...'
  if (status.value === 2 || status.value === 3) return '开启新一轮咨询'
  return '给人工客服发送消息...'
})
const inputDisabled = ref(false)   // 不再因 status≥2 禁用（除非发送中）

// ===== 语音转文字（Web Speech API） =====
// 仅 Chrome/Edge 支持 webkitSpeechRecognition，Safari 用 SpeechRecognition，Firefox 不支持
// 浏览器不支持时 speechSupported=false，麦克风按钮不会渲染出来
const speechSupported = ref(false)
const isRecording = ref(false)
let recognition = null    // webkitSpeechRecognition 实例

/** 初始化语音识别能力：浏览器支持时才创建 recognition 实例 */
const initSpeechRecognition = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    speechSupported.value = false
    return
  }
  speechSupported.value = true
  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'           // 中文识别
  recognition.continuous = true          // 持续识别（按按钮期间一直听）
  recognition.interimResults = true      // 实时显示中间结果（边说边转）
  recognition.maxAlternatives = 1        // 只返回最可能的一个结果

  // result 事件：每次识别到新的文本片段
  recognition.onresult = (event) => {
    let transcript = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      transcript += event.results[i][0].transcript
    }
    // 把识别结果填充到输入框（替换或追加，这里用追加更自然）
    inputText.value = inputText.value.trim() + ' ' + transcript
    inputText.value = inputText.value.trim()
  }

  // end 事件：识别结束（用户停止说话或超时）
  recognition.onend = () => {
    isRecording.value = false
  }

  // error 事件：识别出错（麦克风权限被拒绝等）
  recognition.onerror = (e) => {
    isRecording.value = false
    if (e.error === 'not-allowed' || e.error === 'service-not-allowed') {
      alert('麦克风权限被拒绝，请在浏览器设置中允许后重试')
    } else if (e.error !== 'aborted') {
      console.warn('[SpeechRecognition] error:', e.error)
    }
  }
}

/** 切换录音状态：点一次开始，再点一次停止 */
const toggleSpeech = () => {
  if (!recognition) return
  if (isRecording.value) {
    recognition.stop()
    isRecording.value = false
  } else {
    try {
      recognition.start()
      isRecording.value = true
    } catch (e) {
      // 已经在录音时 start 会抛错，忽略
      console.warn('[SpeechRecognition] start failed:', e)
    }
  }
}

// ===== 评价弹窗 =====
const showReviewModal = ref(false)
const reviewRating = ref(0)
const reviewContent = ref('')

const doSubmitReview = async () => {
  if (!serviceRequestId.value || reviewRating.value === 0) return
  try {
    await api.submitServiceReview(serviceRequestId.value, {
      rating: reviewRating.value,
      content: reviewContent.value.trim()
    })
    status.value = 3
    showReviewModal.value = false
    // 把 messages 中带 hasReviewEntry 的系统消息替换成"感谢你的评价"
    const idx = messages.value.findIndex(m => m.hasReviewEntry)
    if (idx >= 0) {
      messages.value.splice(idx, 1, {
        role: 'assistant', senderType: 'system',
        content: '💚 感谢你的评价，祝你生活愉快！',
        thanksReview: true,
        createTime: nowLocal()
      })
    }
  } catch (e) {
    alert(e?.response?.data?.message || '评价提交失败，请稍后再试')
  }
}

/** 生成或恢复 Coze 会话 ID（localStorage 持久化） */
const initSessionId = () => {
  sessionId = localStorage.getItem(SESSION_KEY)
  if (!sessionId) {
    sessionId = 'web-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
    localStorage.setItem(SESSION_KEY, sessionId)
  }
}

/** 恢复人工客服会话（刷新页面后继续 + 永久保存所有历史工单） */
const restoreHumanMode = async () => {
  // 未登录时不恢复任何历史消息，保持空的欢迎界面
  if (!localStorage.getItem('token')) return
  const allIds = getSavedRequestIds()
  if (allIds.length === 0) return  // 从未转过人工，跳过恢复

  // 取最后一次（最新的）工单设为当前活跃；老工单保留历史展示
  serviceRequestId.value = allIds[allIds.length - 1]
  aiMode.value = false   // 先进入人工模式加载历史，后面如果 status>=2 再切回 AI

  try {
    // 1) 拉取所有工单的详情和消息
    const allMsgs = []
    let currentStatus = 0
    let latestMsgId = 0
    const requestIdToFirstMsgTime = {}

    for (const rid of allIds) {
      try {
        const d = await api.getServiceRequestDetail(rid)
        if (rid === serviceRequestId.value && d?.code === 200 && d.data) {
          currentStatus = d.data.status ?? 0
        }
      } catch { /* ignore */ }
      try {
        const res = await api.pullServiceMessages(rid, 0)
        if (res?.code === 200 && res.data?.length) {
          const sorted = res.data.slice().sort((a, b) => (a.createTime || '').localeCompare(b.createTime || ''))
          requestIdToFirstMsgTime[rid] = sorted[0].createTime
          sorted.forEach(m => {
            if (m.id > latestMsgId) latestMsgId = m.id
            allMsgs.push({
              role: m.senderType === 'customer' ? 'user' : 'assistant',
              senderType: m.senderType,
              content: m.content,
              id: m.id,
              createTime: m.createTime,
              requestId: rid
            })
          })
        }
      } catch { /* ignore */ }
    }

    // 2) 全局按时间排序
    allMsgs.sort((a, b) => (a.createTime || '').localeCompare(b.createTime || ''))

    // 3) 插入会话分隔条
    const finalMessages = []
    let lastRequestId = null
    for (const msg of allMsgs) {
      if (msg.requestId !== lastRequestId) {
        finalMessages.push({
          isSessionDivider: true,
          label: `💬 ${formatDateLabel(msg.createTime)} · 人工咨询`,
          createTime: msg.createTime
        })
        lastRequestId = msg.requestId
      }
      finalMessages.push(msg)
    }

    messages.value = finalMessages
    lastMsgId = latestMsgId
    status.value = currentStatus

    // 标记已读 + 评价入口/感谢文案（仅对**最新工单**追加）
    if (serviceRequestId.value) {
      api.markServiceRead(serviceRequestId.value).catch(() => {})
      // 找一下历史里是否已经有评价相关消息（恢复时别重复加）
      const hasReviewMsg = messages.value.some(m => m.hasReviewEntry || m.thanksReview)
      if (!hasReviewMsg) {
        if (currentStatus === 2) {
          // 已关闭未评价 → 评价入口
          const promptedKey = 'review_prompted_' + serviceRequestId.value
          if (!sessionStorage.getItem(promptedKey)) {
            sessionStorage.setItem(promptedKey, '1')
            messages.value.push({
              role: 'assistant', senderType: 'system',
              content: '⏱️ 会话已超时关闭。',
              hasReviewEntry: true,
              createTime: nowLocal()
            })
          }
        } else if (currentStatus === 3) {
          // 已评价完成 → 显示感谢文案
          messages.value.push({
            role: 'assistant', senderType: 'system',
            content: '💚 感谢你的评价，祝你生活愉快！',
            thanksReview: true,
            createTime: nowLocal()
          })
        }
      }
    }
  } catch { /* ignore */ }

  // === 关键：会话已结束（status >= 2）则切回 AI 模式，让用户从 AI 重新开始 ===
  if (status.value >= 2) {
    aiMode.value = true
    stopPolling()
    stopIdleTimer()
  } else {
    // 未结束 → 保持人工模式并启动轮询
    startPolling()
    startIdleTimer()
  }
}

const toggle = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    nextTick(() => inputRef.value?.focus())
    if (!aiMode.value) {
      api.markServiceRead(serviceRequestId.value).catch(() => {})
      if (status.value < 2) {
        startPolling()
        startIdleTimer()
      }
    }
  } else {
    stopPolling()
    stopIdleTimer()
  }
}

/** 快捷问题直接发送 */
const sendQuick = (question) => {
  inputText.value = question
  sendMessage()
}

/** 发送消息：会话结束后自动切回 AI 模式重新开始；否则按当前模式发 */
const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || isSending.value) return

  // === 关键：上一个会话已结束（status >= 2）→ 先切回 AI 模式，让用户从 AI 重新开始 ===
  if (status.value >= 2 && !aiMode.value) {
    aiMode.value = true
    stopPolling()
    stopIdleTimer()
  }

  if (aiMode.value) {
    await sendAiMessage(text)
  } else {
    // 人工模式且 status < 2（活跃会话），正常发
    if (serviceRequestId.value) {
      await sendHumanMessage(text)
    }
  }
}

/** 旧工单已结束 → 创建新工单（幂等：同 userId 未关闭的仍会复用） */
const openNewHumanSession = async () => {
  isSending.value = true
  try {
    const chatHistory = JSON.stringify(messages.value.map(m => ({ role: m.role, content: m.content })))
    const res = await api.submitServiceRequest({
      source: 'ai_transfer',
      question: 'ai_transfer',
      content: '客户开启新一轮人工咨询',
      chatSessionId: sessionId,
      chatHistory
    })
    if (res?.code === 200) {
      const newId = res.data.id
      serviceRequestId.value = newId
      status.value = res.data.status ?? 0
      saveRequestId(newId)   // 追加到历史列表（替代原来的 localStorage.setItem 覆盖）
      // 插入新的会话分隔条
      const now = nowLocal()
      messages.value.push({
        isSessionDivider: true,
        label: `💬 ${formatDateLabel(now)} · 人工咨询`,
        createTime: now
      })
      lastMsgId = 0  // 新工单消息从 0 开始拉
      startPolling()
      startIdleTimer()
    }
  } catch { /* ignore */ }
  isSending.value = false
}

const sendAiMessage = async (text) => {
  // 从 localStorage 获取已登录用户的真实 DB ID（可选）
  // 登录状态下前端存了 userInfo JSON，解析后取 .id 字段；未登录则不传 userId
  let optionalUserId = null
  try {
    const raw = localStorage.getItem('userInfo')
    if (raw) {
      const u = JSON.parse(raw)
      if (u?.id != null) optionalUserId = String(u.id)
    }
  } catch { /* ignore */ }

  messages.value.push({ role: 'user', content: text, createTime: nowLocal() })
  messages.value.push({ role: 'assistant', content: '', loading: true })
  inputText.value = ''
  isSending.value = true
  scrollToBottom()

  try {
    // 调用后端 CozeController，参数名对齐：userQuestion + 可选 userId
    const res = await api.cozeChat({
      sessionId,
      userQuestion: text,
      ...(optionalUserId ? { userId: optionalUserId } : {})
    })
    // 返回结构：data = { answer: string, needManual: boolean }
    const answer = res?.data?.answer || '抱歉，我暂时无法回答这个问题~'
    const needManual = !!res?.data?.needManual

    const loadingIdx = messages.value.findIndex(m => m.loading)
    if (loadingIdx >= 0) {
      messages.value.splice(loadingIdx, 1, {
        role: 'assistant',
        content: answer
      })
    }

    // AI 回复命中转人工关键词 → 弹窗引导提交人工售后工单
    if (needManual) {
      if (postReplyTimer) clearTimeout(postReplyTimer)
      postReplyTimer = setTimeout(() => {
        postReplyTimer = null
        if (confirm('AI 已无法解答您的问题，是否提交人工售后工单？')) {
          transferToHuman()
        }
      }, 400)   // 延迟一下，让 AI 回复先显示出来再弹窗
    }
  } catch (e) {
    const loadingIdx = messages.value.findIndex(m => m.loading)
    if (loadingIdx >= 0) {
      messages.value.splice(loadingIdx, 1, {
        role: 'assistant',
        content: '网络似乎不太通畅，您可以稍后再试，或拨打客服电话 400-888-8888~'
      })
    }
  } finally {
    isSending.value = false
    scrollToBottom()
  }
}

const sendHumanMessage = async (text) => {
  // 先乐观 push 一条"临时消息"（没有 id）
  const optimisticIdx = messages.value.length
  messages.value.push({
    role: 'user', senderType: 'customer',
    content: text,
    createTime: nowLocal(),
    requestId: serviceRequestId.value,
    loading: true   // 标记发送中（灰色或打勾动画，可选）
  })
  inputText.value = ''
  isSending.value = true
  scrollToBottom()
  try {
    const res = await api.replyServiceRequest(serviceRequestId.value, { content: text })
    if (res?.code === 200 && res.data) {
      // === 关键：把乐观消息**原地替换**成后端返回的真实消息 ===
      // 这样 pollOnce 再拉就不会重复，时间戳也用后端权威值
      messages.value[optimisticIdx] = {
        role: 'user', senderType: 'customer',
        content: text,
        id: res.data.id,
        createTime: res.data.createTime || nowLocal(),
        requestId: serviceRequestId.value
      }
      if (res.data.id) lastMsgId = Math.max(lastMsgId, res.data.id)
      resetIdleTimer()   // 客户发消息重置闲置倒计时
    }
  } catch (e) {
    // 失败：把乐观消息标红 + 提示失败
    if (messages.value[optimisticIdx]?.loading) {
      messages.value[optimisticIdx] = {
        role: 'user', senderType: 'customer',
        content: text + ' ⚠️发送失败',
        createTime: nowLocal(),
        requestId: serviceRequestId.value
      }
    } else {
      messages.value.push({ role: 'assistant', senderType: 'system', content: '⚠️ 发送失败，请稍后重试', createTime: nowLocal() })
    }
  } finally {
    isSending.value = false
    scrollToBottom()
  }
}

/** 转人工：已结束旧工单→开新工单；活跃工单→直接切模式 */
const transferToHuman = async () => {
  // 如果有活跃工单（status < 2），直接切模式返回
  if (serviceRequestId.value && status.value < 2) {
    aiMode.value = false
    startPolling()
    startIdleTimer()
    return
  }
  // 没有工单 或 旧工单已结束(status>=2) → 创建新工单
  if (!messages.value.length) {
    alert('请先和 AI 聊几句，或直接联系人工客服热线 400-888-8888')
    return
  }
  try {
    const chatHistory = JSON.stringify(messages.value.map(m => ({ role: m.role, content: m.content })))
    const res = await api.submitServiceRequest({
      source: 'ai_transfer',
      question: 'ai_transfer',
      content: '客户从AI转人工，请查看AI对话历史了解上下文',
      chatSessionId: sessionId,
      chatHistory
    })
    if (res?.code === 200) {
      const newId = res.data.id
      serviceRequestId.value = newId
      status.value = res.data.status ?? 0
      saveRequestId(newId)
      // 插入新的会话分隔条
      const now = nowLocal()
      messages.value.push({
        isSessionDivider: true,
        label: `💬 ${formatDateLabel(now)} · 人工咨询`,
        createTime: now
      })
      aiMode.value = false
      startPolling()
      startIdleTimer()
    } else {
      alert(res?.message || '提交失败，请稍后再试')
    }
  } catch {
    alert('网络异常，请稍后再试或拨打 400-888-8888')
  }
}

/** 轮询拉取人工客服新消息 */
const startPolling = () => {
  stopPolling()
  pollTimer = setInterval(pollOnce, POLL_INTERVAL)
}
const stopPolling = () => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}
const pollOnce = async () => {
  if (!serviceRequestId.value || aiMode.value) return
  if (status.value >= 2) { stopPolling(); return }
  try {
    const res = await api.pullServiceMessages(serviceRequestId.value, lastMsgId)
    if (res?.code === 200 && res.data?.length) {
      let hasAdminMessage = false
      res.data.forEach(m => {
        if (m.id > lastMsgId) lastMsgId = m.id
        if (m.senderType === 'system') return
        if (m.senderType === 'admin') hasAdminMessage = true
        messages.value.push({
          role: m.senderType === 'customer' ? 'user' : 'assistant',
          senderType: m.senderType,
          content: m.content,
          id: m.id,
          createTime: m.createTime,   // 保留后端返回的时间戳
          requestId: serviceRequestId.value
        })
      })
      // 收到管理员新消息 → 重置闲置倒计时
      if (hasAdminMessage) resetIdleTimer()
      // 同步工单状态（管理端可能已触发超时/归档）
      api.getServiceRequestDetail(serviceRequestId.value).then(r => {
        if (r?.code === 200 && r.data?.status != null) {
          if (r.data.status !== status.value) {
            status.value = r.data.status
            if (status.value >= 2) {
              stopPolling()
              stopIdleTimer()
              // 轮询发现工单被超时关闭（如管理端触发）→ 追加评价入口，不自动弹窗
              if (status.value === 2) {
                messages.value.push({
                  role: 'assistant', senderType: 'system',
                  content: '⏱️ 会话已超时关闭。',
                  hasReviewEntry: true,
                  createTime: nowLocal()
                })
                scrollToBottom()
              }
            }
          }
        }
      }).catch(() => {})
      api.markServiceRead(serviceRequestId.value).catch(() => {})
      scrollToBottom()
    }
  } catch { /* ignore */ }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

/** 退出登录时：清空内存消息 + 清除聊天相关 localStorage */
const onAuthCleared = () => {
  // 停掉所有后台任务
  stopPolling()
  stopIdleTimer()
  if (postReplyTimer) { clearTimeout(postReplyTimer); postReplyTimer = null }
  // 清空内存状态
  messages.value = []
  serviceRequestId.value = null
  status.value = 0
  aiMode.value = true
  isSending.value = false
  showReviewModal.value = false
  // 清除聊天相关的 localStorage（但保留 SESSION_KEY，让 AI 会话 ID 继续复用）
  localStorage.removeItem(SERVICE_REQ_KEY)
  localStorage.removeItem(SERVICE_REQ_IDS_KEY)
  // 刷新 sessionId（避免新用户看到旧 AI 会话）
  sessionId = ''
}

/** 登录成功时：重新尝试恢复历史消息 */
const onLoginSuccess = () => {
  // 重置状态后重新恢复
  messages.value = []
  serviceRequestId.value = null
  status.value = 0
  aiMode.value = true
  nextTick(() => {
    restoreHumanMode()
  })
}

onMounted(() => {
  initSessionId()
  restoreHumanMode()
  // 初始化 Web Speech API 语音识别能力（浏览器支持时才生效）
  initSpeechRecognition()
  // 监听登录/退出事件
  window.addEventListener('gc-auth-cleared', onAuthCleared)
  window.addEventListener('gc-login-success', onLoginSuccess)
})
onUnmounted(() => {
  stopPolling()
  stopIdleTimer()
  // 清理 AI 转人工延迟弹窗定时器（组件销毁后不应再弹出）
  if (postReplyTimer) { clearTimeout(postReplyTimer); postReplyTimer = null }
  // 清理语音识别实例（避免组件销毁后 recognition.onend 回调访问已销毁状态）
  if (recognition) {
    try { recognition.stop() } catch (e) { /* ignore */ }
    recognition = null
  }
  // 移除事件监听
  window.removeEventListener('gc-auth-cleared', onAuthCleared)
  window.removeEventListener('gc-login-success', onLoginSuccess)
})
</script>

<style scoped>
/* 位置计算（桌面端）：
   float-toolbar [返回顶部→电话] flex-col gap:82 bottom:32 按钮50px
   电话：bottom 32~82
   返回顶部：bottom 164~214
   AI（50px）精确卡中间：电话顶82 + 16间距 = AI底98 → AI 98~148
   视觉从上到下：返回顶部 → AI → 电话 */
.floating-chat {
  position: fixed;
  right: 24px;
  bottom: 98px;
  z-index: 10001;
}

/* 悬浮按钮 —— 和 toolbar-btn 一样大（50×50），视觉统一 */
.chat-fab {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(5, 150, 105, 0.4);
  transition: all 0.25s ease;
  position: relative;
}
.chat-fab:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 20px rgba(5, 150, 105, 0.5);
}
.chat-fab svg { width: 22px; height: 22px; color: white; }
.fab-badge {
  position: absolute;
  top: -2px; right: -2px;
  background: #ef4444; color: white;
  font-size: 9px; font-weight: 700;
  padding: 1px 5px; border-radius: 8px;
  border: 2px solid white;
}

/* 面板（缩小版：更紧凑不挡内容） */
.chat-panel {
  width: 280px; height: 400px;
  background: white; border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
  display: flex; flex-direction: column;
  overflow: hidden; position: relative;
}

/* 头部 */
.chat-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 12px;
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  color: white; flex-shrink: 0;
}
.chat-title { display: flex; align-items: center; gap: 8px; }
.chat-avatar { font-size: 20px; }
.chat-title-text strong { display: block; font-size: 14px; }
.chat-sub { font-size: 10px; opacity: 0.85; }

.chat-close {
  width: 28px; height: 28px;
  border: none; background: rgba(255, 255, 255, 0.2);
  border-radius: 50%; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  color: white; transition: background 0.15s;
}
.chat-close:hover { background: rgba(255, 255, 255, 0.35); }
.chat-close svg { width: 14px; height: 14px; }

/* 快捷问题 */
.chat-shortcuts {
  display: flex; gap: 6px; padding: 8px 10px;
  background: #f9fafb; border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.chat-history-tip {
  padding: 8px 12px;
  background: #eff6ff; border-bottom: 1px solid #dbeafe;
  font-size: 11px; color: #1e40af; line-height: 1.5;
  flex-shrink: 0;
}
.shortcut-btn {
  flex: 1; padding: 5px 3px;
  border: 1px solid #d1d5db; background: white;
  border-radius: 6px; font-size: 11px; color: #374151;
  cursor: pointer; transition: all 0.15s; white-space: nowrap;
}
.shortcut-btn:hover {
  border-color: #059669; color: #059669; background: #ecfdf5;
}

/* 消息列表 */
.chat-messages {
  flex: 1; overflow-y: auto;
  padding: 10px; background: #f9fafb;
}
.chat-welcome { text-align: center; color: #6b7280; padding: 24px 10px; }
.chat-welcome p { margin: 4px 0; }
.chat-welcome-tip { font-size: 11px; color: #9ca3af; }

.chat-msg { display: flex; gap: 0; margin-bottom: 10px; align-items: flex-start; }
.chat-msg.user { flex-direction: row-reverse; }

/* 头像已删除，保留占位为 0（CSS 层面不再显示） */
.msg-avatar {
  display: none;
}
/* 包裹气泡 + 时间戳的中间层：头像删了，气泡可以占满更多宽度 */
.msg-wrap {
  display: flex; flex-direction: column; align-items: flex-start;
  max-width: 85%;
}
.chat-msg.user .msg-wrap { align-items: flex-end; }

.msg-bubble {
  padding: 8px 12px;
  border-radius: 10px; font-size: 12.5px; line-height: 1.5;
  word-break: break-word;
}
.chat-msg.user .msg-bubble {
  background: #059669; color: white; border-top-right-radius: 4px;
}
.chat-msg.assistant .msg-bubble {
  background: white; color: #1f2937; border: 1px solid #e5e7eb; border-top-left-radius: 4px;
}

/* 助手消息气泡内的导航关键词链接 */
.chat-nav-link {
  color: #0d9488;
  font-weight: 500;
  cursor: pointer;
  text-decoration: underline;
  text-decoration-style: dotted;
  text-underline-offset: 3px;
  transition: all 0.15s;
  border-radius: 3px;
  padding: 0 2px;
}
.chat-nav-link:hover {
  color: #0f766e;
  background: #ecfdf5;
  text-decoration-style: solid;
}

/* 时间戳基础样式：简约商务 — 更小、更淡 */
.msg-time {
  font-size: 10px;
  line-height: 1;
  user-select: none;
  white-space: nowrap;
  margin-top: 4px;
}

/* 对方/系统消息：气泡下方靠左，灰色 */
.msg-time-below {
  color: #cbd5e1;
}

/* 用户消息：气泡下方靠右，浅灰色（不抢视觉） */
.msg-time-user {
  color: #cbd5e1;   /* 和对方消息同一个极淡灰，但位置靠右 */
}

/* 日期分隔条（微信风格，不同日期自动插入） */
.date-divider {
  display: flex; align-items: center; gap: 8px;
  margin: 10px 0 6px;
}
.date-divider-line {
  flex: 1; height: 1px; background: #e5e7eb;
}
.date-divider-label {
  font-size: 11px; color: #9ca3af;
  background: #f9fafb;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
}

/* 会话分隔条（不同工单之间，带青色强调） */
.session-divider {
  display: flex; align-items: center; gap: 10px;
  margin: 16px 0 10px;
}
.divider-line {
  flex: 1; height: 1px;
  background: linear-gradient(90deg, transparent, #0d9488 30%, #0d9488 70%, transparent);
}
.divider-label {
  font-size: 11px;
  color: #0d9488;
  font-weight: 500;
  background: #ecfdf5;
  padding: 3px 10px;
  border-radius: 12px;
  white-space: nowrap;
}

/* loading dots */
.loading-dots { display: inline-flex; gap: 3px; }
.loading-dots span {
  width: 6px; height: 6px; background: #9ca3af; border-radius: 50%;
  animation: dotBounce 1.4s infinite ease-in-out both;
}
.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }
@keyframes dotBounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* 评价入口：系统消息气泡内的可点击链接 */
.review-entry {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e5e7eb;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
}
.review-entry-link {
  color: #0d9488;
  font-weight: 500;
  cursor: pointer;
  text-decoration: underline;
  transition: color 0.15s;
}
.review-entry-link:hover {
  color: #0f766e;
}
/* 已评价感谢样式 */
.review-entry.thanks {
  border-top-style: solid;
  border-top-color: #d1fae5;
  color: #059669;
  cursor: default;
}

/* 输入区 */
.chat-input-area {
  display: flex; gap: 6px; padding: 8px 10px;
  background: white; border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.chat-input-area textarea {
  flex: 1; border: 1px solid #d1d5db; border-radius: 8px;
  padding: 6px 10px; font-size: 12px; resize: none;
  max-height: 64px; font-family: inherit; line-height: 1.4;
}
.chat-input-area textarea:focus {
  outline: none; border-color: #059669;
  box-shadow: 0 0 0 2px rgba(5, 150, 105, 0.1);
}

/* 麦克风按钮（Web Speech API） */
.mic-btn {
  width: 32px; height: 32px;
  border: 1px solid #d1d5db; background: white;
  border-radius: 8px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  color: #6b7280; transition: all 0.15s; flex-shrink: 0;
  padding: 0;
}
.mic-btn svg { width: 16px; height: 16px; }
.mic-btn:hover {
  border-color: #059669; color: #059669; background: #ecfdf5;
}
/* 录音中：绿色呼吸边框 + 背景 */
.mic-btn.recording {
  background: #059669; border-color: #059669; color: white;
  animation: micPulse 1.2s ease-in-out infinite;
}
@keyframes micPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(5, 150, 105, 0.5); }
  50%      { box-shadow: 0 0 0 8px rgba(5, 150, 105, 0); }
}
/* 录音中三点动画 */
.mic-recording-dots {
  display: inline-flex; gap: 3px;
}
.mic-recording-dots span {
  width: 5px; height: 5px; background: white; border-radius: 50%;
  animation: dotBounce 1.2s infinite ease-in-out both;
}
.mic-recording-dots span:nth-child(1) { animation-delay: -0.32s; }
.mic-recording-dots span:nth-child(2) { animation-delay: -0.16s; }

.send-btn {
  width: 32px; height: 32px;
  border: none; background: #059669; border-radius: 8px;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  color: white; transition: all 0.15s; flex-shrink: 0;
}
.send-btn:hover:not(:disabled) { background: #047857; }
.send-btn:disabled { background: #d1d5db; cursor: not-allowed; }
.send-btn svg { width: 16px; height: 16px; }

/* ===== 评价弹窗 ===== */
.review-modal {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,.45);
  display: flex; align-items: center; justify-content: center;
  z-index: 10002;
}
.review-box {
  width: 340px; padding: 22px;
  background: white; border-radius: 16px;
  box-shadow: 0 16px 40px rgba(0,0,0,.2);
}
.review-title {
  display: flex; gap: 12px; align-items: flex-start; margin-bottom: 16px;
}
.review-emoji { font-size: 30px; }
.review-title strong { display: block; font-size: 15px; color: #1f2937; }
.review-sub { font-size: 12px; color: #6b7280; margin: 4px 0 0; }

.review-rating {
  display: flex; justify-content: center; gap: 4px; margin: 12px 0;
}
.star-btn {
  font-size: 32px; background: transparent; border: none;
  cursor: pointer; color: #d1d5db; transition: transform .15s;
  padding: 0 2px; line-height: 1;
}
.star-btn:hover { transform: scale(1.15); }
.star-btn.active { color: #f59e0b; }

.review-text {
  width: 100%; padding: 8px 12px;
  border: 1px solid #d1d5db; border-radius: 10px;
  font-size: 13px; resize: none; line-height: 1.5;
  box-sizing: border-box;
}
.review-text:focus { outline: none; border-color: #0d9488; }

.review-actions {
  display: flex; gap: 10px; justify-content: flex-end; margin-top: 14px;
}
.review-skip {
  padding: 8px 16px; border: 1px solid #d1d5db; border-radius: 8px;
  background: white; color: #6b7280; cursor: pointer; font-size: 13px;
}
.review-skip:hover { background: #f3f4f6; }
.review-submit {
  padding: 8px 20px; border: none; border-radius: 8px;
  background: #0d9488; color: white; font-size: 13px;
  cursor: pointer; font-weight: 500;
}
.review-submit:hover:not(:disabled) { background: #0f766e; }
.review-submit:disabled { background: #d1d5db; cursor: not-allowed; }

/* 过渡动画 */
.chat-btn-enter-active, .chat-btn-leave-active { transition: all 0.25s ease; }
.chat-btn-enter-from, .chat-btn-leave-to { opacity: 0; transform: scale(0.8) translateY(10px); }
.chat-panel-enter-active, .chat-panel-leave-active { transition: all 0.25s ease; }
.chat-panel-enter-from, .chat-panel-leave-to { opacity: 0; transform: scale(0.9) translateY(10px); }

/* 移动端适配（toolbar 46px按钮, gap:78, bottom:24）
   电话 bottom 24~70，返回顶部 bottom 148~194
   AI（46px）卡中间：电话顶70 + 16 = AI底86 → AI 86~132 */
@media (max-width: 640px) {
  .floating-chat { right: 16px; bottom: 86px; }
  .chat-fab { width: 46px; height: 46px; }
  .chat-fab svg { width: 20px; height: 20px; }
  .chat-panel { width: calc(100vw - 32px); height: 400px; right: 0; bottom: 0; }
}
</style>

