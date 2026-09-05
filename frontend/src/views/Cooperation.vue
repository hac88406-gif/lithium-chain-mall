<template>
  <div>
    <section class="cooperation-banner">
      <div class="banner-content">
        <h1>大型锂电项目战略合作</h1>
        <p>大容量储能电池定制｜锂电池全自动产线搭建方案</p>
      </div>
    </section>

    <section class="solutions-section py-16 bg-gray-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <h2 class="text-3xl font-bold text-dark mb-4">业务方案</h2>
          <p class="text-gray-500">专业锂电解决方案，助力企业发展</p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div class="solution-card">
            <div class="card-icon">
              <svg class="w-12 h-12 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"/>
              </svg>
            </div>
            <h3 class="text-xl font-semibold text-dark mb-4">大容量储能电池定制</h3>
            <p class="text-gray-600 mb-6">提供大容量储能电池定制服务，涵盖电芯选型、PACK组装、BMS系统集成，满足各类储能场景需求。</p>
          </div>

          <div class="solution-card">
            <div class="card-icon">
              <svg class="w-12 h-12 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
              </svg>
            </div>
            <h3 class="text-xl font-semibold text-dark mb-4">锂电池整套自动化PACK产线方案</h3>
            <p class="text-gray-600 mb-6">从电芯来料检测到成品包装出货，提供完整的锂电池PACK自动化生产线解决方案。</p>
          </div>
        </div>
      </div>
    </section>

    <section ref="formSection" class="contact-section py-16">
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <h2 class="text-3xl font-bold text-dark mb-4">商务合作</h2>
          <p class="text-gray-500">选择您的合作类型，填写信息后我们将尽快与您联系</p>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <!-- 大客户洽谈 -->
          <div class="contact-form-wrapper">
            <div class="text-center mb-8">
              <h3 class="text-2xl font-bold text-dark mb-2">大客户洽谈</h3>
              <p class="text-gray-500 text-sm">企业级锂电解决方案合作</p>
            </div>

            <form @submit.prevent="submitForm" class="contact-form">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">企业名称 <span class="text-red-500">*</span></label>
                  <input 
                    v-model="formData.companyName"
                    type="text"
                    placeholder="请输入企业名称"
                    class="form-input"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">企业地址</label>
                  <input 
                    v-model="formData.companyAddress"
                    type="text"
                    placeholder="请输入企业地址"
                    class="form-input"
                  />
                </div>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">对接人 <span class="text-red-500">*</span></label>
                  <input 
                    v-model="formData.contactPerson"
                    type="text"
                    placeholder="请输入对接人姓名"
                    class="form-input"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">职务</label>
                  <input 
                    v-model="formData.position"
                    type="text"
                    placeholder="请输入职务"
                    class="form-input"
                  />
                </div>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">联系电话 <span class="text-red-500">*</span></label>
                  <input 
                    v-model="formData.phone"
                    type="tel"
                    placeholder="请输入联系电话"
                    class="form-input"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">企业邮箱</label>
                  <input 
                    v-model="formData.email"
                    type="email"
                    placeholder="请输入企业邮箱"
                    class="form-input"
                  />
                </div>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">需求类型</label>
                  <select v-model="formData.requirementType" class="form-input">
                    <option value="battery">储能电池</option>
                    <option value="production">整线方案</option>
                  </select>
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">预算范围</label>
                  <select v-model="formData.budget" class="form-input">
                    <option value="">请选择预算范围</option>
                    <option value="100-500">100-500万</option>
                    <option value="500-1000">500-1000万</option>
                    <option value="1000-5000">1000-5000万</option>
                    <option value="5000+">5000万以上</option>
                  </select>
                </div>
              </div>

              <div class="mb-6 mt-6">
                <label class="block text-sm font-medium text-gray-700 mb-2">合作意向</label>
                <div class="flex flex-wrap gap-4">
                  <label class="checkbox-label">
                    <input 
                      type="checkbox"
                      :checked="formData.intention.includes('investment')"
                      @change="updateIntention('investment')"
                      class="checkbox-input"
                    />
                    <span>投资合作</span>
                  </label>
                  <label class="checkbox-label">
                    <input 
                      type="checkbox"
                      :checked="formData.intention.includes('technology')"
                      @change="updateIntention('technology')"
                      class="checkbox-input"
                    />
                    <span>技术合作</span>
                  </label>
                  <label class="checkbox-label">
                    <input 
                      type="checkbox"
                      :checked="formData.intention.includes('production')"
                      @change="updateIntention('production')"
                      class="checkbox-input"
                    />
                    <span>产能合作</span>
                  </label>
                  <label class="checkbox-label">
                    <input 
                      type="checkbox"
                      :checked="formData.intention.includes('distribution')"
                      @change="updateIntention('distribution')"
                      class="checkbox-input"
                    />
                    <span>渠道分销</span>
                  </label>
                </div>
              </div>

              <div class="mb-6 mt-6">
                <label class="block text-sm font-medium text-gray-700 mb-2">期望交付周期</label>
                <select v-model="formData.deliveryCycle" class="form-input">
                  <option value="">请选择期望交付周期</option>
                  <option value="3month">3个月内</option>
                  <option value="3-6month">3–6个月</option>
                  <option value="6-12month">6–12个月</option>
                  <option value="12month+">12个月以上</option>
                </select>
              </div>

              <div class="mb-6 mt-6">
                <label class="block text-sm font-medium text-gray-700 mb-2">项目需求备注 <span class="text-red-500">*</span></label>
                <textarea 
                  v-model="formData.remark"
                  rows="4"
                  placeholder="请描述您的具体需求，如产品规格、数量、交付周期等..."
                  class="form-textarea"
                ></textarea>
              </div>

              <button type="submit" class="btn-primary w-full">提交洽谈申请</button>
            </form>
          </div>

          <!-- 媒体采访 -->
          <div class="contact-form-wrapper">
            <div class="text-center mb-8">
              <h3 class="text-2xl font-bold text-dark mb-2">媒体采访</h3>
              <p class="text-gray-500 text-sm">新闻媒体采访与合作咨询</p>
            </div>

            <form @submit.prevent="submitMediaForm" class="contact-form">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">媒体名称 <span class="text-red-500">*</span></label>
                  <input 
                    v-model="mediaData.mediaName"
                    type="text"
                    placeholder="请输入媒体名称"
                    class="form-input"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">媒体地址 <span class="text-red-500">*</span></label>
                  <input 
                    v-model="mediaData.mediaAddress"
                    type="text"
                    placeholder="请输入媒体地址"
                    class="form-input"
                  />
                </div>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">姓名 <span class="text-red-500">*</span></label>
                  <input 
                    v-model="mediaData.name"
                    type="text"
                    placeholder="请输入姓名"
                    class="form-input"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">职务</label>
                  <input 
                    v-model="mediaData.position"
                    type="text"
                    placeholder="请输入职务"
                    class="form-input"
                  />
                </div>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">工作邮箱 <span class="text-red-500">*</span></label>
                  <input 
                    v-model="mediaData.email"
                    type="email"
                    placeholder="请输入工作邮箱"
                    class="form-input"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">电话 <span class="text-red-500">*</span></label>
                  <input 
                    v-model="mediaData.phone"
                    type="tel"
                    placeholder="请输入联系电话"
                    class="form-input"
                  />
                </div>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">采访主题</label>
                  <input 
                    v-model="mediaData.topic"
                    type="text"
                    placeholder="请输入采访主题"
                    class="form-input"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">预计日期</label>
                  <input 
                    v-model="mediaData.date"
                    type="date"
                    class="form-input"
                  />
                </div>
              </div>

              <div class="mb-6 mt-6">
                <label class="block text-sm font-medium text-gray-700 mb-2">采访形式</label>
                <div class="flex flex-wrap gap-4">
                  <label class="radio-label">
                    <input 
                      type="radio"
                      v-model="mediaData.format"
                      value="onsite"
                      class="radio-input"
                    />
                    <span>现场采访</span>
                  </label>
                  <label class="radio-label">
                    <input 
                      type="radio"
                      v-model="mediaData.format"
                      value="phone"
                      class="radio-input"
                    />
                    <span>电话采访</span>
                  </label>
                  <label class="radio-label">
                    <input 
                      type="radio"
                      v-model="mediaData.format"
                      value="video"
                      class="radio-input"
                    />
                    <span>视频采访</span>
                  </label>
                  <label class="radio-label">
                    <input 
                      type="radio"
                      v-model="mediaData.format"
                      value="written"
                      class="radio-input"
                    />
                    <span>书面采访</span>
                  </label>
                </div>
              </div>

              <div class="mb-6 mt-6">
                <label class="block text-sm font-medium text-gray-700 mb-2">来函目的</label>
                <div class="flex flex-wrap gap-4">
                  <label class="checkbox-label">
                    <input 
                      type="checkbox"
                      :checked="mediaData.purpose.includes('advertisement')"
                      @change="updatePurpose('advertisement')"
                      class="checkbox-input"
                    />
                    <span>广告投放</span>
                  </label>
                  <label class="checkbox-label">
                    <input 
                      type="checkbox"
                      :checked="mediaData.purpose.includes('interview')"
                      @change="updatePurpose('interview')"
                      class="checkbox-input"
                    />
                    <span>采访</span>
                  </label>
                  <label class="checkbox-label">
                    <input 
                      type="checkbox"
                      :checked="mediaData.purpose.includes('visit')"
                      @change="updatePurpose('visit')"
                      class="checkbox-input"
                    />
                    <span>参观</span>
                  </label>
                  <label class="checkbox-label">
                    <input 
                      type="checkbox"
                      :checked="mediaData.purpose.includes('activity')"
                      @change="updatePurpose('activity')"
                      class="checkbox-input"
                    />
                    <span>活动</span>
                  </label>
                  <label class="checkbox-label">
                    <input 
                      type="checkbox"
                      :checked="mediaData.purpose.includes('other')"
                      @change="updatePurpose('other')"
                      class="checkbox-input"
                    />
                    <span>其他</span>
                  </label>
                </div>
              </div>

              <div class="mb-6 mt-6">
                <label class="block text-sm font-medium text-gray-700 mb-2">详细需求描述 <span class="text-red-500">*</span></label>
                <textarea 
                  v-model="mediaData.description"
                  rows="4"
                  placeholder="请描述您的具体需求..."
                  class="form-textarea"
                ></textarea>
              </div>

              <button type="submit" class="btn-primary w-full">提交采访申请</button>
            </form>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { api } from '../api/index'

