<template>
  <section class="relative min-h-[250px] h-[250px] md:h-[267px] overflow-hidden bg-gradient-to-r from-[#0d9488] via-[#0f766e] to-[#0891b2]">
    <!-- 科技风 SVG 背景装饰（纯本地，零外部依赖） -->
    <div class="absolute inset-0 pointer-events-none">
      <!-- 左上三角网络 -->
      <svg class="absolute -top-10 -left-10 w-[320px] h-[320px] opacity-[0.15]" viewBox="0 0 200 200">
        <defs>
          <pattern id="tri" width="40" height="34.64" patternUnits="userSpaceOnUse">
            <polygon points="20,0 40,34.64 0,34.64" fill="none" stroke="#fff" stroke-width="0.6"/>
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#tri)"/>
      </svg>
      <!-- 右下抽象电池 -->
      <svg class="absolute bottom-0 right-0 w-[280px] h-[280px] opacity-[0.2]" viewBox="0 0 200 200">
        <rect x="30" y="50" width="140" height="90" rx="10" fill="none" stroke="#fff" stroke-width="1.2"/>
        <rect x="60" y="35" width="80" height="20" rx="4" fill="none" stroke="#fff" stroke-width="1"/>
        <rect x="40" y="60" width="120" height="70" rx="6" fill="#fff" fill-opacity="0.15"/>
        <rect x="48" y="68" width="35" height="54" rx="3" fill="#fff" fill-opacity="0.35"/>
        <rect x="88" y="68" width="35" height="54" rx="3" fill="#fff" fill-opacity="0.25"/>
        <rect x="128" y="68" width="24" height="54" rx="3" fill="#fff" fill-opacity="0.15"/>
        <line x1="20" y1="170" x2="180" y2="170" stroke="#fff" stroke-width="0.6" stroke-dasharray="3 4"/>
        <circle cx="50" cy="185" r="2" fill="#fff" fill-opacity="0.5"/>
        <circle cx="100" cy="185" r="2" fill="#fff" fill-opacity="0.5"/>
        <circle cx="150" cy="185" r="2" fill="#fff" fill-opacity="0.5"/>
      </svg>
      <!-- 发光圆环 -->
      <svg class="absolute top-6 right-10 w-[120px] h-[120px] opacity-20" viewBox="0 0 100 100">
        <circle cx="50" cy="50" r="42" fill="none" stroke="#fff" stroke-width="1.2" stroke-dasharray="8 12"/>
        <circle cx="50" cy="50" r="32" fill="none" stroke="#fff" stroke-width="0.8" opacity="0.6"/>
        <circle cx="50" cy="50" r="8" fill="#fff" fill-opacity="0.5"/>
      </svg>
    </div>

    <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-full flex items-center">
      <div class="max-w-2xl">
        <!-- 主标题与副标题同行基线对齐：副标题右移并上移，与【标准化锂电现货】处于同一水平基线 -->
        <div class="flex flex-wrap items-baseline gap-x-10">
          <h1 class="text-4xl md:text-5xl font-bold text-white">
            标准化锂电现货
          </h1>
          <p class="text-xl text-white/90">
            小批量快速发货 | 品质保证 | 专业服务
          </p>
        </div>
      </div>
    </div>

    <div class="absolute bottom-[-1px] left-0 right-0">
      <svg viewBox="0 0 1440 60" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M0 60L60 55C120 50 240 40 360 35C480 30 600 30 720 32C840 34 960 38 1080 38C1200 38 1320 34 1380 32L1440 30V60H1380C1320 60 1200 60 1080 60C960 60 840 60 720 60C600 60 480 60 360 60C240 60 120 60 60 60H0Z" fill="#F9FAFB"/>
      </svg>
    </div>
  </section>
</template>