<template>
  <div>
    <section class="cooperation-about-banner">
      <div class="banner-content">
        <h1>关于我们</h1>
        <p>专注锂电领域，打造新能源产业标杆</p>
      </div>
    </section>

    <section class="about-section py-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
          <div>
            <h2 class="text-3xl font-bold text-dark mb-6">企业简介</h2>
            <p class="text-gray-600 leading-relaxed mb-6">
              绿链锂电成立于2010年，是一家专注于锂电池研发、生产和销售的高新技术企业。公司拥有现代化的生产基地和先进的自动化生产线，致力于为全球客户提供高品质的锂电产品和解决方案。
            </p>
            <p class="text-gray-600 leading-relaxed">
              经过多年发展，公司已成为国内领先的锂电池供应商之一，产品广泛应用于新能源汽车、储能系统、电动工具等领域。我们始终坚持创新驱动发展，不断提升产品品质和服务水平，赢得了国内外客户的广泛认可。
            </p>
          </div>
          <div class="about-image">
            <img 
              src="/images/factory-building.png" 
              alt="新能源产业园厂区" 
              class="factory-image"
            />
          </div>
        </div>
      </div>
    </section>

    <section class="stats-section py-16 bg-gray-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div class="stat-card text-center">
            <div class="stat-number">15+</div>
            <div class="stat-label">年行业经验</div>
          </div>
          <div class="stat-card text-center">
            <div class="stat-number">500+</div>
            <div class="stat-label">合作企业</div>
          </div>
          <div class="stat-card text-center">
            <div class="stat-number">100万+</div>
            <div class="stat-label">产能规模</div>
          </div>
          <div class="stat-card text-center">
            <div class="stat-number">99.8%</div>
            <div class="stat-label">客户满意度</div>
          </div>
        </div>
      </div>
    </section>

    <section class="threejs-section py-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-8">
          <h2 class="text-3xl font-bold text-dark mb-4">3D厂区沙盘</h2>
          <p class="text-gray-500">交互式展示企业生产基地布局</p>
        </div>
        <div class="relative">
          <div ref="containerRef" class="threejs-container">
            <div v-if="!modelLoaded" class="model-placeholder">
              <div class="placeholder-content">
                <svg class="w-16 h-16 text-primary mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <p>Three.js 厂区3D沙盘</p>
                <p class="text-sm text-gray-500">{{ loadingText }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="honors-section py-16 bg-gray-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <h2 class="text-3xl font-bold text-dark mb-4">企业荣誉</h2>
          <p class="text-gray-500">绿链锂电历年斩获行业权威荣誉</p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <div 
            v-for="(honor, index) in honors" 
            :key="index"
            class="honor-card"
            @click="openPreview(honor)"
          >
            <div class="honor-image-wrapper">
              <img :src="honor.image" :alt="honor.title" class="honor-image" />
            </div>
            <div class="honor-info">
              <h3 class="honor-title">{{ honor.title }}</h3>
            </div>
          </div>
        </div>
      </div>
    </section>

    <GlobalLayout />

    <!-- 图片预览弹窗 -->
    <Teleport to="body">
      <div v-if="showPreview" class="preview-overlay" @click="closePreview">
        <div class="preview-close" @click.stop="closePreview">×</div>
        <div class="preview-content" @click.stop>
          <img :src="previewImage" :alt="previewTitle" class="preview-image" />
          <div class="preview-title">{{ previewTitle }}</div>
        </div>
      </div>
    </Teleport>

    <section class="contact-section py-16">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <h2 class="text-3xl font-bold text-dark mb-4">联系我们</h2>
          <p class="text-gray-500">欢迎企业客户洽谈合作</p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div class="contact-card">
            <div class="contact-icon">
              <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
              </svg>
            </div>
            <h3>公司地址</h3>
            <p>江苏省苏州市工业园区</p>
            <p>新能源科技产业园A座</p>
          </div>
          <div class="contact-card">
            <div class="contact-icon">
              <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
              </svg>
            </div>
            <h3>商务热线</h3>
            <p>400-888-9999</p>
            <p>0512-12345678</p>
          </div>
          <div class="contact-card">
            <div class="contact-icon">
              <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
              </svg>
            </div>
            <h3>电子邮箱</h3>
            <p>business@lidian.com</p>
            <p>cooperation@lidian.com</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useThreeJS } from '../composables/useThreeJS'
import GlobalLayout from '../components/GlobalLayout.vue'

const { 
  containerRef, 
  modelLoaded, 
  loadingText, 
  initThreeJS, 
  disposeThreeJS,
  getCamera,
  getControls,
  getRenderer
} = useThreeJS()

let isUnmounted = false