const formSection = ref(null)

const formData = reactive({
  companyName: '',
  companyAddress: '',
  contactPerson: '',
  position: '',
  phone: '',
  email: '',
  requirementType: 'battery',
  budget: '',
  intention: [],
  deliveryCycle: '',
  remark: ''
})

const mediaData = reactive({
  mediaName: '',
  mediaAddress: '',
  name: '',
  position: '',
  email: '',
  phone: '',
  topic: '',
  date: '',
  format: '',
  purpose: [],
  description: ''
})

const updateIntention = (value) => {
  const index = formData.intention.indexOf(value)
  if (index > -1) {
    formData.intention.splice(index, 1)
  } else {
    formData.intention.push(value)
  }
}

const updatePurpose = (value) => {
  const index = mediaData.purpose.indexOf(value)
  if (index > -1) {
    mediaData.purpose.splice(index, 1)
  } else {
    mediaData.purpose.push(value)
  }
}

const scrollToForm = () => {
  formSection.value?.scrollIntoView({ behavior: 'smooth' })
}

const submitForm = async () => {
  if (!formData.companyName.trim()) { alert('请填写企业名称'); return }
  if (!formData.contactPerson.trim()) { alert('请填写对接人'); return }
  if (!formData.phone.trim()) { alert('请填写联系电话'); return }
  if (!formData.remark.trim()) { alert('请填写项目需求备注'); return }

  try {
    const res = await api.submitCooperation({
      type: 'cooperation',
      companyName: formData.companyName.trim(),
      companyAddress: formData.companyAddress.trim(),
      contactPerson: formData.contactPerson.trim(),
      position: formData.position.trim(),
      phone: formData.phone.trim(),
      email: formData.email.trim(),
      requirementType: formData.requirementType,
      budget: formData.budget,
      intention: formData.intention.join(','),
      deliveryCycle: formData.deliveryCycle,
      remark: formData.remark.trim()
    })
    if (res?.code === 200) {
      alert('提交成功！我们将在24小时内与您联系。')
      Object.assign(formData, {
        companyName: '', companyAddress: '', contactPerson: '',
        position: '', phone: '', email: '',
        requirementType: 'battery', budget: '', intention: [],
        deliveryCycle: '', remark: ''
      })
    } else {
      alert(res?.message || '提交失败')
    }
  } catch (e) {
    alert('网络异常，请稍后重试')
  }
}

