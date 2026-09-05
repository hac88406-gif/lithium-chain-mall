<template>
  <div class="py-16">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="text-center mb-12">
        <h2 class="text-3xl font-bold text-dark mb-4">关于我们</h2>
        <p class="text-gray-500">了解公司发展历程和核心优势</p>
      </div>

      <div class="about-content">
        <div class="about-text">
          <h3 class="text-2xl font-semibold text-dark mb-6">公司简介</h3>
          <p class="text-gray-600 mb-6 leading-relaxed">
            绿链锂电科技有限公司成立于2015年，是一家专注于锂电池研发、生产和销售的高新技术企业。公司拥有先进的生产设备和专业的技术团队，致力于为客户提供高品质的锂电产品和解决方案。
          </p>
          <p class="text-gray-600 mb-6 leading-relaxed">
            我们的产品广泛应用于新能源汽车、储能系统、便携式电源等领域，产品远销国内外，深受客户信赖。
          </p>
        </div>

        <div class="about-stats">
          <div class="stat-item">
            <div class="stat-value">500+</div>
            <div class="stat-label">合作客户</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">10年</div>
            <div class="stat-label">行业经验</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">50万㎡</div>
            <div class="stat-label">生产基地</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">200+</div>
            <div class="stat-label">专利技术</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.about-content {
  background: white;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.about-text {
  max-width: 800px;
  margin: 0 auto 40px;
}

.about-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px;
}

.stat-item {
  text-align: center;
  padding: 24px;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  border-radius: 12px;
}

.stat-value {
  font-size: 2.5rem;
  font-weight: bold;
  color: #0d9488;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .about-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>