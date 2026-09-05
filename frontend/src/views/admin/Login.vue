<template>
  <div class="min-h-screen bg-gray-100 flex items-center justify-center">
    <div class="w-full max-w-md">
      <div class="bg-white rounded-2xl shadow-xl p-8">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold text-primary mb-2">绿链锂电管理后台</h1>
          <p class="text-gray-500">管理员登录</p>
        </div>
        
        <form @submit.prevent="handleLogin" class="space-y-6">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">用户名</label>
            <input 
              v-model="form.username" 
              type="text"
              placeholder="请输入用户名"
              class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition"
              required
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
            <input 
              v-model="form.password" 
              type="password" 
              placeholder="请输入密码（测试阶段可不填）"
              class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition"
            />
          </div>
          
          <button 
            type="submit"
            :disabled="loading"
            class="w-full bg-primary text-white py-3 rounded-lg font-medium hover:bg-secondary transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </form>
        
        <p class="text-center text-gray-400 text-sm mt-6">
          默认账号：超管 admin / admin123　销售运营 operator / op123456
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { setPermissions } from '../../directives/permission'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!form.username.trim()) {
    alert('请输入用户名')
    return
  }
  
  loading.value = true
  
  try {
    // 调用后端登录接口
    const response = await fetch('/api/admin/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        username: form.username,
        password: form.password
      })
    })
    
    const data = await response.json()

    if (data.code === 200) {
      // 保存登录信息
      localStorage.setItem('admin_token', data.data.token)
      localStorage.setItem('admin_username', data.data.username)

      // 保存角色信息（roleId=1 超级管理员 / 其他=运营角色），供路由守卫与侧边栏菜单动态渲染使用
      localStorage.setItem('admin_role_id', String(data.data.roleId || 0))
      localStorage.setItem('admin_role_name', data.data.roleName || '')

      // 保存权限集合（新增字段，不破坏原有返回结构）
      if (data.data.permissions) {
        setPermissions(data.data.permissions)
      }

      // 登录后统一进入销售运营后台
      router.push('/sale-admin/cooperation')
    } else {
      alert(data.message || '登录失败')
    }
  } catch (error) {
      // 后端不可达时直接提示（已删除模拟登录回退：后端 /api/admin/** 已启用 JWT 鉴权，
      // 写死的 test_admin_token 会被 AdminAuthInterceptor 拒绝，回退只会造成 401 死循环）
      console.error('登录失败:', error)
      alert('登录失败，请确认后端服务已启动')
    } finally {
      loading.value = false
    }
  }
</script>