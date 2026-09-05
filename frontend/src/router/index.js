import { createRouter, createWebHashHistory } from 'vue-router'
import { hasPermission, getPermissions } from '../directives/permission'

function isMobileDevice() {
  const userAgent = navigator.userAgent || navigator.vendor || window.opera
  
  // 注意：故意移除 'iPad' —— iPad 有类似 PC 的使用场景（实体键盘、多任务），
  // 不应被强制跳移动端页面，让 iPad 用户可以正常访问管理后台。
  // iPadOS 13+ 的 UA 甚至不再包含 'iPad' 字符串。
  const mobileKeywords = [
    'Android',
    'webOS',
    'iPhone',
    'iPod',
    'BlackBerry',
    'Windows Phone'
  ]
  
  const isMobileUA = mobileKeywords.some(keyword => userAgent.includes(keyword))
  const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0
  
  const result = isMobileUA || isTouchDevice
  
  console.debug('[Device Detection]', {
    userAgent: userAgent.substring(0, 100) + '...',
    isMobileUA,
    isTouchDevice,
    isMobileDevice: result
  })
  
  return result
}

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('../views/Products.vue')
  },
  {
    path: '/news',
    name: 'News',
    component: () => import('../views/News.vue')
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('../views/About.vue')
  },
  {
    path: '/contact',
    name: 'Contact',
    component: () => import('../views/Contact.vue')
  },
  {
    path: '/cooperation',
    name: 'Cooperation',
    component: () => import('../views/Cooperation.vue')
  },
  {
    path: '/cooperation/about',
    name: 'CooperationAbout',
    component: () => import('../views/CooperationAbout.vue')
  },
  {
    path: '/cooperation/technology',
    name: 'Technology',
    component: () => import('../views/Technology.vue')
  },
  {
    path: '/cooperation/download',
    name: 'DownloadPage',
    component: () => import('../views/DownloadPage.vue')
  },
  {
    path: '/process-graph',
    name: 'ProcessGraph',
    component: () => import('../views/ProcessGraph.vue')
  },
  {
    path: '/product/detail',
    name: 'ProductDetail',
    component: () => import('../views/ProductDetail.vue')
  },
  {
    path: '/my-applications',
    name: 'MyApplications',
    component: () => import('../views/MyApplications.vue')
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('../views/Cart.vue')
  },
  {
    path: '/checkout',
    name: 'Checkout',
    component: () => import('../views/Checkout.vue')
  },
  {
    path: '/my-orders',
    name: 'MyOrders',
    component: () => import('../views/MyOrders.vue')
  },
  {
    path: '/footprint',
    name: 'Footprint',
    component: () => import('../views/Footprint.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/admin/Login.vue')
  },
  // 旧入口 /admin：统一重定向到销售运营后台
  {
    path: '/admin',
    redirect: '/sale-admin/cooperation'
  },

  // ==================== 销售运营后台（/#/sale-admin） ====================
  // 面向销售运营角色：仪表盘大屏、订单、售后、客户、商品品类、商务合作、客服工单
  // 注：运营内容、车间工艺、库存/财务/供应商 等业务菜单已下架，避免点进去空白报错
  {
    path: '/sale-admin',
    name: 'SaleAdmin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '',           name: 'Dashboard',           component: () => import('../views/admin/Dashboard.vue'),           meta: { permission: 'sys:dashboard' } },
      { path: 'orders',     name: 'OrderManagement',     component: () => import('../views/admin/OrderManagement.vue'),     meta: { permission: 'sys:order:list' } },
      { path: 'aftersales', name: 'AfterSaleManage',     component: () => import('../views/admin/AfterSaleManage.vue'),     meta: { permission: 'sys:aftersale:list' } },
      { path: 'users',      name: 'UserManagement',      component: () => import('../views/admin/UserManagement.vue'),      meta: { permission: 'sys:user:list' } },
      { path: 'products',   name: 'ProductManagement',   component: () => import('../views/admin/ProductManagement.vue'),   meta: { permission: 'sys:product:list' } },
      { path: 'categories', name: 'CategoryManagement',  component: () => import('../views/admin/CategoryManagement.vue'),  meta: { permission: 'sys:category:list' } },
      { path: 'cooperation',      name: 'BusinessCoopManage',   component: () => import('../views/admin/BusinessCoopManage.vue'),   meta: { permission: 'sys:cooperation:list' } },
      { path: 'service-request',   name: 'ServiceRequestManage', component: () => import('../views/admin/ServiceRequestManage.vue'), meta: { permission: 'sys:service:list' } }
    ]
  },
  // 全屏数据大屏：独立顶层路由（不套 AdminLayout），真·100vw×100vh，不再被侧边栏/顶栏挤占导致底部被截断
  {
    path: '/sale-admin/screen',
    name: 'DataScreen',
    component: () => import('../views/admin/DataScreen.vue'),
    meta: { requiresAuth: true, title: '绿链运营数据大屏' }
  },

  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin_token')
  // 管理后台路由统一前缀：旧入口 /admin + 销售运营后台 /sale-admin
  const isAdminRoute = to.path.startsWith('/admin') || to.path.startsWith('/sale-admin')
  // 当前登录管理员角色：1=超级管理员，其他=运营角色
  const roleId = Number(localStorage.getItem('admin_role_id') || 0)

  // 调试信息：输出路由访问信息
  console.debug('[Router Guard]', {
    path: to.path,
    isAdminRoute,
    hasToken: !!token,
    roleId
  })

  // 前台页面（非管理后台路由）不做任何拦截，PC、手机自由访问
  if (!isAdminRoute) {
    next()
    return
  }

  // 管理员路由：检测真实设备类型
  const isMobile = isMobileDevice()

  console.debug('[Router Guard] Admin Route Check', {
    isMobileDevice: isMobile,
    path: to.path
  })

  // 手机/平板真实移动设备：禁止访问管理后台
  if (isMobile) {
    console.warn('[Router Guard] Blocked: Mobile device accessing admin route')
    alert('管理员后台仅支持在桌面端访问')
    next('/')
    return
  }

  // PC电脑：允许访问管理后台，继续后续校验
  console.debug('[Router Guard] Allowed: Desktop device accessing admin route')

  // 检查是否需要登录
  if (to.meta.requiresAuth && !token) {
    next('/admin/login')
    return
  }

  // 已登录用户访问登录页，统一重定向到销售运营后台
  if (to.path === '/admin/login' && token) {
    next('/sale-admin/cooperation')
    return
  }

  // 细粒度权限校验
  if (to.meta.requiresAuth && to.meta.permission) {
    // 超级管理员拥有全部权限，直接放行
    if (roleId === 1) {
      next()
      return
    }
    const permissions = getPermissions()
    // 如果没有权限数据（测试阶段或未从后端获取），默认允许访问
    if (permissions.length === 0) {
      next()
      return
    }
    // 销售运营后台：运营角色登录即可访问其业务页面
    if (to.path.startsWith('/sale-admin')) {
      next()
      return
    }
    if (!hasPermission(to.meta.permission)) {
      alert('没有访问权限')
      next(from.path || '/sale-admin')
      return
    }
  }

  next()
})

export default router