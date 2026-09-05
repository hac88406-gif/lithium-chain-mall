<template>
  <div class="admin-layout flex h-screen bg-[#f5f7fa]">
    <!-- 左侧侧边栏：单一滚动承载层（flex-1 + overflow-y-auto），避免被挤压截断 -->
    <aside class="sidebar w-[232px] flex-shrink-0 text-white flex flex-col" :class="[themeClass]">
      <!-- LOGO 区（按当前后台身份切换标题） -->
      <div class="logo h-16 flex items-center px-5 border-b border-white/10 gap-2.5 flex-shrink-0">
        <div class="w-8 h-8 rounded-lg bg-white/15 flex items-center justify-center text-base font-bold">
          🌿
        </div>
        <div class="flex flex-col leading-tight">
          <span class="text-[15px] font-bold tracking-wide">绿链销售运营后台</span>
          <span class="text-[11px] opacity-60 mt-0.5">Sales Operation Console</span>
        </div>
      </div>

      <!-- 可滚动菜单区 -->
      <nav class="flex-1 overflow-y-auto py-3 px-2 sidebar-scroll">
        <div v-for="group in menuGroups" :key="group.title" class="mb-5">
          <div class="group-title">
            {{ group.title }}
          </div>
          <ul class="menu-list">
            <li
              v-for="menu in group.items"
              :key="menu.path"
              :class="{ active: isActive(menu), 'has-children': menu.children && menu.children.length }"
              @click="handleMenuClick(menu)"
            >
              <div class="menu-item">
                <span class="menu-left">
                  <el-icon class="menu-icon"><component :is="menu.icon" /></el-icon>
                  <span class="menu-name">{{ menu.name }}</span>
                </span>
                <el-icon v-if="menu.children && menu.children.length" class="expand-icon">
                  <ArrowDown v-if="expandedMenus.includes(menu.path)" />
                  <ArrowRight v-else />
                </el-icon>
              </div>

              <ul
                v-if="menu.children && menu.children.length && expandedMenus.includes(menu.path)"
                class="sub-menu"
              >
                <li
                  v-for="child in menu.children"
                  :key="child.path"
                  :class="{ active: currentPath === child.path }"
                  @click.stop="navigate(child.path)"
                >
                  <span>{{ child.name }}</span>
                </li>
              </ul>
            </li>
          </ul>
        </div>
      </nav>

      <!-- 底部版本号 -->
      <div class="sidebar-footer flex-shrink-0">
        <div class="text-[11px] opacity-50 leading-5">
          v1.0 · 锂电采购运营平台
        </div>
      </div>
    </aside>

    <!-- 右侧主区：100vh 外层 overflow-hidden，子 content-area 作为唯一滚动层 -->
    <main class="main-content flex-1 flex flex-col overflow-hidden min-w-0">
      <!-- 顶部栏 -->
      <header class="top-bar h-16 flex-shrink-0 bg-white border-b border-gray-200 flex items-center justify-between px-6">
        <div class="flex items-center gap-3 text-gray-700 font-medium">
          <el-icon class="text-gray-400"><Location /></el-icon>
          <span>{{ currentPageName }}</span>
        </div>

        <div class="flex items-center gap-5">
          <!-- 快捷入口：全屏数据大屏 -->
          <router-link
            to="/sale-admin/screen"
            target="_blank"
            class="shortcut-btn"
            title="新窗口打开全屏数据大屏"
          >
            <el-icon><Monitor /></el-icon>
            <span>数据大屏</span>
          </router-link>

          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="30" class="admin-avatar">
                {{ (adminUsername || 'A').charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="text-gray-600 ml-2">{{ adminUsername }}</span>
              <el-icon class="text-gray-400 text-xs"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon class="mr-1 text-red-500"><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区：单一滚动承载层。DataScreen 会通过 keep-alive + exclude 保持自绘 -->
      <div class="content-area flex-1 overflow-y-auto overflow-x-hidden" :class="contentClass">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <keep-alive :exclude="['DataScreen']">
              <component :is="Component" />
            </keep-alive>
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DataAnalysis, Grid, Goods, ShoppingCart, RefreshLeft,
  UserFilled, Monitor,
  ArrowDown, ArrowRight, Location, SwitchButton,
  Opportunity, ChatDotRound
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const adminUsername = ref('管理员')
const currentPath = ref('/sale-admin')
const expandedMenus = ref([])

// ==================== 销售运营后台菜单 ====================
const saleMenuGroups = [
  {
    title: '仪表盘大屏',
    items: [
      { path: '/sale-admin',           name: '仪表盘首页',   icon: DataAnalysis, permission: 'sys:dashboard' },
      { path: '/sale-admin/screen',    name: '全屏数据大屏', icon: Monitor,      permission: 'sys:dashboard:screen' },
    ]
  },
  {
    title: '订单与售后',
    items: [
      { path: '/sale-admin/orders',    name: '订单管理',     icon: ShoppingCart, permission: 'sys:order:list' },
      { path: '/sale-admin/aftersales', name: '售后管理',    icon: RefreshLeft,  permission: 'sys:aftersale:list' },
    ]
  },
  {
    title: '客户管理',
    items: [
      { path: '/sale-admin/users',            name: '客户档案',         icon: UserFilled,       permission: 'sys:user:list' },
      { path: '/sale-admin/cooperation',       name: '商务合作申请',     icon: Opportunity,      permission: 'sys:cooperation:list' },
      { path: '/sale-admin/service-request',   name: '人工客服请求',     icon: ChatDotRound,     permission: 'sys:service:list' },
    ]
  },
  {
    title: '商品与品类',
    items: [
      { path: '/sale-admin/products',  name: '商品管理',     icon: Goods,        permission: 'sys:product:list' },
      { path: '/sale-admin/categories', name: '品类管理',    icon: Grid,         permission: 'sys:category:list' },
    ]
  }
]

// 统一使用销售运营后台菜单
const menuGroups = computed(() => saleMenuGroups)

// 侧边栏配色（和管理后台登录页统一翡翠绿，避免现在深蓝配后台表格配色打架）
const themeClass = 'sidebar-theme-jade'

// 内容区 padding：数据大屏不能有任何 padding，否则破坏 1:1 缩放
const contentClass = computed(() => {
  return currentPath.value === '/sale-admin/screen' ? 'content-screen' : 'content-default'
})

const isActive = (menu) => {
  if (menu.children && menu.children.length) {
    return menu.children.some(c => currentPath.value === c.path)
  }
  // 顶级路径精确匹配（如 /sale-admin 及其子页）
  return currentPath.value === menu.path
}

// 页面标题映射：销售运营后台路径
const pageNames = {
  // 销售运营后台
  '/sale-admin':            '仪表盘首页',
  '/sale-admin/screen':     '全屏数据大屏',
  '/sale-admin/orders':     '订单管理',
  '/sale-admin/aftersales': '售后管理',
  '/sale-admin/users':      '客户档案',
  '/sale-admin/cooperation': '商务合作申请',
  '/sale-admin/service-request': '人工客服请求',
  '/sale-admin/products':   '商品管理',
  '/sale-admin/categories': '品类管理',
}

const currentPageName = computed(() => pageNames[currentPath.value] || '管理后台')

const handleMenuClick = (menu) => {
  if (menu.children && menu.children.length) {
    const i = expandedMenus.value.indexOf(menu.path)
    i > -1 ? expandedMenus.value.splice(i, 1) : expandedMenus.value.push(menu.path)
  } else {
    navigate(menu.path)
  }
}

const navigate = (path) => {
  currentPath.value = path
  router.push(path)
  nextTick(() => {
    // 切页后回到内容区顶部，避免上一页滚动位置残留导致"下面内容被截断"
    const area = document.querySelector('.admin-layout .content-area')
    area && (area.scrollTop = 0)
  })
}

const handleLogout = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_username')
  localStorage.removeItem('admin_permissions')
  // 同步清理角色信息，避免下次登录前残留旧角色导致路由守卫误判
  localStorage.removeItem('admin_role_id')
  localStorage.removeItem('admin_role_name')
  router.push('/admin/login')
}

const handleCommand = (cmd) => {
  if (cmd === 'logout') handleLogout()
}

onMounted(() => {
  adminUsername.value = localStorage.getItem('admin_username') || '管理员'
  currentPath.value = route.path
})

watch(() => route.path, (p) => { currentPath.value = p }, { immediate: false })
</script>

<style scoped>
.admin-layout {
  font-family: 'PingFang SC', 'Microsoft YaHei', -apple-system, sans-serif;
  background: #F8FAFC;  /* A档 slate-50 浅底 */
}

/* ===== 侧边栏主题：A 档干净 B 端白底 + 翡翠绿点缀（不是旧的深色渐变） ===== */
.sidebar-theme-jade {
  background: #ffffff;
  border-right: 1px solid #E2E8F0;
  box-shadow: 2px 0 12px rgba(13,148,136,.04);
}
.sidebar-theme-jade .group-title {
  color: #94A3B8;
}
.sidebar-theme-jade .menu-item {
  color: #334155;
  border-radius: 14px;
}
.sidebar-theme-jade .menu-item:hover {
  background: #F1F5F9;
  color: #0d9488;
}
.sidebar-theme-jade .menu-list li.active > .menu-item {
  background: linear-gradient(135deg, rgba(13,148,136,.10), rgba(6,182,212,.08));
  color: #0d9488;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(13,148,136,.10);
}

/* ===== 通用布局 ===== */
.sidebar { min-width: 240px; }
.group-title {
  font-size: 11px;
  letter-spacing: 1.5px;
  color: #94A3B8;
  padding: 18px 14px 6px;
  font-weight: 700;
  text-transform: uppercase;
}
.menu-list { list-style: none; padding: 0 8px; margin: 0; }
.menu-list li {
  margin-bottom: 3px;
  border-radius: 14px;
  overflow: hidden;
}
.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 14px;
  cursor: pointer;
  transition: all .2s ease;
  color: #334155;
  font-size: 14px;
}
.menu-left { display: flex; align-items: center; gap: 12px; }
.menu-icon { font-size: 17px; width: 20px; height: 20px; flex-shrink: 0; color: #64748B; }
li.active > .menu-item .menu-icon { color: #0d9488; }
.menu-name  { line-height: 1; }
.expand-icon { font-size: 12px; opacity: 0.65; }

.sub-menu {
  list-style: none;
  padding: 4px 0 6px 0;
  margin: 4px 0;
  background: #F8FAFC;
  border-radius: 12px;
  border: 1px solid #F1F5F9;
}
.sub-menu li {
  padding: 10px 14px 10px 48px;
  cursor: pointer;
  transition: all .2s ease;
  color: #475569;
  font-size: 13px;
  border-radius: 10px;
}
.sub-menu li:hover  { background: #ffffff; color: #0d9488; }
.sub-menu li.active {
  background: #ffffff;
  color: #0d9488;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(13,148,136,.08);
}

.sidebar-scroll::-webkit-scrollbar { width: 6px; }
.sidebar-scroll::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 4px; }
.sidebar-scroll::-webkit-scrollbar-thumb:hover { background: #CBD5E1; }

.sidebar-footer {
  padding: 14px 18px 18px;
  border-top: 1px solid #F1F5F9;
  color: #64748B;
  text-align: left;
}

/* ===== 顶部栏：A 档白底 + 软阴影 ===== */
.top-bar {
  background: #ffffff;
  border-bottom: 1px solid #F1F5F9;
  box-shadow: 0 2px 12px rgba(13,148,136,.04);
}
.admin-avatar {
  background: linear-gradient(135deg,#0d9488,#06b6d4) !important;
  color: #fff;
  font-weight: 700;
  box-shadow: 0 4px 10px rgba(13,148,136,.22);
}
.user-info {
  display: flex; align-items: center; cursor: pointer;
  padding: 5px 10px;
  border-radius: 14px;
  transition: all .2s;
}
.user-info:hover {
  background: #F8FAFC;
  box-shadow: 0 4px 10px rgba(15,23,42,.04);
}

/* 快捷入口：数据大屏按钮 — A 档统一 14px 圆角 + 软阴影 */
.shortcut-btn {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 8px 18px;
  border-radius: 14px;
  background: #ffffff;
  color: #0d9488;
  border: 1px solid #CCFBF1;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: all .2s ease;
  box-shadow: 0 4px 12px rgba(13,148,136,.06);
}
.shortcut-btn:hover {
  background: linear-gradient(135deg, #0d9488, #06b6d4);
  color: #ffffff !important;
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(13,148,136,.22);
  border-color: transparent;
}

/* ===== 内容区：统一滚动 ===== */
.content-default { padding: 24px 28px 36px; }
.content-screen  { padding: 0; background: #F1F5F9; }

/* 页面切换淡入淡出，避免"闪截断" */
.fade-enter-active, .fade-leave-active { transition: opacity .2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
