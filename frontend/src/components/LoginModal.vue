<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay" @click="handleOverlayClick">
      <div class="modal-container" @click.stop>
        <button class="modal-close" @click="handleClose">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
        
        <div class="modal-header">
          <button 
            class="tab-btn" 
            :class="{ active: activeTab === 'login' }"
            @click="activeTab = 'login'"
          >
            登录
          </button>
          <span class="tab-divider">｜</span>
          <button 
            class="tab-btn" 
            :class="{ active: activeTab === 'register' }"
            @click="activeTab = 'register'"
          >
            注册
          </button>
        </div>

        <div class="modal-body">
          <!-- 登录表单 -->
          <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="form-content">
            <div class="form-group">
              <label class="form-label">账号</label>
              <input 
                v-model="loginForm.username"
                type="text"
                placeholder="请输入账号"
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label class="form-label">密码</label>
              <input 
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                class="form-input"
              />
            </div>
            <div class="form-group flex items-center justify-between">
              <label class="checkbox-label">
                <input 
                  v-model="loginForm.remember"
                  type="checkbox"
                  class="checkbox-input"
                />
                <span>记住账号</span>
              </label>
            </div>
            <button type="submit" class="btn-submit" :disabled="loading">
  {{ loading ? '登录中...' : '登录' }}
</button>
            <p class="form-footer">
              没有账号？<button type="button" class="link-btn" @click="activeTab = 'register'">切换至注册页</button>
            </p>
          </form>

          <!-- 注册表单 -->
          <form v-if="activeTab === 'register'" @submit.prevent="handleRegister" class="form-content">
            <div class="grid grid-cols-2 gap-4">
              <div class="form-group">
                <label class="form-label">企业名称</label>
                <input 
                  v-model="registerForm.companyName"
                  type="text"
                  placeholder="请输入企业名称"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label class="form-label">联系人姓名</label>
                <input 
                  v-model="registerForm.contactName"
                  type="text"
                  placeholder="请输入联系人姓名"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label class="form-label">联系电话</label>
                <input 
                  v-model="registerForm.phone"
                  type="tel"
                  placeholder="请输入联系电话"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label class="form-label">邮箱</label>
                <input 
                  v-model="registerForm.email"
                  type="email"
                  placeholder="请输入邮箱"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label class="form-label">登录账号</label>
                <input 
                  v-model="registerForm.username"
                  type="text"
                  placeholder="请设置登录账号"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label class="form-label">登录密码</label>
                <input 
                  v-model="registerForm.password"
                  type="password"
                  placeholder="请设置登录密码"
                  class="form-input"
                />
              </div>
              <div class="form-group col-span-2">
                <label class="form-label">确认密码</label>
                <input 
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入密码"
                  class="form-input"
                />
              </div>
            </div>
            <button type="submit" class="btn-submit" :disabled="loading">
  {{ loading ? '注册中...' : '注册' }}
</button>
            <p class="form-footer">
              已有账号？<button type="button" class="link-btn" @click="activeTab = 'login'">切换至登录页</button>
            </p>
          </form>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { api } from '../api'
import { getGuestCart, saveGuestCart } from '../utils/guestCart'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'login'])

const activeTab = ref('login')
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const registerForm = reactive({
  companyName: '',
  contactName: '',
  phone: '',
  email: '',
  username: '',
  password: '',
  confirmPassword: ''
})

const handleOverlayClick = () => {
  emit('close')
}

const handleClose = () => {
  emit('close')
}

