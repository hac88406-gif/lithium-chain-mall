<template>
  <section id="contact" class="py-16 bg-white">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="text-center mb-12">
        <h2 class="text-3xl font-bold text-dark mb-4">采购咨询</h2>
        <p class="text-gray-500">填写以下信息，我们将尽快与您联系</p>
      </div>

      <div class="max-w-xl mx-auto bg-gray-50 rounded-2xl p-8">
        <form @submit.prevent="handleSubmit">
          <div class="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">姓名</label>
              <input 
                v-model="form.name"
                type="text" 
                placeholder="请输入姓名"
                class="w-full px-4 py-3 rounded-lg border border-gray-300 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">电话</label>
              <input 
                v-model="form.phone"
                type="tel" 
                placeholder="请输入电话"
                class="w-full px-4 py-3 rounded-lg border border-gray-300 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all"
              />
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">采购数量</label>
              <input 
                v-model="form.quantity"
                type="number" 
                placeholder="请输入数量"
                class="w-full px-4 py-3 rounded-lg border border-gray-300 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">需求产品</label>
              <select 
                v-model="form.product"
                class="w-full px-4 py-3 rounded-lg border border-gray-300 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all"
              >
                <option value="">请选择产品</option>
                <option value="18650">18650电芯</option>
                <option value="21700">21700电芯</option>
                <option value="12v">12V储能模组</option>
                <option value="48v">48V电动车电池</option>
                <option value="power">便携储能电源</option>
                <option value="bms">BMS保护板</option>
              </select>
            </div>
          </div>

          <div class="mb-6">
            <label class="block text-sm font-medium text-gray-700 mb-2">需求备注</label>
            <textarea 
              v-model="form.remark"
              rows="3"
              placeholder="请描述您的具体需求..."
              class="w-full px-4 py-3 rounded-lg border border-gray-300 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all resize-none"
            ></textarea>
          </div>

          <button type="submit" class="btn-primary w-full">提交询价</button>
        </form>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const form = ref({
  name: '',
  phone: '',
  quantity: '',
  product: '',
  remark: ''
})

const handleSubmit = () => {
  if (!form.value.name || !form.value.phone) {
    alert('请填写姓名和电话')
    return
  }
  alert('提交成功！我们将尽快与您联系。')
  form.value = { name: '', phone: '', quantity: '', product: '', remark: '' }
}
</script>