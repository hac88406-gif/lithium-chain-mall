<template>
  <section class="global-section py-20">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="text-center mb-16">
        <h2 class="text-3xl font-bold text-dark mb-4">全球战略布局</h2>
        <p class="text-gray-500">海内外营销网络布局</p>
      </div>

      <div class="map-container">
        <div class="map-inner">
          <!-- 中层：SVG连线 -->
          <svg class="map-lines-main" viewBox="0 0 100 100" preserveAspectRatio="none">
            <!-- 长沙到北美 -->
            <path d="M67,52 Q50,49 33,43" stroke="#f97316" stroke-width="0.35" fill="none"/>
            <!-- 长沙到英国 -->
            <path d="M67,52 Q60,44 48,34" stroke="#f97316" stroke-width="0.35" fill="none"/>
            <!-- 长沙到肯尼亚 -->
            <path d="M67,52 Q64,54 57,60" stroke="#f97316" stroke-width="0.35" fill="none"/>
            <!-- 长沙到南非 -->
            <path d="M67,52 Q60,60 53,72" stroke="#f97316" stroke-width="0.35" fill="none"/>
            <!-- 长沙到澳大利亚 -->
            <path d="M67,52 Q70,62 74,78" stroke="#f97316" stroke-width="0.35" fill="none"/>
          </svg>

          <!-- 顶层：圆点 + 地点标签 -->
          
          <!-- 国内总部：长沙 -->
          <div class="location-marker origin" style="left:67%; top:52%">
            <div class="marker-dot origin-dot"></div>
            <div class="marker-label origin-label">长沙</div>
          </div>

          <!-- 北美 -->
          <div class="location-marker" style="left:33%; top:43%">
            <div class="marker-dot overseas-dot"></div>
            <div class="marker-label overseas-label">北美</div>
          </div>

          <!-- 英国 -->
          <div class="location-marker" style="left:48%; top:34%">
            <div class="marker-dot overseas-dot"></div>
            <div class="marker-label overseas-label">英国</div>
          </div>

          <!-- 肯尼亚 -->
          <div class="location-marker" style="left:57%; top:60%">
            <div class="marker-dot overseas-dot"></div>
            <div class="marker-label overseas-label">肯尼亚</div>
          </div>

          <!-- 南非 -->
          <div class="location-marker" style="left:53%; top:72%">
            <div class="marker-dot overseas-dot"></div>
            <div class="marker-label overseas-label">南非</div>
          </div>

          <!-- 澳大利亚 -->
          <div class="location-marker" style="left:74%; top:78%">
            <div class="marker-dot overseas-dot"></div>
            <div class="marker-label overseas-label">澳大利亚</div>
          </div>
        </div>

        <!-- 图例 -->
        <div class="map-legend">
          <div class="legend-item">
            <div class="legend-marker origin"></div>
            <span>国内总部</span>
          </div>
          <div class="legend-item">
            <div class="legend-marker overseas"></div>
            <span>海外网点</span>
          </div>
          <div class="legend-item">
            <div class="legend-line main"></div>
            <span>主干连线</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
</script>

<style scoped>
.global-section {
  background: white;
}

.map-container {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  padding: 40px;
}

.map-inner {
  position: relative;
  width: 100%;
  padding-top: 50%;
  background: white;
  background-image: url('../assets/images/globe.jpg');
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
}

.map-lines-main {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.location-marker {
  position: absolute;
  transform: translate(-50%, -50%);
  cursor: pointer;
  transition: all 0.3s ease;
}

.location-marker:hover {
  z-index: 10;
}

.location-marker:hover .marker-dot {
  transform: scale(1.3);
}

.location-marker:hover .marker-label {
  transform: translate(-50%, -50%) scale(1.05);
}

.marker-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  transition: transform 0.3s ease;
}

.origin-dot {
  background: #059669;
  box-shadow: 0 0 0 4px rgba(5, 150, 105, 0.2);
}

.overseas-dot {
  width: 14px;
  height: 14px;
  background: transparent;
  border: 3px solid #f97316;
}

.marker-label {
  position: absolute;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 12px;
  transition: transform 0.3s ease;
}

.origin-label {
  bottom: -32px;
  left: 50%;
  transform: translateX(-50%);
  background: white;
  color: #059669;
  border: 2px solid #059669;
}

.overseas-label {
  top: -28px;
  left: 50%;
  transform: translateX(-50%);
  background: white;
  color: #f97316;
  border: 2px solid #f97316;
}

.map-legend {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #6b7280;
}

.legend-marker {
  width: 14px;
  height: 14px;
  border-radius: 50%;
}

.legend-marker.origin {
  background: #059669;
}

.legend-marker.overseas {
  background: transparent;
  border: 3px solid #f97316;
}

.legend-line {
  width: 28px;
  height: 3px;
  border-radius: 2px;
}

.legend-line.main {
  background: #f97316;
}

.text-dark {
  color: #1f2937;
}

@media (max-width: 768px) {
  .map-container {
    padding: 24px;
  }
  
  .marker-label {
    font-size: 10px;
    padding: 3px 8px;
  }
  
  .marker-dot {
    width: 12px;
    height: 12px;
  }
  
  .overseas-dot {
    width: 10px;
    height: 10px;
    border-width: 2px;
  }
  
  .map-legend {
    flex-wrap: wrap;
    gap: 16px;
  }
  
  .legend-item {
    font-size: 12px;
  }
}
</style>