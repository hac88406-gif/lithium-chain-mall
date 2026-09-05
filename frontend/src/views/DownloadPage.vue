<template>
  <div>
    <section class="download-banner">
      <div class="banner-content">
        <h1>资料下载</h1>
        <p>下载企业相关资料，了解更多产品信息</p>
      </div>
    </section>

    <section class="download-section py-16">
      <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <h2 class="text-3xl font-bold text-dark mb-4">下载中心</h2>
          <p class="text-gray-500">以下资料可供下载，无需登录即可获取</p>
        </div>

        <div class="space-y-6">
          <div 
            v-for="(file, index) in files" 
            :key="index"
            class="download-card"
          >
            <div class="file-info">
              <div class="file-icon">
                <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
              </div>
              <div class="file-details">
                <h3 class="file-name">{{ file.name }}</h3>
                <p class="file-size">{{ file.size }}</p>
              </div>
            </div>
            <button 
              @click="handleDownload(file)"
              class="download-btn"
            >
              <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
              </svg>
              下载
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const files = ref([
  { 
    name: '企业介绍手册.pdf', 
    size: '3.2 MB',
    url: '#'
  },
  { 
    name: '产能参数文档.pdf', 
    size: '1.8 MB',
    url: '#'
  }
])

const handleDownload = (file) => {
  alert(`开始下载：${file.name}\n\n文件大小：${file.size}\n\n由于这是演示版本，实际下载功能需要后端支持。`)
}
</script>

<style scoped>
.download-banner {
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

.download-section {
  background: white;
}

.download-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f9fafb;
  border-radius: 16px;
  padding: 20px 24px;
  transition: all 0.3s ease;
}

.download-card:hover {
  background: #f3f4f6;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.file-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.file-icon {
  width: 56px;
  height: 56px;
  background: #e0f2f1;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0d9488;
}

.file-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.file-size {
  font-size: 14px;
  color: #6b7280;
}

.download-btn {
  display: inline-flex;
  align-items: center;
  padding: 12px 24px;
  background: linear-gradient(135deg, #0d9488 0%, #0891b2 100%);
  color: white;
  font-size: 14px;
  font-weight: 500;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 150, 136, 0.3);
}

.download-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 150, 136, 0.4);
}

.download-btn:active {
  transform: translateY(0);
}

.text-dark {
  color: #1f2937;
}

@media (max-width: 768px) {
  .download-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .download-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>