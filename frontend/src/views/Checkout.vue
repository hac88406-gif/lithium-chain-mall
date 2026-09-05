<template>
  <div class="checkout-page py-12">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="page-header mb-8">
        <h1 class="page-title">确认订单</h1>
      </div>

      <div v-if="isLoading" class="text-center py-20">
        <div class="inline-block w-10 h-10 border-4 border-green-500 border-t-transparent rounded-full animate-spin"></div>
      </div>

      <template v-else>
        <div class="address-section card mb-6">
          <div class="section-header">
            <h2 class="section-title">收货地址</h2>
            <button class="btn-link" @click="showAddressForm = true">+ 添加新地址</button>
          </div>
          <div class="address-list">
            <div 
              v-for="addr in addresses" 
              :key="addr.id" 
              class="address-item"
              :class="{ selected: selectedAddressId === addr.id }"
              @click="selectAddress(addr.id)"
            >
              <div class="address-main">
                <span class="addr-name">{{ addr.name }}</span>
                <span class="addr-phone">{{ addr.phone }}</span>
                <span v-if="addr.isDefault" class="addr-badge">默认</span>
              </div>
              <div class="addr-detail">
                {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}
              </div>
              <div class="addr-actions" @click.stop>
                <button @click="editAddress(addr)">编辑</button>
                <button @click="deleteAddress(addr.id)">删除</button>
              </div>
              <div v-if="selectedAddressId === addr.id" class="selected-mark">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
                </svg>
              </div>
            </div>
            <div v-if="addresses.length === 0" class="no-address">
              <p>还没有收货地址，请添加一个</p>
            </div>
          </div>
        </div>

        <div class="products-section card mb-6">
          <div class="section-header">
            <h2 class="section-title">商品清单</h2>
          </div>
          <div class="product-list">
            <div v-for="item in orderItems" :key="item.id" class="product-row">
              <div class="product-img">
                <img :src="getImageUrl(item.image)" :alt="item.name" />
              </div>
              <div class="product-info">
                <h4 class="product-name">{{ item.name }}</h4>
              </div>
              <div class="product-price">¥{{ item.price }}</div>
              <div class="product-qty">×{{ item.quantity }}</div>
              <div class="product-subtotal">¥{{ (parseFloat(item.price) * item.quantity).toFixed(2) }}</div>
            </div>
          </div>
        </div>

        <div class="remark-section card mb-6">
          <div class="section-header">
            <h2 class="section-title">订单备注</h2>
          </div>
          <textarea 
            v-model="remark" 
            class="remark-input" 
            placeholder="选填，请输入您的备注信息"
            rows="3"
          ></textarea>
        </div>

        <div class="order-summary card">
          <div class="summary-row">
            <span>商品金额</span>
            <span>¥{{ subtotal.toFixed(2) }}</span>
          </div>
          <div class="summary-row">
            <span>运费</span>
            <span class="free-shipping">免运费</span>
          </div>
          <div class="summary-row total">
            <span>应付金额</span>
            <span class="total-amount">¥{{ totalAmount.toFixed(2) }}</span>
          </div>
          <div class="summary-actions">
            <button class="btn-back" @click="goBack">返回购物车</button>
            <button 
              class="btn-submit" 
              :disabled="!canSubmit || submitting"
              @click="submitOrder"
            >
              {{ submitting ? '处理中...' : `提交订单并付款 ¥${totalAmount.toFixed(2)}` }}
            </button>
          </div>
        </div>
      </template>
    </div>

    <div v-if="showAddressForm" class="modal-overlay" @click.self="closeAddressForm">
      <div class="modal">
        <h3 class="modal-title">{{ editingAddress ? '编辑地址' : '添加地址' }}</h3>
        <form @submit.prevent="saveAddress">
          <div class="form-group">
            <label>收货人</label>
            <input type="text" v-model="addressForm.name" required />
          </div>
          <div class="form-group">
            <label>手机号</label>
            <input type="tel" v-model="addressForm.phone" required />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>省份</label>
              <input type="text" v-model="addressForm.province" />
            </div>
            <div class="form-group">
              <label>城市</label>
              <input type="text" v-model="addressForm.city" />
            </div>
          </div>
          <div class="form-group">
            <label>详细地址</label>
            <textarea v-model="addressForm.detail" rows="2" required></textarea>
          </div>
          <div class="form-checkbox">
            <input type="checkbox" v-model="addressForm.isDefault" :true-value="1" :false-value="0" />
            <label>设为默认地址</label>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn-cancel" @click="closeAddressForm">取消</button>
            <button type="submit" class="btn-confirm">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { api } from '../api/index'

const router = useRouter()
const route = useRoute()

const isLoading = ref(true)
const addresses = ref([])
const orderItems = ref([])
const selectedAddressId = ref(null)
const remark = ref('')
const submitting = ref(false)
const showAddressForm = ref(false)
const editingAddress = ref(null)
const addressForm = ref({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: 0
})

