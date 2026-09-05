<template>
  <div class="py-16">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="text-center mb-12">
        <h2 class="text-3xl font-bold text-dark mb-4">联系我们</h2>
        <p class="text-gray-500">如有疑问或合作意向，请随时联系我们</p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-12">
        <div class="contact-info">
          <div class="info-item">
            <div class="info-icon">
              <svg class="w-6 h-6 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
              </svg>
            </div>
            <div>
              <h4>公司地址</h4>
              <p>深圳市南山区科技园XX大厦</p>
            </div>
          </div>

          <div class="info-item">
            <div class="info-icon">
              <svg class="w-6 h-6 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/>
              </svg>
            </div>
            <div>
              <h4>联系电话</h4>
              <p>400-888-8888</p>
            </div>
          </div>

          <div class="info-item">
            <div class="info-icon">
              <svg class="w-6 h-6 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
              </svg>
            </div>
            <div>
              <h4>电子邮箱</h4>
              <p>contact@lithium.com</p>
            </div>
          </div>

          <div class="info-item">
            <div class="info-icon">
              <svg class="w-6 h-6 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            </div>
            <div>
              <h4>工作时间</h4>
              <p>周一至周五 8:30-17:30</p>
            </div>
          </div>
        </div>

        <div class="contact-form">
          <h3 class="text-xl font-semibold text-dark mb-6">发送消息</h3>
          <form @submit.prevent="submitForm">
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">姓名</label>
              <input 
                v-model="form.name"
                type="text"
                placeholder="请输入姓名"
                class="form-input"
              />
            </div>
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">电话</label>
              <input 
                v-model="form.phone"
                type="tel"
                placeholder="请输入电话"
                class="form-input"
              />
            </div>
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">邮箱</label>
              <input 
                v-model="form.email"
                type="email"
                placeholder="请输入邮箱"
                class="form-input"
              />
            </div>
            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 mb-2">留言内容</label>
              <textarea 
                v-model="form.message"
                rows="4"
                placeholder="请输入留言内容..."
                class="form-textarea"
              ></textarea>
            </div>
            <button type="submit" class="btn-primary w-full">发送消息</button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { api } from '../api/index'

const form = ref({
  name: '',
  phone: '',
  email: '',
  message: ''
})

const submitForm = async () => {
  if (!form.value.name || !form.value.phone) {
    alert('请填写姓名和电话')
    return
  }
  if (!form.value.message || !form.value.message.trim()) {
    alert('请填写留言内容')
    return
  }
  try {
    const res = await api.submitServiceRequest({
      source: 'contact',
      question: 'other',
      name: form.value.name.trim(),
      phone: form.value.phone.trim(),
      email: form.value.email.trim(),
      content: form.value.message.trim()
    })
    if (res?.code === 200) {
      alert('留言已发送，我们将尽快与您联系！')
      form.value = { name: '', phone: '', email: '', message: '' }
    } else {
      alert(res?.message || '发送失败')
    }
  } catch (e) {
    alert('网络异常，请稍后重试')
  }
}
</script>

<style scoped>
.contact-info {
  background: white;
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.info-item {
  display: flex;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #eee;
}

.info-item:last-child {
  border-bottom: none;
}

.info-icon {
  width: 48px;
  height: 48px;
  background: #f0fdf4;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info-item h4 {
  font-weight: 600;
  color: #2C3E50;
  margin-bottom: 4px;
}

.info-item p {
  color: #666;
}

.contact-form {
  background: white;
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s ease;
}

.form-input:focus {
  outline: none;
  border-color: #0d9488;
}

.form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  resize: none;
  transition: border-color 0.3s ease;
}

.form-textarea:focus {
  outline: none;
  border-color: #0d9488;
}
</style>