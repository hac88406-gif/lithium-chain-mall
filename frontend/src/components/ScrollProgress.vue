<template>
  <div class="scroll-progress-wrap">
    <div class="global-progress"></div>
  </div>
  <svg 
    class="energy-icon" 
    t="1785075442036" 
    viewBox="0 0 1024 1024" 
    version="1.1" 
    xmlns="http://www.w3.org/2000/svg" 
    width="18" 
    height="14"
  >
    <g transform="rotate(90 512 512)">
      <path d="M512 64.32L288.05 512.44l222.61 111.81-110.63 335.43 335.92-447.24-225.29-111.81z" fill="#FFD500"></path>
    </g>
  </svg>
</template>

<style scoped>
.scroll-progress-wrap {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 5px;
  z-index: 9999;
  pointer-events: none;
}

.global-progress {
  width: 100%;
  height: 5px;
  border-radius: 4px;
  background: linear-gradient(90deg, #16a34a, #84cc16);
  transform-origin: left center;
  animation: progress-grow linear;
  animation-timeline: scroll(root block);
}

.energy-icon {
  position: fixed;
  top: 8px;
  left: 0;
  z-index: 10000;
  pointer-events: none;
  animation: icon-move linear;
  animation-timeline: scroll(root block);
}

@keyframes progress-grow {
  from {
    transform: scaleX(0);
  }
  to {
    transform: scaleX(1);
  }
}

@keyframes icon-move {
  from {
    left: 0;
  }
  to {
    left: calc(100% - 18px);
  }
}
</style>