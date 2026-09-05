<template>
  <div class="py-16">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="text-center mb-12">
        <h2 class="text-3xl font-bold text-dark mb-4">企业资讯</h2>
        <p class="text-gray-500">了解最新行业动态和公司新闻</p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div v-for="(news, index) in newsList" :key="index" class="news-card">
          <div class="news-image">
            <img :src="news.image" :alt="news.title" />
          </div>
          <div class="news-content">
            <span class="news-date">{{ news.date }}</span>
            <h3 class="news-title">{{ news.title }}</h3>
            <p class="news-desc">{{ news.desc }}</p>
            <a href="#" class="news-link">阅读更多 →</a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const newsList = [
  {
    title: '公司荣获新能源行业创新奖',
    desc: '我司凭借创新技术和优质服务，荣获2024年度新能源行业创新奖。',
    date: '2024-01-15',
    image: 'https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=modern%20award%20ceremony%2C%20business%20people%2C%20professional%20event&image_size=square'
  },
  {
    title: '新生产线正式投产',
    desc: '投资5000万元的全新自动化生产线正式投产运营。',
    date: '2024-01-10',
    image: 'https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=modern%20factory%20interior%2C%20automated%20production%20line%2C%20industrial&image_size=square'
  },
  {
    title: '与XX企业达成战略合作',
    desc: '双方将在储能领域展开深度合作，共同推动行业发展。',
    date: '2024-01-05',
    image: 'https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=business%20meeting%2C%20partnership%20agreement%2C%20professional&image_size=square'
  }
]
</script>

<style scoped>
.news-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.news-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.news-image {
  height: 200px;
  overflow: hidden;
}

.news-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.news-content {
  padding: 20px;
}

.news-date {
  font-size: 12px;
  color: #999;
}

.news-title {
  font-size: 18px;
  font-weight: 600;
  color: #2C3E50;
  margin: 12px 0;
}

.news-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.news-link {
  display: inline-block;
  margin-top: 16px;
  color: #0d9488;
  font-size: 14px;
  text-decoration: none;
}

.news-link:hover {
  text-decoration: underline;
}
</style>