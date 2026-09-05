import axios from 'axios'

const API_BASE_URL = '/api'

/**
 * 全局一次性 401 防护：避免同一个请求周期里多个并发接口都返回 code 401，
 * 连弹多次"登录已失效"并反复跳转首页。
 */
let _global401Handled = false
const markAndFlush401 = () => {
  if (_global401Handled) return false
  _global401Handled = true
  // 150ms 后解锁（跳转/刷新后页面会被换掉，不设也无所谓，加了更保险）
  setTimeout(() => { _global401Handled = false }, 150)
  return true
}

/** 统一清理客户端登录态（买家端 + 管理后台所有相关缓存键） */
export const clearClientAuth = (showAlert = true) => {
  localStorage.removeItem('token')
  localStorage.removeItem('admin_token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('admin_username')
  localStorage.removeItem('admin_permissions')
  localStorage.removeItem('admin_role_id')
  localStorage.removeItem('admin_role_name')
  // 派发自定义事件，让 Header / LoginModal / Cart 等组件同步刷新 UI
  try {
    window.dispatchEvent(new CustomEvent('gc-auth-cleared'))
  } catch (e) { /* ignore */ }
  if (showAlert && markAndFlush401()) {
    alert('登录已失效，请重新登录')
  }
  // 仅在非登录页时跳首页，避免登录页弹窗后原地踏步
  if (location.hash && location.hash !== '#/admin/login') {
    window.location.href = '/'
  }
}

const instance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token') || localStorage.getItem('admin_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

instance.interceptors.response.use(
  (response) => {
    const data = response.data
    // ------- 业务层 401：HTTP 200，但 body 里 code===401（AdminAuthInterceptor 返回体格式） -------
    // 注意：不能简单把所有 code !== 200 都当错，因为 /client/upload/** 返回 {code:200}、
    // 商品详情 {code:200,data:...} 都是正常；其他业务 code 由页面自行处理。
    if (data && typeof data === 'object' && Number(data.code) === 401) {
      clearClientAuth(true)
      // Promise.reject 到 catch 链，页面里的 if(res.code!==200) 分支不会再被意外走到
      return Promise.reject(new Error(data.message || '登录已失效'))
    }
    return data
  },
  (error) => {
    console.error('API Error:', error)

    // 传输层 401：与业务层 401 同一套清理逻辑
    if (error.response && error.response.status === 401) {
      clearClientAuth(true)
    }

    return Promise.reject(error)
  }
)

export const api = {
  login: (data) => instance.post('/client/auth/login', data),
  register: (data) => instance.post('/client/auth/register', data),
  
  getProducts: (params) => instance.get('/client/products', { params }),
  getProductById: (id) => instance.get(`/client/products/${id}`),
  // 同系列兄弟规格（含已下架 status=0，已删除的物理不存在不返回），供详情页规格选择器全量展示+置灰
  getProductSiblings: (id) => instance.get(`/client/products/${id}/siblings`),
  getCategories: () => instance.get('/client/categories'),
  getCarousel: () => instance.get('/client/carousel'),
  
  getCartList: () => instance.get('/client/cart'),
  addCart: (data) => instance.post('/client/cart', data),
  updateCart: (id, data) => instance.put(`/client/cart/${id}`, data),
  deleteCart: (id) => instance.delete(`/client/cart/${id}`),
  clearCart: () => instance.delete('/client/cart/clear'),
  // 游客购物车合并（登录后调用，必须携带 token）
  mergeGuestCart: (data) => instance.post('/client/cart/mergeGuestCart', data),
  
  // 创建订单（config 可传 headers 携带 Idempotent-Token 幂等令牌）
  createOrder: (data, config) => instance.post('/client/orders', data, config),
  // 获取幂等令牌（下单前调用，放入请求头 Idempotent-Token，防重复提交）
  getIdempotentToken: () => instance.get('/client/idempotent/token'),

  // ===== 商品热销榜 & 用户足迹 =====
  // 热销榜 TOP10（公开接口，Redis ZSet rank:product:sales 倒序）
  getRankTop10: () => instance.get('/client/products/rank/top10'),
  // 当前用户浏览足迹（最近 20 条，需登录）
  getFootprint: () => instance.get('/client/user/footprint'),

  // ===== Coze 绿链锂电智能体小绿客服 =====
  // 智能客服对话（公开接口，无需登录）
  // data 结构：{ sessionId: string (必填,前端生成UUID), userQuestion: string (必填,用户问题), userId: string (可选,已登录用户真实DB ID) }
  // 返回 Result.data = { answer: string (AI回复), needManual: boolean (是否命中转人工关键词) }
  cozeChat: (data) => instance.post('/client/coze/chat', data),
  getOrderList: (params) => instance.get('/client/orders', { params }),
  getOrderById: (id) => instance.get(`/client/orders/${id}`),
  // 发起支付（真实支付打通）：返回 { paymentNo, amount, payUrl, status }，订单仍为 pending，待回调置 paid
  payOrder: (id) => instance.post(`/client/orders/${id}/pay`),
  // 等价入口：新支付模块的预下单接口（等价于 payOrder，可二选一）
  createPayment: (orderId) => instance.post('/client/payment/create', { orderId }),
  // 本地演示：模拟用户在支付网关"支付成功"，走一遍验签回调后返回订单状态
  simulatePay: (paymentNo) => instance.post(`/client/payment/${paymentNo}/simulate-pay`),
  cancelOrder: (id) => instance.post(`/client/orders/${id}/cancel`),
  confirmReceive: (id) => instance.post(`/client/orders/${id}/confirm`),
  
  getUserInfo: () => instance.get('/client/user'),
  updateUser: (data) => instance.put('/client/user', data),
  getAddresses: () => instance.get('/client/user/addresses'),
  addAddress: (data) => instance.post('/client/user/addresses', data),
  updateAddress: (id, data) => instance.put(`/client/user/addresses/${id}`, data),
  deleteAddress: (id) => instance.delete(`/client/user/addresses/${id}`),
  
  submitNegotiation: (data) => instance.post('/client/negotiation/submit', data),
  getNegotiationByOrderId: (orderId) => instance.get(`/client/negotiation/order/${orderId}`),
  
  getWorkshopList: (params) => instance.get('/client/workshop/list', { params }),
  getWorkshopById: (id) => instance.get(`/client/workshop/${id}`),
  getWorkshop3D: (id) => instance.get(`/client/workshop/${id}/3d`),

  // ===== 工序流程（Neo4j 真实数据） =====
  // 所有工序节点列表
  getProcessList: () => instance.get('/client/process/list'),
  // 完整工序链路（含 nodes / edges，前端直接可用于图谱渲染）
  getProcessChain: () => instance.get('/client/process/chain'),
  // 单工序详情
  getProcessById: (id) => instance.get(`/client/process/${id}`),
  // 某节点后续所有工序
  getProcessNext: (id) => instance.get(`/client/process/${id}/next`),
  // 图谱统计（节点数 / 关系数）
  getProcessInfo: () => instance.get('/client/process/info'),
  // ===== 数字孪生图算法（Neo4j 原生 Cypher） =====
  // 图算法总览：KPI + 风险传播链 + 设备冲突 + 产品追溯清单
  getProcessInsightOverview: () => instance.get('/client/process/insight/overview'),
  // 影响范围推演：某工序异常后下游受影响工序 + 产品 + 风险损失
  getProcessImpact: (id) => instance.get(`/client/process/insight/impact/${id}`),
  // 节点全维度详情：设备 / 风险 / 缺陷 / 产品
  getProcessNodeInsight: (id) => instance.get(`/client/process/insight/node/${id}`),
  // 前台附件上传（售后凭证图片等），file 为 File 对象；FormData 请求由浏览器自动设置 Content-Type(boundary)
  uploadAttachment: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return instance.post('/client/upload/attachment', formData, {
      headers: { 'Content-Type': undefined }
    })
  },
  // ===== 售后模块（买家端：/client/afterSale/*） =====
  // 发起售后申请（仅已确认收货/已付款订单，一个订单仅一次）
  applyAfterSale: (data) => instance.post('/client/afterSale/apply', data),
  // 我的售后列表
  getMyAfterSales: () => instance.get('/client/afterSale/myList'),
  // 售后详情
  getAfterSaleDetail: (id) => instance.get(`/client/afterSale/${id}`),

  // ===== 商务合作 / 人工客服（客户端） =====
  submitCooperation: (data) => instance.post('/client/cooperation/submit', data),
  getMyCooperations: () => instance.get('/client/cooperation/mine'),
  submitServiceRequest: (data) => instance.post('/client/service-request/submit', data),
  getMyServiceRequests: () => instance.get('/client/service-request/mine'),
  // AI 转人工双向聊天
  replyServiceRequest: (id, data) => instance.post(`/client/service-request/${id}/reply`, data),
  pullServiceMessages: (id, lastMsgId) => instance.get(`/client/service-request/${id}/messages`, { params: { lastMsgId } }),
  markServiceRead: (id) => instance.put(`/client/service-request/${id}/read`),
  getServiceRequestDetail: (id) => instance.get(`/client/service-request/${id}`),
  // 闲置超时关闭（前端 10 秒无消息触发，后端置 status=2）
  timeoutCloseServiceRequest: (id) => instance.put(`/client/service-request/${id}/timeout-close`),
  // 客户评价（会话关闭后提交，status 2→3）
  submitServiceReview: (id, data) => instance.post(`/client/service-request/${id}/review`, data),
  getServiceReview: (id) => instance.get(`/client/service-request/${id}/review`)
}