const getHonorImage = (name) => {
  try {
    return new URL(`../assets/images/${name}`, import.meta.url).href
  } catch (e) {
    console.error(`Image not found: ${name}`, e)
    return ''
  }
}

const honors = ref([
  {
    image: getHonorImage('1.png'),
    title: '国家高新技术企业',
    year: '——'
  },
  {
    image: getHonorImage('2 .png'),
    title: '省级企业技术中心',
    year: '——'
  },
  {
    image: getHonorImage('3.png'),
    title: '中国锂电池行业十强',
    year: '——'
  },
  {
    image: getHonorImage('4.png'),
    title: 'ISO9001质量管理认证',
    year: '——'
  },
  {
    image: getHonorImage('5 .png'),
    title: '绿色制造示范企业',
    year: '——'
  },
  {
    image: getHonorImage('6 .png'),
    title: '科技创新领军企业',
    year: '——'
  }
])

const previewImage = ref(null)
const previewTitle = ref('')
const showPreview = ref(false)

const openPreview = (honor) => {
  previewImage.value = honor.image
  previewTitle.value = honor.title
  showPreview.value = true
}

const closePreview = () => {
  showPreview.value = false
  previewImage.value = null
  previewTitle.value = ''
}

const handleResize = () => {
  if (isUnmounted) return
  
  const container = containerRef.value
  if (!container) return
  
  const width = container.clientWidth
  const height = container.clientHeight
  
  const camera = getCamera()
  const renderer = getRenderer()
  
  if (camera) {
    camera.aspect = width / height
    camera.updateProjectionMatrix()
  }
  if (renderer) {
    renderer.setSize(width, height)
  }
}

onMounted(() => {
  isUnmounted = false
  initThreeJS()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  isUnmounted = true
  
  const controls = getControls()
  const renderer = getRenderer()
  
  if (controls) {
    try {
      controls.dispose()
    } catch (e) {
      console.warn('[CooperationAbout] 控制器销毁失败:', e)
    }
  }
  
  window.removeEventListener('resize', handleResize)
  
  if (renderer) {
    try {
      renderer.dispose()
    } catch (e) {
      console.warn('[CooperationAbout] 渲染器销毁失败:', e)
    }
  }
  
  disposeThreeJS()
  
  console.log('[CooperationAbout] 组件已卸载，资源清理完成')
})
</script>

<style scoped>
.cooperation-about-banner {
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

.about-section {
  background: white;
}

.about-image {
  display: flex;
  align-items: center;
  justify-content: center;
}

.factory-image {
  width: 100%;
  height: auto;
  border-radius: 16px;
  object-fit: cover;
  max-height: 400px;
}

.stats-section {
  background: #f9fafb;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.stat-number {
  font-size: 2.5rem;
  font-weight: bold;
  color: #0d9488;
  margin-bottom: 8px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.threejs-section {
  background: white;
}

.threejs-container {
  background: linear-gradient(145deg, #87ceeb, #b0e0e6);
  border-radius: 16px;
  height: 500px;
  position: relative;
  overflow: hidden;
  overscroll-behavior: none;
  touch-action: none;
  user-select: none;
}

.model-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  top: 0;
  left: 0;
  background: rgba(135, 206, 235, 0.8);
}

.placeholder-content {
  text-align: center;
  color: #0891b2;
}

.text-primary {
  color: #0d9488;
}

.contact-section {
  background: white;
}

.contact-card {
  text-align: center;
  padding: 32px;
  background: #f9fafb;
  border-radius: 12px;
}

.contact-icon {
  width: 64px;
  height: 64px;
  background: #e0f2f1;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: #0d9488;
}

.contact-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.contact-card p {
  color: #666;
  margin-bottom: 4px;
}

.text-dark {
  color: #1f2937;
}

.honors-section {
  background: #f9fafb;
}

.honor-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.honor-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
}

.honor-image-wrapper {
  width: 100%;
  padding-top: 75%;
  position: relative;
  overflow: hidden;
}

.honor-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.honor-card:hover .honor-image {
  transform: scale(1.05);
}

.honor-info {
  padding: 20px;
  text-align: center;
}

.honor-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.honor-year {
  font-size: 14px;
  color: #0d9488;
  font-weight: 500;
}

.preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  cursor: pointer;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.preview-content {
  background: white;
  border-radius: 16px;
  padding: 24px;
  max-width: 90vw;
  max-height: 90vh;
  text-align: center;
  animation: scaleIn 0.3s ease;
}

@keyframes scaleIn {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.preview-image {
  max-width: 100%;
  max-height: 70vh;
  border-radius: 8px;
}

.preview-title {
  margin-top: 16px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.preview-close {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  transition: background 0.2s ease;
}

.preview-close:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>