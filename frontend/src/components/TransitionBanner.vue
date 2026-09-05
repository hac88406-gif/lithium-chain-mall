<template>
  <section class="transition-banner">
    <div class="banner-content">
      <span class="banner-text">科技赋能锂电 · 绿色驱动未来</span>
    </div>
  </section>
</template>

<script setup>
</script>

<style scoped>
.transition-banner {
  height: 140px;
  background: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36px 0;
  padding-top: 84px;
  margin-bottom: 36px;
}

.banner-content {
  text-align: center;
}

.banner-text {
  font-size: 2.25rem;
  font-weight: 700;
  color: #059669;
  letter-spacing: 3px;
}

@media (min-width: 768px) {
  .banner-text {
    font-size: 3rem;
  }
}

@media (max-width: 768px) {
  .banner-text {
    font-size: 1.75rem;
    letter-spacing: 2px;
    padding: 0 16px;
  }
}

@media (max-width: 480px) {
  .banner-text {
    font-size: 1.5rem;
    letter-spacing: 1px;
  }
}
</style>