<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">财务对账中心 <el-tag size="small" type="danger" effect="light">高风险操作</el-tag></h2>
        <p class="page-sub">销售账单、采购应付、回款计划与供应商对账台账</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" plain><el-icon><Download /></el-icon>导出本月账单</el-button>
        <el-button type="primary"><el-icon><Plus /></el-icon>新建对账批次</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6"><div class="stat-card"><span class="stat-label">本月应收</span><span class="stat-value val-green">¥128.5万</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">本月实收</span><span class="stat-value val-cyan">¥96.2万</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">本月应付</span><span class="stat-value val-amber">¥74.8万</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">逾期应收</span><span class="stat-value val-red">¥8.6万</span></div></el-col>
    </el-row>

    <div class="content-card">
      <el-tabs v-model="tab">
        <el-tab-pane label="销售应收" name="ar">
          <div class="toolbar">
            <el-date-picker type="month" v-model="month" placeholder="对账月份" style="width: 200px" value-format="YYYY-MM" />
            <el-select v-model="status" placeholder="状态" clearable style="width: 160px">
              <el-option label="待开票" value="1" /><el-option label="已开票未收款" value="2" />
              <el-option label="部分收款" value="3" /><el-option label="已结清" value="4" />
              <el-option label="已逾期" value="5" />
            </el-select>
            <el-input v-model="keyword" placeholder="客户名 / 订单号" style="width: 280px" clearable />
            <el-button type="primary" style="margin-left:auto">查询</el-button>
          </div>
          <el-table :data="arRows" stripe style="width:100%">
            <el-table-column prop="billNo" label="账单号" width="150" />
            <el-table-column prop="customer" label="客户" min-width="180" />
            <el-table-column prop="orderNo" label="关联订单" width="150" />
            <el-table-column prop="amount" label="应收金额(元)" width="140" align="right" />
            <el-table-column prop="paid" label="已收金额(元)" width="140" align="right" />
            <el-table-column prop="dueDate" label="到期日" width="120" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status==='已逾期'?'danger':(row.status==='已结清'?'success':(row.status==='部分收款'?'warning':'primary'))">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default>
                <el-button size="small" link type="primary">账单详情</el-button>
                <el-button size="small" link type="primary">登记收款</el-button>
                <el-button size="small" link type="primary">开票</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="采购应付" name="ap">
          <div class="toolbar">
            <el-input v-model="keyword" placeholder="供应商 / 账单号" style="width: 320px" clearable />
            <el-button type="primary" style="margin-left:auto">查询</el-button>
          </div>
          <el-table :data="apRows" stripe style="width:100%">
            <el-table-column prop="billNo" label="账单号" width="150" />
            <el-table-column prop="supplier" label="供应商" min-width="220" />
            <el-table-column prop="amount" label="应付(元)" width="140" align="right" />
            <el-table-column prop="paid" label="已付(元)" width="140" align="right" />
            <el-table-column prop="dueDate" label="到期日" width="120" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status==='已结清'?'success':'warning'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default>
                <el-button size="small" link type="primary">详情</el-button>
                <el-button size="small" link type="primary">登记付款</el-button>
                <el-button size="small" link type="primary">收票</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus, Download } from '@element-plus/icons-vue'
const tab = ref('ar')
const month = ref('2026-09')
const status = ref('')
const keyword = ref('')
const arRows = ref([
  { billNo:'AR202609-0023', customer:'达达电动车有限公司', orderNo:'GN20260900-0128', amount:'48,680.00', paid:'48,680.00', dueDate:'2026-09-28', status:'已结清' },
  { billNo:'AR202609-0019', customer:'绿能储能科技(苏州)', orderNo:'GN20260900-0096', amount:'126,800.00', paid:'63,400.00', dueDate:'2026-09-25', status:'部分收款' },
  { billNo:'AR202608-0131', customer:'星航新能源车业',        orderNo:'GN20260828-0012', amount:'86,400.00', paid:'0.00',       dueDate:'2026-09-10', status:'已逾期' },
  { billNo:'AR202609-0041', customer:'华贸进出口公司',        orderNo:'GN20260900-0258', amount:'52,960.00', paid:'0.00',       dueDate:'2026-10-05', status:'已开票未收款' },
])
const apRows = ref([
  { billNo:'AP202609-0011', supplier:'宁德时代新能源材料（宜春）有限公司', amount:'286,400.00', paid:'0.00',       dueDate:'2026-09-30', status:'待付款' },
  { billNo:'AP202608-0285', supplier:'恩捷股份 · 隔膜事业部',               amount:'124,800.00', paid:'124,800.00', dueDate:'2026-08-30', status:'已结清' },
])
</script>

<style scoped>
.page-shell { padding: 4px 2px; }
.page-header { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom: 18px; }
.page-title { font-size: 22px; font-weight: 700; color:#0f172a; margin:0 0 4px; display:flex; align-items:center; gap: 8px; }
.page-sub { color:#64748b; font-size: 13px; margin:0; }
.page-actions { display:flex; gap:10px; }
.stat-row { margin-bottom: 18px; }
.stat-card { background:#fff; border-radius:12px; padding:16px 18px; box-shadow:0 2px 8px rgba(15,23,42,.05); display:flex; flex-direction:column; gap:6px; }
.stat-label { color:#64748b; font-size:12px; }
.stat-value { font-size: 26px; font-weight: 800; }
.val-green { color:#0d9488; } .val-cyan  { color:#0891b2; }
.val-amber { color:#d97706; } .val-red   { color:#dc2626; }
.content-card { background:#fff; border-radius:12px; padding:16px; box-shadow:0 2px 8px rgba(15,23,42,.05); }
.toolbar { display:flex; align-items:center; gap:10px; margin-bottom:12px; }
</style>
