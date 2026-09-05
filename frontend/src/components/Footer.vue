<template>
  <footer id="footer" class="bg-dark text-white py-12">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
        <div class="md:col-span-2">
          <div class="flex items-center space-x-2 mb-4">
            <div class="w-10 h-10 bg-primary rounded-lg flex items-center justify-center">
              <span class="text-white text-xl font-bold">L</span>
            </div>
            <span class="text-xl font-bold">绿链锂电</span>
          </div>
          <p class="text-gray-400 mb-4">
            专注于锂电池产品的研发、生产与销售，为客户提供高品质的锂电解决方案。
          </p>
          <div class="flex space-x-4">
            <!-- X（原 Twitter）官网，新标签页打开 -->
            <a href="https://x.com" target="_blank" rel="noopener noreferrer" title="X (Twitter)" class="w-10 h-10 bg-gray-700 rounded-full flex items-center justify-center hover:bg-primary transition-colors">
              <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/>
              </svg>
            </a>
            <!-- Facebook 官网，新标签页打开 -->
            <a href="https://www.facebook.com" target="_blank" rel="noopener noreferrer" title="Facebook" class="w-10 h-10 bg-gray-700 rounded-full flex items-center justify-center hover:bg-primary transition-colors">
              <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 2C6.477 2 2 6.477 2 12c0 4.991 3.657 9.128 8.438 9.879V14.89h-2.54V12h2.54V9.797c0-2.506 1.492-3.89 3.777-3.89 1.094 0 2.238.195 2.238.195v2.46h-1.26c-1.243 0-1.63.771-1.63 1.562V12h2.773l-.443 2.89h-2.33v6.989C18.343 21.129 22 16.99 22 12c0-5.523-4.477-10-10-10z"/>
              </svg>
            </a>
            <!-- LinkedIn 领英官网（B2B 商务社交，与锂电采购平台客户群匹配），新标签页打开 -->
            <a href="https://www.linkedin.com" target="_blank" rel="noopener noreferrer" title="LinkedIn 领英" class="w-10 h-10 bg-gray-700 rounded-full flex items-center justify-center hover:bg-primary transition-colors">
              <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/>
              </svg>
            </a>
          </div>
        </div>

        <div>
          <h4 class="font-semibold mb-4">快速链接</h4>
          <ul class="space-y-2">
            <li><a href="#products" class="text-gray-400 hover:text-white transition-colors">产品商城</a></li>
            <li><a href="#news" class="text-gray-400 hover:text-white transition-colors">企业资讯</a></li>
            <li><a href="#about" class="text-gray-400 hover:text-white transition-colors">关于我们</a></li>
            <li><a href="#contact" class="text-gray-400 hover:text-white transition-colors">商务合作</a></li>
          </ul>
        </div>

        <div>
          <h4 class="font-semibold mb-4">联系方式</h4>
          <ul class="space-y-2 text-gray-400">
            <li class="flex items-center">
              <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
              </svg>
              深圳市南山区科技园
            </li>
            <li class="flex items-center">
              <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/>
              </svg>
              400-888-8888
            </li>
            <li class="flex items-center">
              <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
              </svg>
              contact@lithium.com
            </li>
          </ul>
        </div>
      </div>

      <div class="border-t border-gray-700 pt-8">
        <div class="flex flex-wrap items-center justify-between gap-4 mb-6">
          <p class="text-gray-400 text-sm">公司地址 | 联系电话 | 粤ICP备12345678号 | 版权所有 © 2024 绿链锂电</p>

          <!-- 管理后台入口：永远在页脚最右侧可见，不再难找 -->
          <router-link
            to="/admin/login"
            class="admin-entry-link"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1"/>
            </svg>
            <span>管理后台登录（运营/管理员入口）</span>
          </router-link>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.admin-entry-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(13, 148, 136, 0.18), rgba(20, 184, 166, 0.18));
  color: #5eead4;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid rgba(94, 234, 212, 0.35);
  text-decoration: none;
  transition: all .2s ease;
}

.admin-entry-link:hover {
  background: linear-gradient(135deg, rgba(13, 148, 136, 0.35), rgba(20, 184, 166, 0.3));
  color: #99f6e4;
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(13, 148, 136, 0.25);
}
</style>