const subtotal = computed(() => {
  return orderItems.value.reduce((sum, item) => sum + parseFloat(item.price) * item.quantity, 0)
})

const totalAmount = computed(() => subtotal.value)

const canSubmit = computed(() => {
  return selectedAddressId.value !== null && orderItems.value.length > 0
})

const getImageUrl = (path) => {
  if (!path) return 'https://via.placeholder.com/100x100?text=No+Image'
  if (path.startsWith('http')) return path
  return path
}

const loadData = async () => {
  isLoading.value = true
  try {
    const idsParam = route.query.ids
    const ids = idsParam ? idsParam.split(',').map(Number) : []
    
    // 两个请求并行，任一请求超时（15s）整体 reject，防止页面永久 loading
    const timeout = (p, label) => Promise.race([
      p,
      new Promise((_, rej) => setTimeout(() => rej(new Error(`${label} 请求超时，请检查网络`)), 15000))
    ])
    const [cartRes, addrRes] = await Promise.all([
      timeout(api.getCartList(), '购物车'),
      timeout(api.getAddresses(), '地址列表')
    ])

    if (cartRes && cartRes.code === 200) {
      const allItems = cartRes.data || []
      orderItems.value = ids.length > 0 
        ? allItems.filter(item => ids.includes(item.id))
        : allItems
    } else if (cartRes && cartRes.code !== 200) {
      console.warn('购物车数据异常:', cartRes.message)
    }

    if (addrRes && addrRes.code === 200) {
      addresses.value = addrRes.data || []
      if (selectedAddressId.value === null && addresses.value.length > 0) {
        const defaultAddr = addresses.value.find(a => a.isDefault === 1)
        selectedAddressId.value = defaultAddr ? defaultAddr.id : addresses.value[0].id
      }
    }
  } catch (e) {
    console.error('Failed to load checkout data:', e)
    const msg = e?.message || '加载数据失败，请重试'
    alert(msg)
  } finally {
    isLoading.value = false
  }
}

const selectAddress = (id) => {
  selectedAddressId.value = id
}

const openAddressForm = () => {
  editingAddress.value = null
  addressForm.value = {
    name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0
  }
  showAddressForm.value = true
}

const editAddress = (addr) => {
  editingAddress.value = addr
  addressForm.value = { ...addr }
  showAddressForm.value = true
}

const closeAddressForm = () => {
  showAddressForm.value = false
  editingAddress.value = null
}

const saveAddress = async () => {
  try {
    let res
    if (editingAddress.value) {
      res = await api.updateAddress(editingAddress.value.id, addressForm.value)
    } else {
      res = await api.addAddress(addressForm.value)
    }
    
    if (res.code === 200) {
      closeAddressForm()
      loadData()
    } else {
      alert(res.message || '保存失败')
    }
  } catch (e) {
    console.error('Failed to save address:', e)
    alert('保存失败，请重试')
  }
}

const deleteAddress = async (id) => {
  if (!confirm('确定要删除该地址吗？')) return
  try {
    const res = await api.deleteAddress(id)
    if (res.code === 200) {
      if (selectedAddressId.value === id) {
        selectedAddressId.value = null
      }
      loadData()
    }
  } catch (e) {
    console.error('Failed to delete address:', e)
  }
}

const submitOrder = async () => {
  if (!canSubmit.value) {
    alert('请选择收货地址')
    return
  }

  if (!confirm(`确认提交订单并立即付款？应付金额：¥${totalAmount.value.toFixed(2)}`)) return

  submitting.value = true
  try {
    const items = orderItems.value.map(item => ({
      productId: item.productId,
      quantity: item.quantity,
      price: item.price
    }))

    // 幂等令牌：下单前先获取，防止重复提交
    const tokenRes = await api.getIdempotentToken()
    if (tokenRes.code !== 200 || !tokenRes.data) {
      alert('获取下单令牌失败，请重试')
      return
    }

    // 第 1 步：创建订单（pending 状态）
    const createRes = await api.createOrder({
      items,
      addressId: selectedAddressId.value,
      remark: remark.value
    }, { headers: { 'Idempotent-Token': tokenRes.data } })

    if (createRes.code !== 200) {
      alert(createRes.message || '下单失败')
      return
    }

    const orderId = createRes.data?.id ?? createRes.data
    if (!orderId) {
      alert('下单成功但未获取到订单号，请到我的订单完成付款')
      router.push('/my-orders')
      return
    }

    // 第 2 步：发起预下单（真实支付打通），返回支付信息，订单仍为 pending
    const payRes = await api.payOrder(orderId)
    if (payRes.code !== 200) {
      // 创建成功但付款未发起，让用户去"我的订单"手动付款
      alert(`订单已创建，但支付未发起（${payRes.message || '未知原因'}）。请到"我的订单"完成付款。`)
      router.push('/my-orders')
      return
    }
    const payInfo = payRes.data || {}
    if (!payInfo.paymentNo) {
      alert('支付流水创建失败，请到"我的订单"完成付款')
      router.push('/my-orders')
      return
    }
    // 第 3 步：模拟用户在支付网关"支付成功"（本地演示，走完整验签回调流程）
    if (!confirm(`跳转支付网关，应付金额 ¥${payInfo.amount ?? totalAmount.value.toFixed(2)}。\n确认已通过模拟网关支付成功？`)) {
      router.push('/my-orders')
      return
    }
    const simRes = await api.simulatePay(payInfo.paymentNo)
    if (simRes.code !== 200) {
      alert(simRes.message || '支付未完成，请到"我的订单"查看')
      router.push('/my-orders')
      return
    }

    // 第 4 步：清空购物车，跳转
    await api.clearCart()
    alert('下单并付款成功！')
    router.push('/my-orders')
  } catch (e) {
    console.error('Failed to submit order:', e)
    const msg = e?.response?.data?.message || '下单失败，请重试'
    alert(msg)
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.push('/cart')
}

onMounted(() => {
  const token = localStorage.getItem('token')
  if (!token) {
    alert('请先登录')
    router.push('/')
    return
  }
  loadData()
})
</script>

<style scoped>
.checkout-page {
  background: #f9fafb;
  min-height: calc(100vh - 64px);
}

.page-header {
  text-align: center;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
}

.card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1f2937;
}

