import { Vector3 } from 'three'

export function useMouseControls() {
  let isLeftMouseDown = false
  let isRightMouseDown = false
  let isMiddleMouseDown = false
  let mouseStartInCanvas = false
  let lastMouseX = 0
  let lastMouseY = 0
  let sphericalRadius = 0
  let sphericalTheta = 0
  let sphericalPhi = 0

  let mouseDownHandler = null
  let mouseMoveHandler = null
  let mouseUpHandler = null
  let mouseLeaveHandler = null
  let wheelHandler = null
  let handleGlobalMouseUp = null
  
  // 触摸事件相关
  let isTouching = false
  let lastTouchDistance = 0
  let lastTouchCenterX = 0
  let lastTouchCenterY = 0
  let touchStartInCanvas = false
  
  let touchStartHandler = null
  let touchMoveHandler = null
  let touchEndHandler = null
  let touchCancelHandler = null

  let camera = null
  let controls = null
  let renderer = null

  const init = (cam, ctrl, rend) => {
    camera = cam
    controls = ctrl
    renderer = rend
    setupEventListeners()
  }

  const getSphericalCoords = () => {
    if (!camera || !controls) return
    const offset = new Vector3()
    offset.copy(camera.position).sub(controls.target)
    sphericalRadius = offset.length()
    sphericalTheta = Math.atan2(offset.x, offset.z)
    sphericalPhi = Math.acos(Math.min(1, Math.max(-1, offset.y / sphericalRadius)))
  }

  const setSphericalCoords = (theta, phi, radius) => {
    if (!camera || !controls) return
    radius = Math.max(10, Math.min(400, radius))
    phi = Math.max(0.01, Math.min(Math.PI / 2, phi))

    camera.position.x = controls.target.x + radius * Math.sin(phi) * Math.sin(theta)
    camera.position.y = controls.target.y + radius * Math.cos(phi)
    camera.position.z = controls.target.z + radius * Math.sin(phi) * Math.cos(theta)

    camera.lookAt(controls.target)
  }

  const setupEventListeners = () => {
    if (!renderer) return

    mouseDownHandler = (event) => {
      event.preventDefault()
      event.stopImmediatePropagation()

      lastMouseX = event.clientX
      lastMouseY = event.clientY

      mouseStartInCanvas = true

      if (event.button === 0) {
        isLeftMouseDown = true
        getSphericalCoords()
      } else if (event.button === 1) {
        isMiddleMouseDown = true
        getSphericalCoords()
      } else if (event.button === 2) {
        isRightMouseDown = true
      }
    }
    renderer.domElement.addEventListener('mousedown', mouseDownHandler, { capture: true })

    mouseMoveHandler = (event) => {
      event.preventDefault()
      event.stopImmediatePropagation()

      if (!mouseStartInCanvas) {
        return
      }

      const deltaX = event.clientX - lastMouseX
      const deltaY = event.clientY - lastMouseY

      if (isLeftMouseDown) {
        const thetaDelta = deltaX * 0.005
        const phiDelta = deltaY * 0.005

        sphericalTheta += thetaDelta
        sphericalPhi += phiDelta

        setSphericalCoords(sphericalTheta, sphericalPhi, sphericalRadius)
      } else if (isMiddleMouseDown || isRightMouseDown) {
        const panSpeed = 0.002 * sphericalRadius

        const right = new Vector3()
        const up = new Vector3(0, 1, 0)
        const lookDir = new Vector3()
        lookDir.subVectors(controls.target, camera.position).normalize()
        right.crossVectors(up, lookDir).normalize()

        const panX = -deltaX * panSpeed
        const panZ = -deltaY * panSpeed

        camera.position.addScaledVector(right, panX)
        camera.position.addScaledVector(lookDir, panZ)
        controls.target.addScaledVector(right, panX)
        controls.target.addScaledVector(lookDir, panZ)
      }

      lastMouseX = event.clientX
      lastMouseY = event.clientY
    }
    renderer.domElement.addEventListener('mousemove', mouseMoveHandler, { capture: true })

    mouseUpHandler = (event) => {
      event.preventDefault()
      event.stopImmediatePropagation()

      if (event.button === 0) {
        isLeftMouseDown = false
      } else if (event.button === 1) {
        isMiddleMouseDown = false
      } else if (event.button === 2) {
        isRightMouseDown = false
      }
      mouseStartInCanvas = false
    }
    renderer.domElement.addEventListener('mouseup', mouseUpHandler, { capture: true })

    mouseLeaveHandler = (event) => {
      event.preventDefault()
      event.stopImmediatePropagation()
      isLeftMouseDown = false
      isMiddleMouseDown = false
      isRightMouseDown = false
      mouseStartInCanvas = false
    }
    renderer.domElement.addEventListener('mouseleave', mouseLeaveHandler, { capture: true })

    wheelHandler = (event) => {
      event.preventDefault()
      event.stopImmediatePropagation()

      getSphericalCoords()
      const zoomSpeed = sphericalRadius * 0.001
      const newRadius = sphericalRadius + event.deltaY * zoomSpeed

      setSphericalCoords(sphericalTheta, sphericalPhi, newRadius)
    }
    renderer.domElement.addEventListener('wheel', wheelHandler, { capture: true })

    handleGlobalMouseUp = (event) => {
      isLeftMouseDown = false
      isMiddleMouseDown = false
      isRightMouseDown = false
      mouseStartInCanvas = false
    }
    window.addEventListener('mouseup', handleGlobalMouseUp, { capture: true })
    
    // ========== 触摸事件处理 ==========
    
    touchStartHandler = (event) => {
      event.preventDefault()
      event.stopImmediatePropagation()
      
      touchStartInCanvas = true
      
      if (event.touches.length === 1) {
        // 单指触摸 - 旋转视角（模拟左键）
        isTouching = true
        lastMouseX = event.touches[0].clientX
        lastMouseY = event.touches[0].clientY
        getSphericalCoords()
      } else if (event.touches.length === 2) {
        // 双指触摸 - 缩放和移动
        isTouching = true
        const touch1 = event.touches[0]
        const touch2 = event.touches[1]
        lastTouchDistance = Math.hypot(touch2.clientX - touch1.clientX, touch2.clientY - touch1.clientY)
        lastTouchCenterX = (touch1.clientX + touch2.clientX) / 2
        lastTouchCenterY = (touch1.clientY + touch2.clientY) / 2
        lastMouseX = lastTouchCenterX
        lastMouseY = lastTouchCenterY
        getSphericalCoords()
      }
    }
    renderer.domElement.addEventListener('touchstart', touchStartHandler, { capture: true, passive: false })
    
    touchMoveHandler = (event) => {
      event.preventDefault()
      event.stopImmediatePropagation()
      
      if (!touchStartInCanvas || !isTouching) {
        return
      }
      
      if (event.touches.length === 1) {
        // 单指滑动 - 旋转视角
        const deltaX = event.touches[0].clientX - lastMouseX
        const deltaY = event.touches[0].clientY - lastMouseY
        
        const thetaDelta = deltaX * 0.005
        const phiDelta = deltaY * 0.005
        
        sphericalTheta += thetaDelta
        sphericalPhi += phiDelta
        
        setSphericalCoords(sphericalTheta, sphericalPhi, sphericalRadius)
        
        lastMouseX = event.touches[0].clientX
        lastMouseY = event.touches[0].clientY
      } else if (event.touches.length === 2) {
        // 双指操作 - 缩放 + 平移
        const touch1 = event.touches[0]
        const touch2 = event.touches[1]
        
        // 计算新的距离（用于缩放）
        const currentDistance = Math.hypot(touch2.clientX - touch1.clientX, touch2.clientY - touch1.clientY)
        const deltaDistance = currentDistance - lastTouchDistance
        
        // 计算新的中心点（用于平移）
        const currentCenterX = (touch1.clientX + touch2.clientX) / 2
        const currentCenterY = (touch1.clientY + touch2.clientY) / 2
        const deltaX = currentCenterX - lastTouchCenterX
        const deltaY = currentCenterY - lastTouchCenterY
        
        // 缩放
        getSphericalCoords()
        const zoomSpeed = sphericalRadius * 0.002
        const newRadius = sphericalRadius - deltaDistance * zoomSpeed
        setSphericalCoords(sphericalTheta, sphericalPhi, newRadius)
        
        // 平移（鼠标中键效果）
        const panSpeed = 0.002 * sphericalRadius
        const right = new Vector3()
        const up = new Vector3(0, 1, 0)
        const lookDir = new Vector3()
        lookDir.subVectors(controls.target, camera.position).normalize()
        right.crossVectors(up, lookDir).normalize()
        
        const panX = -deltaX * panSpeed
        const panZ = -deltaY * panSpeed
        
        camera.position.addScaledVector(right, panX)
        camera.position.addScaledVector(lookDir, panZ)
        controls.target.addScaledVector(right, panX)
        controls.target.addScaledVector(lookDir, panZ)
        
        lastTouchDistance = currentDistance
        lastTouchCenterX = currentCenterX
        lastTouchCenterY = currentCenterY
        lastMouseX = currentCenterX
        lastMouseY = currentCenterY
      }
    }
    renderer.domElement.addEventListener('touchmove', touchMoveHandler, { capture: true, passive: false })
    
    touchEndHandler = (event) => {
      event.preventDefault()
      event.stopImmediatePropagation()
      
      if (event.touches.length === 0) {
        isTouching = false
        touchStartInCanvas = false
      }
    }
    renderer.domElement.addEventListener('touchend', touchEndHandler, { capture: true, passive: false })
    
    touchCancelHandler = (event) => {
      event.preventDefault()
      event.stopImmediatePropagation()
      isTouching = false
      touchStartInCanvas = false
    }
    renderer.domElement.addEventListener('touchcancel', touchCancelHandler, { capture: true, passive: false })
  }

  const cleanupEventListeners = () => {
    if (renderer && renderer.domElement) {
      if (mouseDownHandler) {
        renderer.domElement.removeEventListener('mousedown', mouseDownHandler, { capture: true })
        mouseDownHandler = null
      }
      if (mouseMoveHandler) {
        renderer.domElement.removeEventListener('mousemove', mouseMoveHandler, { capture: true })
        mouseMoveHandler = null
      }
      if (mouseUpHandler) {
        renderer.domElement.removeEventListener('mouseup', mouseUpHandler, { capture: true })
        mouseUpHandler = null
      }
      if (mouseLeaveHandler) {
        renderer.domElement.removeEventListener('mouseleave', mouseLeaveHandler, { capture: true })
        mouseLeaveHandler = null
      }
      if (wheelHandler) {
        renderer.domElement.removeEventListener('wheel', wheelHandler, { capture: true })
        wheelHandler = null
      }
      // 清理触摸事件
      if (touchStartHandler) {
        renderer.domElement.removeEventListener('touchstart', touchStartHandler, { capture: true })
        touchStartHandler = null
      }
      if (touchMoveHandler) {
        renderer.domElement.removeEventListener('touchmove', touchMoveHandler, { capture: true })
        touchMoveHandler = null
      }
      if (touchEndHandler) {
        renderer.domElement.removeEventListener('touchend', touchEndHandler, { capture: true })
        touchEndHandler = null
      }
      if (touchCancelHandler) {
        renderer.domElement.removeEventListener('touchcancel', touchCancelHandler, { capture: true })
        touchCancelHandler = null
      }
    }

    if (handleGlobalMouseUp) {
      window.removeEventListener('mouseup', handleGlobalMouseUp, { capture: true })
      handleGlobalMouseUp = null
    }

    isLeftMouseDown = false
    isMiddleMouseDown = false
    isRightMouseDown = false
    mouseStartInCanvas = false
    isTouching = false
    touchStartInCanvas = false
  }

  return {
    init,
    cleanupEventListeners
  }
}