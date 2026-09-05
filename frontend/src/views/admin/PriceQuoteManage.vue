<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">报价与价格管理 <el-tag size="small" type="success" effect="light">销售中心</el-tag></h2>
        <p class="page-sub">SKU 阶梯价、客户报价单、特价审批与历史价追溯</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" plain><el-icon><PriceTag /></el-icon>生成报价单</el-button>
        <el-button type="primary"><el-icon><Plus /></el-icon>新增价格政策</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6"><div class="stat-card"><span class="stat-label">在售 SKU</span><span class="stat-value val-green">26</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">待审批特价单</span><span class="stat-value val-amber">7</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">本月报价单</span><span class="stat-value val-cyan">54</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">均价变动(月)</span><span class="stat-value val-red">-1.8%</span></div></el-col>
    </el-row>

    <div class="content-card">
      <div class="toolbar">
        <el-select v-model="category" placeholder="类目" clearable style="width: 180px">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="level" placeholder="客户级别" clearable style="width: 160px">
          <el-option label="战略客户" value="S" />
          <el-option label="一级客户" value="A" />
          <el-option label="普通客户" value="B" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索 SKU / 商品名称" style="width: 280px" clearable />
        <el-button type="primary" style="margin-left:auto">查询</el-button>
      </div>

      <el-table :data="rows" stripe style="width: 100%">
        <el-table-column prop="sku" label="SKU" width="150" />
        <el-table-column prop="name" label="商品" min-width="220" />
        <el-table-column prop="category" label="类目" width="120" />
        <el-table-column label="基准价" width="110">
          <template #default="{ row }"><b>¥{{ row.basePrice }}</b></template>
        </el-table-column>
        <el-table-column label="阶梯价(批量)" min-width="260">
          <template #default="{ row }">
            <span class="tier">1-99 <b>¥{{ row.t1 }}</b></span>
            <el-divider direction="vertical" />
            <span class="tier">100-999 <b>¥{{ row.t2 }}</b></span>
            <el-divider direction="vertical" />
            <span class="tier">1000+ <b>¥{{ row.t3 }}</b></span>
          </template>
        </el-table-column>
        <el-table-column prop="moq" label="MOQ" width="90" />
        <el-table-column prop="effective" label="生效期" width="200" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status==='生效中'?'success':'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default>
            <el-button size="small" link type="primary">报价单</el-button>
            <el-button size="small" link type="primary">调价</el-button>
            <el-button size="small" link type="primary">历史价</el-button>
            <el-button size="small" link type="danger">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus, PriceTag } from '@element-plus/icons-vue'
const keyword=ref(''); const level=ref(''); const category=ref('')
const categories = ['锂电池组(48V)','锂电池组(60V)','动力电芯','储能模组','便携电源','BMS配件']
const rows = ref([
  { sku:'LC-18650-48V-20A', name:'18650锂电池组 48V 20Ah',     category:'锂电池组(48V)', basePrice:'1,899.00', t1:'1,899', t2:'1,820', t3:'1,750', moq:'1件',  effective:'2025-08-01 ~ 2025-12-31', status:'生效中' },
  { sku:'LC-21700-60V-30A', name:'21700锂电池组 60V 30Ah',    category:'锂电池组(60V)', basePrice:'2,899.00', t1:'2,899', t2:'2,780', t3:'2,680', moq:'1件',  effective:'2025-08-01 ~ 2025-12-31', status:'生效中' },
  { sku:'ESM-5Kwh-LFP',    name:'储能模组 5KWh 磷酸铁锂',      category:'储能模组',      basePrice:'5,299.00', t1:'5,299', t2:'5,099', t3:'4,899', moq:'10件', effective:'2025-07-15 ~ 2025-12-31', status:'生效中' },
  { sku:'PP-1500W-Sine',   name:'便携电源 1500W 纯正玄波',     category:'便携电源',      basePrice:'2,399.00', t1:'2,399', t2:'2,280', t3:'2,180', moq:'1件',  effective:'2025-08-01 ~ 2025-11-30', status:'待生效' },
  { sku:'POW-48V-BMS-V3',  name:'48V BMS 管理板 v3 (16S)',     category:'BMS配件',       basePrice:'268.00',   t1:'268',   t2:'255',   t2_:'245', t3:'245',   moq:'50件', effective:'2025-01-01 ~ 长期',         status:'生效中' },
  { sku:'CELL-21700-50E',  name:'21700 动力电芯 5000mAh',      category:'动力电芯',      basePrice:'18.80',    t1:'18.80', t2:'18.00', t3:'17.20', moq:'200件',effective:'2025-06-01 ~ 2025-12-31', status:'生效中' },
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
.tier { color:#475569; font-size:12px; }
.tier b { color:#0f766e; font-size: 13px; }
</style>