.btn-link {
  background: none;
  border: none;
  color: #059669;
  cursor: pointer;
  font-size: 0.95rem;
}

.btn-link:hover {
  text-decoration: underline;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.address-item {
  position: relative;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.address-item:hover {
  border-color: #059669;
}

.address-item.selected {
  border-color: #059669;
  background: #f0fdf4;
}

.address-main {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.addr-name {
  font-weight: 600;
  color: #1f2937;
  font-size: 1rem;
}

.addr-phone {
  color: #6b7280;
}

.addr-badge {
  background: #059669;
  color: white;
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 4px;
}

.addr-detail {
  color: #6b7280;
  font-size: 0.95rem;
}

.addr-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.addr-actions button {
  background: none;
  border: none;
  color: #059669;
  cursor: pointer;
  font-size: 0.85rem;
}

.addr-actions button:last-child {
  color: #ef4444;
}

.selected-mark {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 24px;
  height: 24px;
  background: #059669;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.no-address {
  text-align: center;
  padding: 24px;
  color: #9ca3af;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-row {
  display: grid;
  grid-template-columns: 80px 1fr 100px 60px 100px;
  gap: 16px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.product-row:last-child {
  border-bottom: none;
}

.product-img {
  width: 80px;
  height: 80px;
  border-radius: 10px;
  overflow: hidden;
}

.product-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-name {
  font-size: 0.95rem;
  color: #1f2937;
  font-weight: 500;
}

.product-price {
  color: #059669;
  font-weight: 600;
  text-align: center;
}

.product-qty {
  text-align: center;
  color: #6b7280;
}

.product-subtotal {
  color: #dc2626;
  font-weight: 600;
  text-align: center;
}

.remark-input {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 12px;
  font-size: 0.95rem;
  resize: vertical;
  box-sizing: border-box;
}

.remark-input:focus {
  outline: none;
  border-color: #059669;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  color: #374151;
}

.summary-row.total {
  border-top: 1px solid #e5e7eb;
  margin-top: 12px;
  padding-top: 16px;
  font-size: 1.1rem;
}

.total-amount {
  color: #dc2626;
  font-size: 1.5rem;
  font-weight: 700;
}

.free-shipping {
  color: #059669;
}

.summary-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  gap: 12px;
}

.btn-back {
  background: white;
  color: #6b7280;
  border: 1px solid #d1d5db;
  padding: 12px 32px;
  border-radius: 10px;
  font-size: 1rem;
  cursor: pointer;
}

.btn-back:hover {
  background: #f9fafb;
}

.btn-submit {
  flex: 1;
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  color: white;
  border: none;
  padding: 12px 32px;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(5, 150, 105, 0.3);
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 500px;
}

.modal-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #374151;
  font-size: 0.9rem;
}

.form-group input,
.form-group textarea {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 0.95rem;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #059669;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn-cancel {
  background: white;
  color: #6b7280;
  border: 1px solid #d1d5db;
  padding: 10px 24px;
  border-radius: 8px;
  cursor: pointer;
}

.btn-confirm {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
}

@media (max-width: 768px) {
  .product-row {
    grid-template-columns: 60px 1fr;
    grid-template-rows: auto auto;
    gap: 8px;
  }
  
  .product-img {
    grid-row: span 2;
  }
  
  .product-price, .product-qty, .product-subtotal {
    grid-column: 2;
    text-align: left;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .summary-actions {
    flex-direction: column;
  }
}
</style>