export const adminApi = {
  
  getDashboard: () => instance.get('/admin/dashboard'),
  
  getCategories: (params) => instance.get('/admin/category/page', { params }),
  createCategory: (data) => instance.post('/admin/category', data),
  updateCategory: (id, data) => instance.put(`/admin/category/${id}`, data),
  deleteCategory: (id) => instance.delete(`/admin/category/${id}`),
  
  getProducts: (params) => instance.get('/admin/product/page', { params }),
  getProduct: (id) => instance.get(`/admin/product/${id}`),
  createProduct: (data) => instance.post('/admin/product/add', data),
  updateProduct: (data) => instance.put('/admin/product/update', data),
  deleteProduct: (id) => instance.delete(`/admin/product/${id}`),
  onShelfProduct: (id) => instance.put(`/admin/product/${id}/on-shelf`),
  offShelfProduct: (id) => instance.put(`/admin/product/${id}/off-shelf`),
  
  getOrders: (params) => instance.get('/admin/order/page', { params }),
  updateOrder: (id, data) => instance.put(`/admin/order/${id}`, data),
  deleteOrder: (id) => instance.delete(`/admin/order/${id}`),
  
  getUsers: (params) => instance.get('/admin/user/page', { params }),
  updateUser: (id, data) => instance.put(`/admin/user/${id}`, data),
  deleteUser: (id) => instance.delete(`/admin/user/${id}`),

  // ===== 资讯管理 =====
  // 后端 AdminNewsController GET /api/admin/news：params: current, size, keyword, category, status
  getNews: (params) => instance.get('/admin/news', { params }),
  createNews: (data) => instance.post('/admin/news', data),
  updateNews: (id, data) => instance.put(`/admin/news/${id}`, data),
  deleteNews: (id) => instance.delete(`/admin/news/${id}`),

  // ===== 轮播管理 =====
  // 后端 AdminCarouselController GET /api/admin/carousels：params: current, size, status
  getCarousels: (params) => instance.get('/admin/carousels', { params }),
  createCarousel: (data) => instance.post('/admin/carousels', data),
  updateCarousel: (id, data) => instance.put(`/admin/carousels/${id}`, data),
  deleteCarousel: (id) => instance.delete(`/admin/carousels/${id}`),

  // 注：买家端售后接口（applyAfterSale / getMyAfterSales / getAfterSaleDetail）
  // 属于客户端接口，已移至上方 api 对象

  // ===== 售后模块（管理后台：/admin/afterSale/*） =====
  // 售后分页列表（关联订单号、用户信息）
  getAfterSales: (params) => instance.get('/admin/afterSale/list', { params }),
  // 审核售后（status: 1同意 2拒绝；拒绝时 rejectReason 必填）
  reviewAfterSale: (data) => instance.post('/admin/afterSale/review', data),

  // ===== 管理后台仪表盘/数据大屏（批次 3 新增） =====
  // 仪表盘总览数据（真实统计：summary + 近7天趋势 + 类目/状态/热销/省份 分布）
  getDashboardOverview: () => instance.get('/admin/dashboard/overview'),
  // 全屏数据大屏（目前与 overview 返回一致，未来可加 Redis 缓存区分）
  getDashboardScreen: () => instance.get('/admin/dashboard/screen'),

  // ===== 文件上传（MinIO 对象存储，返回 http url 直接赋值表单字段） =====
  // 管理端图片上传（商品/车间共用，需 sys:upload:image 权限），file 为 FormData 中的文件字段
  uploadImage: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    // 注意：FormData 请求绝对不能手动设 Content-Type，
    // 浏览器会自动带上 boundary，手动设会丢失 boundary 导致后端解析失败。
    return instance.post('/admin/upload/image', formData, {
      headers: { 'Content-Type': undefined }
    })
  },
  // 管理端按 url 删除存储文件
  deleteUploadFile: (fileUrl) => instance.delete('/admin/upload/file', { params: { fileUrl } }),

  // ===== 商务合作申请管理 =====
  getCooperations: (params) => instance.get('/admin/cooperation/page', { params }),
  getCooperationDetail: (id) => instance.get(`/admin/cooperation/${id}`),
  replyCooperation: (id, data) => instance.put(`/admin/cooperation/${id}/reply`, data),
  deleteCooperation: (id) => instance.delete(`/admin/cooperation/${id}`),

  // ===== 人工客服请求管理 =====
  getServiceRequests: (params) => instance.get('/admin/service-request/page', { params }),
  getServiceRequestDetail: (id) => instance.get(`/admin/service-request/${id}`),
  replyServiceRequest: (id, data) => instance.post(`/admin/service-request/${id}/reply`, data),
  deleteServiceRequest: (id) => instance.delete(`/admin/service-request/${id}`),
  getServiceMessages: (id, lastMsgId) => instance.get(`/admin/service-request/${id}/messages`, { params: { lastMsgId } }),
  markServiceRead: (id) => instance.put(`/admin/service-request/${id}/read`),
  // 管理端查看客户评价
  getServiceReview: (id) => instance.get(`/admin/service-request/${id}/review`)
  // 注：前台附件上传 uploadAttachment 属于客户端接口，已移至上方 api 对象
}

// 把命名导出也挂载到 default axios 实例上，
// 这样前端页面 import instance from '../../api/index' 后直接写 instance.api.getProcessChain()，
// 避免 vite/rollup 在 import { api } from 相对路径 时偶发 Could not resolve 错误。
instance.api = api
instance.adminApi = adminApi

export default instance