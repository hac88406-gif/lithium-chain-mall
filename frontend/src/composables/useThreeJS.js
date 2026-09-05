import { ref } from 'vue';

export function useThreeJS() {
  const containerRef = ref(null);
  const modelLoaded = ref(false);
  const loadingText = ref('模型加载中...');
  let scene, camera, renderer, model, controls, animationId;
  let clouds = [];
  let isUnmounted = false;
  let handleResize = null;
  let handleTouchStart = null;
  let handleTouchMove = null;
  const CLOUD_MIN_Y = 220;
  const CLOUD_MAX_Y = 380;
  const CLOUD_COUNT = Math.floor(Math.random() * 10) + 50;

  const createSoftCloudTexture = (canvas) => {
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    const gradient = ctx.createRadialGradient(width / 2, height / 2, 0, width / 2, height / 2, width / 2);
    gradient.addColorStop(0, 'rgba(255, 255, 255, 0.9)');
    gradient.addColorStop(0.5, 'rgba(255, 255, 255, 0.4)');
    gradient.addColorStop(1, 'rgba(255, 255, 255, 0)');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, width, height);
    for (let i = 0; i < 20; i++) {
      const x = Math.random() * width;
      const y = Math.random() * height;
      const radius = Math.random() * 20 + 10;
      const alpha = Math.random() * 0.15;
      ctx.beginPath();
      ctx.arc(x, y, radius, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(255, 255, 255, ${alpha})`;
      ctx.fill();
    }
    return canvas;
  };

  const createCloudLayer = (scene, Group, Mesh, PlaneGeometry, MeshPhongMaterial, CanvasTexture) => {
    if (!scene) return;
    
    for (let i = 0; i < CLOUD_COUNT; i++) {
      const cloudGroup = new Group();
      const cloudHeight = CLOUD_MIN_Y + Math.random() * (CLOUD_MAX_Y - CLOUD_MIN_Y);
      const baseX = (Math.random() - 0.5) * 500;
      const baseZ = (Math.random() - 0.5) * 500;
      const opacityRange = cloudHeight > 350 ? [0.35, 0.52] : cloudHeight > 310 ? [0.28, 0.45] : cloudHeight > 260 ? [0.25, 0.4] : [0.22, 0.35];
      const numPlanes = Math.floor(Math.random() * 5) + 6;
      
      for (let j = 0; j < numPlanes; j++) {
        const canvas = document.createElement('canvas');
        canvas.width = 256;
        canvas.height = 256;
        createSoftCloudTexture(canvas);
        const cloudTexture = new CanvasTexture(canvas);
        const opacity = Math.random() * (opacityRange[1] - opacityRange[0]) + opacityRange[0];
        
        const material = new MeshPhongMaterial({
          color: 0xffffff,
          transparent: true,
          opacity: opacity,
          depthWrite: false,
          depthTest: true,
          side: 2,
          alphaMap: cloudTexture,
          shininess: 1,
          specular: 0xffffff,
          blending: 1
        });
        
        const geometry = new PlaneGeometry(35 + Math.random() * 45, 35 + Math.random() * 45);
        const plane = new Mesh(geometry, material);
        
        plane.position.x = baseX + (Math.random() - 0.5) * 80;
        plane.position.y = cloudHeight + (Math.random() - 0.5) * 25;
        plane.position.z = baseZ + (Math.random() - 0.5) * 80;
        plane.rotation.x = Math.random() * Math.PI;
        plane.rotation.y = Math.random() * Math.PI;
        plane.rotation.z = Math.random() * Math.PI;
        plane.scale.set(0.9 + Math.random() * 0.7, 0.9 + Math.random() * 0.7, 1);
        
        cloudGroup.add(plane);
      }
      
      clouds.push(cloudGroup);
      scene.add(cloudGroup);
    }
  };

  const initThreeJS = async () => {
    if (!containerRef.value) return;
    
    try {
      const THREE = await import('three');
      const { OrbitControls } = await import('three/examples/jsm/controls/OrbitControls.js');
      const { GLTFLoader } = await import('three/examples/jsm/loaders/GLTFLoader.js');
      
      const { Scene, PerspectiveCamera, WebGLRenderer, AmbientLight, DirectionalLight, GridHelper, 
              BoxGeometry, MeshPhongMaterial, Mesh, Box3, Vector3, Color, Fog, 
              PlaneGeometry, CanvasTexture, Group } = THREE;

      scene = new Scene();
      scene.background = new Color(0xa8d8ea);
      scene.fog = new Fog(0xa8d8ea, 120, 380);

      const width = containerRef.value.clientWidth;
      const height = containerRef.value.clientHeight;

      camera = new PerspectiveCamera(60, width / height, 0.1, 1000);
      camera.position.set(100, 350, 100);

      renderer = new WebGLRenderer({ antialias: true });
      renderer.setSize(width, height);
      renderer.setPixelRatio(window.devicePixelRatio);
      renderer.domElement.style.touchAction = 'none';
      containerRef.value.appendChild(renderer.domElement);

      handleTouchStart = (event) => {
        if (event.touches.length === 1 || event.touches.length === 2) {
          event.preventDefault();
        }
      };

      handleTouchMove = (event) => {
        event.preventDefault();
      };

      renderer.domElement.addEventListener('touchstart', handleTouchStart, { passive: false });
      renderer.domElement.addEventListener('touchmove', handleTouchMove, { passive: false });

      controls = new OrbitControls(camera, renderer.domElement);
      controls.enablePan = true;
      controls.enableZoom = true;
      controls.enableRotate = true;
      controls.enableDamping = true;
      controls.dampingFactor = 0.05;
      controls.target.set(0, 5, 0);
      controls.screenSpacePanning = true;
      controls.maxPolarAngle = Math.PI / 2.2;
      controls.minDistance = 30;
      controls.maxDistance = 500;
      controls.mouseButtons = {
        LEFT: THREE.MOUSE.ROTATE,
        MIDDLE: THREE.MOUSE.PAN,
        RIGHT: THREE.MOUSE.PAN
      };
      controls.touches = {
        ONE: THREE.TOUCH.ROTATE,
        TWO: THREE.TOUCH.DOLLY_PAN
      };
      controls.enabled = true;

      const ambientLight = new AmbientLight(0xffffff, 0.9);
      scene.add(ambientLight);

      const directionalLight = new DirectionalLight(0xffffff, 1.5);
      directionalLight.position.set(50, 150, 50);
      scene.add(directionalLight);

      const gridHelper = new GridHelper(200, 50, 0x009688, 0x80cbc4);
      gridHelper.position.y = 0;
      scene.add(gridHelper);

      createCloudLayer(scene, Group, Mesh, PlaneGeometry, MeshPhongMaterial, CanvasTexture);

      const loader = new GLTFLoader();
      try {
        loadingText.value = '正在加载模型...';
        const result = await loader.loadAsync('/model/Green-chain.glb');
        model = result.scene;
        model.scale.set(1, 1, 1);
        
        if (scene && model) {
          scene.add(model);
          const box = new Box3().setFromObject(model);
          const center = box.getCenter(new Vector3());
          model.position.sub(center);
          model.position.y = 0;
        }
        
        modelLoaded.value = true;
        loadingText.value = '';
      } catch (error) {
        console.error('模型加载失败:', error);
        loadingText.value = '模型加载失败';
        
        if (scene) {
          const cube = new Mesh(new BoxGeometry(30, 10, 30), new MeshPhongMaterial({ color: 0x009688 }));
          cube.position.y = 5;
          scene.add(cube);
          
          const smallBox1 = new Mesh(new BoxGeometry(8, 15, 8), new MeshPhongMaterial({ color: 0x00796b }));
          smallBox1.position.set(-15, 12.5, 0);
          scene.add(smallBox1);
          
          const smallBox2 = new Mesh(new BoxGeometry(8, 15, 8), new MeshPhongMaterial({ color: 0x00796b }));
          smallBox2.position.set(15, 12.5, 0);
          scene.add(smallBox2);
        }
      }

      const animate = () => {
        if (isUnmounted) {
          return;
        }
        animationId = requestAnimationFrame(animate);
        if (controls) {
          controls.update();
        }
        if (renderer && scene && camera) {
          renderer.render(scene, camera);
        }
      };
      animate();

      handleResize = () => {
        if (isUnmounted) return;
        if (!containerRef.value) return;
        const width = containerRef.value.clientWidth;
        const height = containerRef.value.clientHeight;
        
        if (camera) {
          camera.aspect = width / height;
          camera.updateProjectionMatrix();
        }
        if (renderer) {
          renderer.setSize(width, height);
        }
      };
      window.addEventListener('resize', handleResize);

    } catch (error) {
      console.error('Three.js 初始化失败:', error);
      loadingText.value = 'Three.js 加载失败';
    }
  };

  const resetView = () => {
    if (!controls || !camera) return;
    
    controls.enabled = false;
    const startPos = { x: camera.position.x, y: camera.position.y, z: camera.position.z };
    const startTarget = { x: controls.target.x, y: controls.target.y, z: controls.target.z };
    const endPos = { x: 100, y: 350, z: 100 };
    const endTarget = { x: 0, y: 5, z: 0 };
    const duration = 1000;
    const startTime = performance.now();

    const animateReset = (currentTime) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const easeProgress = 1 - Math.pow(1 - progress, 3);
      
      camera.position.x = startPos.x + (endPos.x - startPos.x) * easeProgress;
      camera.position.y = startPos.y + (endPos.y - startPos.y) * easeProgress;
      camera.position.z = startPos.z + (endPos.z - startPos.z) * easeProgress;
      
      controls.target.x = startTarget.x + (endTarget.x - startTarget.x) * easeProgress;
      controls.target.y = startTarget.y + (endTarget.y - startTarget.y) * easeProgress;
      controls.target.z = startTarget.z + (endTarget.z - startTarget.z) * easeProgress;
      
      controls.update();
      
      if (progress < 1) {
        requestAnimationFrame(animateReset);
      } else {
        controls.enabled = false;
      }
    };
    
    requestAnimationFrame(animateReset);
  };

  const disposeThreeJS = () => {
    isUnmounted = true;
    
    if (animationId) {
      cancelAnimationFrame(animationId);
      animationId = null;
    }
    
    if (handleResize) {
      window.removeEventListener('resize', handleResize);
      handleResize = null;
    }

    if (renderer && renderer.domElement) {
      if (handleTouchStart) {
        renderer.domElement.removeEventListener('touchstart', handleTouchStart, { passive: false });
        handleTouchStart = null;
      }
      if (handleTouchMove) {
        renderer.domElement.removeEventListener('touchmove', handleTouchMove, { passive: false });
        handleTouchMove = null;
      }
    }

    clouds.forEach(cloud => {
      cloud.traverse((obj) => {
        if (obj.geometry) obj.geometry.dispose();
        if (obj.material) {
          if (Array.isArray(obj.material)) {
            obj.material.forEach(m => m.dispose());
          } else {
            obj.material.dispose();
          }
        }
      });
    });
    clouds = [];

    if (model) {
      model.traverse((object) => {
        if (object.geometry) object.geometry.dispose();
        if (object.material) {
          if (Array.isArray(object.material)) {
            object.material.forEach(m => m.dispose());
          } else {
            object.material.dispose();
          }
        }
      });
    }

    if (controls) {
      try {
        controls.dispose();
      } catch (e) {
        console.warn('[useThreeJS] 控制器销毁失败:', e);
      }
    }

    if (renderer) {
      try {
        renderer.dispose();
      } catch (e) {
        console.warn('[useThreeJS] 渲染器销毁失败:', e);
      }
      if (containerRef.value && renderer.domElement) {
        containerRef.value.removeChild(renderer.domElement);
      }
    }

    scene = null;
    camera = null;
    renderer = null;
    model = null;
    controls = null;
    
    console.log('[useThreeJS] Three.js资源清理完成');
  };

  return {
    containerRef,
    modelLoaded,
    loadingText,
    initThreeJS,
    resetView,
    disposeThreeJS,
    getCamera: () => camera,
    getControls: () => controls,
    getRenderer: () => renderer
  };
}