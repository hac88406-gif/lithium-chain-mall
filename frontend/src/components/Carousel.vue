<template>
  <section class="carousel-section my-12">
    <div class="carousel-container relative w-full h-[400px] overflow-hidden">
      <!-- 轮播图片 -->
      <div 
        v-for="(slide, index) in slides" 
        :key="index"
        class="carousel-slide absolute inset-0 transition-opacity duration-700"
        :class="{ 'opacity-100 z-10': currentIndex === index, 'opacity-0 z-0': currentIndex !== index }"
      >
        <div 
          class="w-full h-full bg-cover bg-center"
          :style="{ backgroundImage: `url(${slide.image})` }"
        >
          <div class="w-full h-full bg-black/40 flex items-center justify-center">
            <div class="text-center text-white px-4">
              <p class="text-lg md:text-xl mb-4 opacity-90">{{ slide.topText }}</p>
              <h2 class="text-4xl md:text-6xl font-bold mb-4">{{ slide.title }}</h2>
              <p class="text-lg md:text-xl opacity-90">{{ slide.bottomText }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 左箭头 -->
      <button 
        @click="prevSlide"
        class="carousel-arrow carousel-arrow-left absolute left-4 top-1/2 -translate-y-1/2 z-20 w-12 h-12 rounded-full bg-white/20 backdrop-blur-sm flex items-center justify-center text-white transition-all duration-300 hover:bg-white/40 hover:scale-110"
      >
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
      </button>

      <!-- 右箭头 -->
      <button 
        @click="nextSlide"
        class="carousel-arrow carousel-arrow-right absolute right-4 top-1/2 -translate-y-1/2 z-20 w-12 h-12 rounded-full bg-white/20 backdrop-blur-sm flex items-center justify-center text-white transition-all duration-300 hover:bg-white/40 hover:scale-110"
      >
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
        </svg>
      </button>

      <!-- 指示器 -->
      <div class="absolute bottom-6 left-1/2 -translate-x-1/2 z-20 flex gap-3">
        <button 
          v-for="(_, index) in slides" 
          :key="index"
          @click="goToSlide(index)"
          class="w-3 h-3 rounded-full transition-all duration-300"
          :class="currentIndex === index ? 'bg-white w-8' : 'bg-white/50 hover:bg-white/70'"
        ></button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const currentIndex = ref(0)
let autoPlayInterval = null

const slides = ref([
  {
    image: new URL('../assets/images/science.jpeg', import.meta.url).href,
    topText: '品质有标准 服务无极限',
    title: 'Science & Technology',
    bottomText: '科技·创新未来'
  },
  {
    image: new URL('../assets/images/effection.jpeg', import.meta.url).href,
    topText: '品质有标准 服务无极限',
    title: 'Efficiency',
    bottomText: '高效·绿色能源'
  },
  {
    image: new URL('../assets/images/green.jpg', import.meta.url).href,
    topText: '品质有标准 服务无极限',
    title: 'Green Energy',
    bottomText: '绿色·可持续发展'
  }
])

const nextSlide = () => {
  currentIndex.value = (currentIndex.value + 1) % slides.value.length
}

const prevSlide = () => {
  currentIndex.value = (currentIndex.value - 1 + slides.value.length) % slides.value.length
}

const goToSlide = (index) => {
  currentIndex.value = index
}

const startAutoPlay = () => {
  autoPlayInterval = setInterval(nextSlide, 5000)
}

const stopAutoPlay = () => {
  if (autoPlayInterval) {
    clearInterval(autoPlayInterval)
  }
}

onMounted(() => {
  startAutoPlay()
})

onUnmounted(() => {
  stopAutoPlay()
})
</script>

<style scoped>
.carousel-section {
  margin-top: 2rem;
  margin-bottom: 2rem;
}

.carousel-container {
  max-width: 100%;
  position: relative;
}

.carousel-slide {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.carousel-arrow {
  cursor: pointer;
  border: none;
  outline: none;
}

.carousel-arrow:hover {
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
}

@media (max-width: 768px) {
  .carousel-container {
    height: 280px;
  }
  
  .carousel-slide h2 {
    font-size: 2rem !important;
  }
  
  .carousel-slide p {
    font-size: 0.9rem !important;
  }
  
  .carousel-arrow {
    width: 36px;
    height: 36px;
  }
  
  .carousel-arrow svg {
    width: 18px;
    height: 18px;
  }
}

@media (max-width: 480px) {
  .carousel-container {
    height: 220px;
  }
  
  .carousel-slide h2 {
    font-size: 1.5rem !important;
  }
}
</style>