const submitMediaForm = async () => {
  if (!mediaData.mediaName.trim()) { alert('请填写媒体名称'); return }
  if (!mediaData.name.trim()) { alert('请填写姓名'); return }
  if (!mediaData.email.trim()) { alert('请填写工作邮箱'); return }
  if (!mediaData.phone.trim()) { alert('请填写联系电话'); return }
  if (!mediaData.description.trim()) { alert('请填写详细需求描述'); return }

  try {
    const res = await api.submitCooperation({
      type: 'media',
      companyName: mediaData.mediaName.trim(),
      companyAddress: mediaData.mediaAddress.trim(),
      contactPerson: mediaData.name.trim(),
      position: mediaData.position.trim(),
      phone: mediaData.phone.trim(),
      email: mediaData.email.trim(),
      mediaTitle: mediaData.topic.trim(),
      mediaFormat: mediaData.format,
      mediaDate: mediaData.date,
      remark: mediaData.description.trim()
    })
    if (res?.code === 200) {
      alert('提交成功！我们将在24小时内与您联系。')
      Object.assign(mediaData, {
        mediaName: '', mediaAddress: '', name: '', position: '',
        email: '', phone: '', topic: '', date: '', format: '',
        purpose: [], description: ''
      })
    } else {
      alert(res?.message || '提交失败')
    }
  } catch (e) {
    alert('网络异常，请稍后重试')
  }
}
</script>

<style scoped>
.cooperation-banner {
  background: linear-gradient(135deg, #0d9488 0%, #0891b2 100%);
  padding: 60px 0;
  text-align: center;
  color: white;
}

.banner-content h1 {
  font-size: 2.5rem;
  font-weight: bold;
  margin-bottom: 16px;
}

.banner-content p {
  font-size: 1.25rem;
  opacity: 0.9;
}

.solution-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.card-icon {
  margin-bottom: 16px;
}

.solution-card h3 {
  color: #1a202c;
}

.solution-card p {
  color: #4a5568;
}

.contact-form-wrapper {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.contact-form {
  margin-top: 8px;
}

.form-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #0d9488;
}

.form-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-textarea:focus {
  outline: none;
  border-color: #0d9488;
}

.radio-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: 14px;
  color: #4a5568;
}

.radio-input {
  margin-right: 8px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: 14px;
  color: #4a5568;
}

.checkbox-input {
  margin-right: 8px;
}

.btn-primary {
  background: linear-gradient(135deg, #0d9488 0%, #0891b2 100%);
  color: white;
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-primary:hover {
  opacity: 0.9;
}
</style>