const handleLogin = async () => {
  if (!loginForm.username) {
    alert('请输入账号')
    return
  }
  if (!loginForm.password) {
    alert('请输入密码')
    return
  }
  
  loading.value = true
  
  try {
    const response = await api.login({
      username: loginForm.username,
      password: loginForm.password
    })
    
    if (response.code === 200) {
      const userData = response.data.user || response.data
      const userInfo = {
        id: userData.id,
        username: userData.username,
        nickname: userData.nickname || userData.username,
        companyName: userData.companyName,
        phone: userData.phone,
        email: userData.email
      }
      
      localStorage.setItem('token', response.data.token)
      localStorage.setItem('userInfo', JSON.stringify(userInfo))

      if (loginForm.remember) {
        localStorage.setItem('rememberedUsername', loginForm.username)
      } else {
        localStorage.removeItem('rememberedUsername')
      }

      // ===== 游客购物车合并（在 emit('login') 之前执行，不阻断登录流程） =====
      try {
        const guestCart = getGuestCart()
        if (guestCart.length > 0) {
          // 携带 token（axios 拦截器自动附加）调用合并接口
          const mergeRes = await api.mergeGuestCart(guestCart)
          if (mergeRes.code === 200) {
            // 合并成功，清空本地游客购物车
            saveGuestCart([])
          } else {
            console.warn('本地购物车合并失败:', mergeRes.message)
            alert('本地购物车合并失败，请手动重新加入')
          }
        }
      } catch (e) {
        // 合并失败不阻断登录流程
        console.warn('本地购物车合并失败:', e)
        alert('本地购物车合并失败，请手动重新加入')
      }

      alert('登录成功')
      emit('login', userInfo)
      emit('close')
      // 通知其他组件登录成功（FloatingChat 等需要恢复历史消息）
      try { window.dispatchEvent(new CustomEvent('gc-login-success')) } catch (e) { /* ignore */ }
    } else {
      alert(response.message || '登录失败')
    }
  } catch (error) {
    console.error('登录失败:', error)
    alert('登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
  
  loginForm.username = ''
  loginForm.password = ''
  loginForm.remember = false
}

const handleRegister = async () => {
  if (!registerForm.companyName) {
    alert('请输入企业名称')
    return
  }
  if (!registerForm.contactName) {
    alert('请输入联系人姓名')
    return
  }
  if (!registerForm.phone) {
    alert('请输入联系电话')
    return
  }
  if (!registerForm.email) {
    alert('请输入邮箱')
    return
  }
  if (!registerForm.username) {
    alert('请设置登录账号')
    return
  }
  if (!registerForm.password) {
    alert('请设置登录密码')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }
  
  loading.value = true
  
  try {
    const response = await api.register({
      companyName: registerForm.companyName,
      contactName: registerForm.contactName,
      phone: registerForm.phone,
      email: registerForm.email,
      username: registerForm.username,
      password: registerForm.password
    })
    
    if (response.code === 200) {
      alert('注册成功，请登录')
      activeTab.value = 'login'
    } else {
      alert(response.message || '注册失败')
    }
  } catch (error) {
    console.error('注册失败:', error)
    alert('注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
  
  registerForm.companyName = ''
  registerForm.contactName = ''
  registerForm.phone = ''
  registerForm.email = ''
  registerForm.username = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
}

onMounted(() => {
  const rememberedUsername = localStorage.getItem('rememberedUsername')
  if (rememberedUsername) {
    loginForm.username = rememberedUsername
    loginForm.remember = true
  }
})
</script>

<style scoped>
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
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-container {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 480px;
  position: relative;
  animation: scaleIn 0.3s ease;
}

@keyframes scaleIn {
  from { transform: scale(0.9); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.modal-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: #f3f4f6;
  color: #6b7280;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: #e5e7eb;
  color: #374151;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 24px 0;
  gap: 16px;
}

.tab-btn {
  padding: 8px 24px;
  background: transparent;
  border: none;
  font-size: 18px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
}

.tab-btn.active {
  color: #0d9488;
  background: #f0fdf4;
}

.tab-divider {
  color: #e5e7eb;
}

.modal-body {
  padding: 24px;
}

.form-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
}

.form-input {
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s ease;
}

.form-input:focus {
  outline: none;
  border-color: #0d9488;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #6b7280;
}

.checkbox-input {
  accent-color: #0d9488;
}

.btn-submit {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #0d9488 0%, #0891b2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 150, 136, 0.3);
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: #6b7280;
  margin-top: 8px;
}

.link-btn {
  color: #0d9488;
  text-decoration: none;
  background: transparent;
  border: none;
  cursor: pointer;
  font-weight: 500;
}

.link-btn:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .modal-container {
    width: 95%;
    margin: 16px;
  }
  
  .grid {
    grid-template-columns: 1fr;
  }
  
  .col-span-2 {
    grid-column: span 1;
  }
}
</style>