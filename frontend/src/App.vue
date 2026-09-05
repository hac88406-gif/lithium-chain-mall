<template>
  <div class="min-h-screen bg-gray-50" v-if="!isAdminRoute">
    <ScrollProgress />
    <Header />
    <router-view />
    <Footer />
    
    <!-- Coze 智能客服悬浮窗（右下角，与 float-toolbar 协调位置，mobile 端自动适配） -->
    <FloatingChat />

    <div class="float-toolbar">
      <div 
        class="toolbar-btn" 
        :class="{ hidden: !showBackTop }"
        @click="scrollToTop"
      >
        <svg class="toolbar-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M5 10l7-7m0 0l7 7m-7-7v18"/>
        </svg>
        <span class="toolbar-tooltip">返回顶部</span>
      </div>
      
      <div class="toolbar-btn" @click="showPhoneModal = true">
        <svg class="toolbar-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/>
        </svg>
        <span class="toolbar-tooltip">联系电话</span>
      </div>
    </div>

    <div 
      v-if="showPhoneModal" 
      class="modal-overlay" 
      @click.self="showPhoneModal = false"
      @keydown.escape="showPhoneModal = false"
    >
      <div class="modal-content phone-modal">
        <div class="modal-header">
          <h3>联系电话</h3>
          <button class="modal-close" @click="showPhoneModal = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="phone-content">
          <a href="tel:400-888-8888" class="phone-link">400-888-8888</a>
          <p class="phone-tip">点击号码直接拨打</p>
        </div>
      </div>
    </div>
  </div>
  
  <router-view v-else />
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import ScrollProgress from './components/ScrollProgress.vue'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
import FloatingChat from './components/FloatingChat.vue'

const route = useRoute()
const showBackTop = ref(false)
const showPhoneModal = ref(false)

const isAdminRoute = computed(() => {
  // 匹配后台路由前缀：/admin（登录/旧入口）、/sale-admin（销售运营后台）
  return /^\/(admin|sale-admin)(\/|$)/.test(route.path)
})

const handleScroll = () => {
  showBackTop.value = window.scrollY > 350
}

const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style>
.float-toolbar {
  position: fixed;
  right: 24px;
  bottom: 32px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  /* gap 82px = 16px padding + 50px AI 按钮占位 + 16px padding，让 AI 精确卡中间 */
  gap: 82px;
}

.toolbar-btn {
  width: 50px;
  height: 50px;
  background: #059669;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
}

.toolbar-btn.hidden {
  opacity: 0;
  transform: translateY(20px);
  visibility: hidden;
}

.toolbar-btn:hover {
  transform: translateY(-4px) scale(1.05);
  box-shadow: 0 8px 20px rgba(5, 150, 105, 0.4);
}

.toolbar-icon {
  width: 24px;
  height: 24px;
  color: #ffffff;
}

.toolbar-tooltip {
  position: absolute;
  right: calc(100% + 12px);
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all 0.2s ease;
  pointer-events: none;
}

.toolbar-btn:hover .toolbar-tooltip {
  opacity: 1;
  visibility: visible;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: #ffffff;
  border-radius: 16px;
  width: 90%;
  max-width: 450px;
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { 
    opacity: 0; 
    transform: translateY(20px); 
  }
  to { 
    opacity: 1; 
    transform: translateY(0); 
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #1f2937;
}

.modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background: #f3f4f6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: #e5e7eb;
  color: #374151;
}

.modal-close svg {
  width: 18px;
  height: 18px;
}

.wechat-content {
  padding: 24px;
  text-align: center;
}

.qrcode-container {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.qrcode {
  width: 200px;
  height: 200px;
  border-radius: 12px;
}

.wechat-tip {
  color: #6b7280;
  font-size: 0.9rem;
}

.phone-content {
  padding: 24px;
  text-align: center;
}

.phone-link {
  display: block;
  font-size: 2rem;
  font-weight: 700;
  color: #059669;
  text-decoration: none;
  margin-bottom: 12px;
}

.phone-link:hover {
  text-decoration: underline;
}

.phone-tip {
  color: #6b7280;
  font-size: 0.9rem;
}

@media (max-width: 640px) {
  .float-toolbar {
    right: 16px;
    bottom: 24px;
    /* gap 78px = 16px padding + 46px AI 按钮占位 + 16px padding */
    gap: 78px;
  }
  
  .toolbar-btn {
    width: 46px;
    height: 46px;
  }
  
  .toolbar-icon {
    width: 22px;
    height: 22px;
  }
  
  .modal-content {
    width: 92%;
    max-width: 360px;
  }
}
</style>