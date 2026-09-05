<template>
  <div>
    <Banner />
    <TransitionBanner />

    <section id="products" class="products-with-sidebar">
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div class="flex gap-6 items-start">
          <!-- 左侧：热销现货商品（ProductSection） -->
          <div class="flex-1 min-w-0">
            <ProductSection :maxItems="6" />
          </div>
          <!-- 右侧：热销榜 TOP10 —— 顶部对齐到左侧「热销现货 产品」标题行
               ProductSection 内部 .ps-root 有 padding-top: 56px，
               所以 HotRanking 下移 56px 让两者顶部水平对齐 -->
          <div class="w-[260px] flex-shrink-0 hidden lg:block hot-ranking-anchor">
            <HotRanking />
          </div>
        </div>
      </div>
    </section>

    <AdvantageSection />
  </div>
</template>

<script setup>
import Banner from '../components/Banner.vue'
import TransitionBanner from '../components/TransitionBanner.vue'
import ProductSection from '../components/ProductSection.vue'
import AdvantageSection from '../components/AdvantageSection.vue'
import HotRanking from '../components/HotRanking.vue'
</script>

<style scoped>
.products-with-sidebar {
  background: #f9fafb;
}

/* 让热销榜顶部对齐左侧 ProductSection 内部的标题行 */
/* ProductSection .ps-root { padding: 56px 0 48px }，这里用相同值补偿 */
.hot-ranking-anchor {
  margin-top: 56px;
}

@media (max-width: 1024px) {
  .products-with-sidebar .flex {
    flex-direction: column;
  }
  /* 移动端单列时去掉补偿 margin，恢复紧贴顶部 */
  .hot-ranking-anchor {
    margin-top: 0;
  }
}
</style>