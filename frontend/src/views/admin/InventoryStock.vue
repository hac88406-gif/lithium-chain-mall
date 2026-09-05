<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">库存与仓储管理 <el-tag size="small" type="info" effect="light">进销存</el-tag></h2>
        <p class="page-sub">成品/半成品/原材料仓库库存、出入库流水、安全库存预警与调拨</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" plain><el-icon><Download /></el-icon>导出库存快照</el-button>
        <el-button type="primary"><el-icon><Plus /></el-icon>新建出入库单</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6"><div class="stat-card"><span class="stat-label">SKU 品种</span><span class="stat-value val-green">26</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">库存件数</span><span class="stat-value val-cyan">18,520</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">库存总值</span><span class="stat-value val-amber">¥682万</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">低于安全库存</span><span class="stat-value val-red">3</span></div></el-col>
    </el-row>

    <div class="content-card">
      <el-tabs v-model="tab">
        <el-tab-pane label="实时库存" name="stock">
          <div class="toolbar">
            <el-select v-model="warehouse" placeholder="仓库" clearable style="width: 200px">
              <el-option v-for="w in warehouses" :key="w" :label="w" :value="w" />
            </el-select>
            <el-select v-model="warn" placeholder="预警类型" clearable style="width: 180px">
              <el-option label="低于安全库存" value="low" />
              <el-option label="即将过期" value="expire" />
              <el-option label="呆滞 > 180天" value="dead" />
            </el-select>
            <el-input v-model="keyword" placeholder="搜索 SKU / 品名 / 批次" style="width: 320px" clearable />
            <el-button type="primary" style="margin-left:auto">查询</el-button>
          </div>

          <el-table :data="stockRows" stripe style="width:100%">
            <el-table-column prop="sku" label="SKU" width="170" />
            <el-table-column prop="name" label="品名" min-width="220" />
            <el-table-column prop="warehouse" label="仓库" width="140" />
            <el-table-column prop="batch" label="批次" width="130" />
            <el-table-column label="数量" width="130" align="right">
              <template #default="{ row }">
                <b>{{ row.qty }}</b> <span class="unit">{{ row.unit }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="safety" label="安全库存" width="110" align="right" />
            <el-table-column label="预警" width="110">
              <template #default="{ row }">
                <el-tag v-if="row.qty < row.safety" type="danger" size="small">库存不足</el-tag>
                <el-tag v-else-if="row.expire" type="warning" size="small">即将过期</el-tag>
                <el-tag v-else type="success" size="small" effect="plain">正常</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastIn" label="最近入库" width="120" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default>
                <el-button size="small" link type="primary">流水</el-button>
                <el-button size="small" link type="primary">入库</el-button>
                <el-button size="small" link type="primary">出库</el-button>
                <el-button size="small" link type="primary">调拨</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="出入库流水" name="flow">
          <div class="toolbar">
            <el-select v-model="flowType" placeholder="类型" clearable style="width: 160px">
              <el-option label="入库" value="in" /><el-option label="出库" value="out" /><el-option label="调拨" value="move" />
            </el-select>
            <el-input v-model="keyword" placeholder="单号 / SKU" style="width: 260px" clearable />
            <el-button type="primary" style="margin-left:auto">查询</el-button>
          </div>
          <el-table :data="flowRows" stripe style="width:100%">
            <el-table-column prop="no" label="单号" width="160" />
            <el-table-column prop="type" label="类型" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.type==='入库'?'success':(row.type==='出库'?'warning':'primary')">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sku" label="SKU" width="170" />
            <el-table-column prop="qty" label="数量" width="110" align="right" />
            <el-table-column prop="fromTo" label="往来方" min-width="180" />
            <el-table-column prop="warehouse" label="仓库" width="120" />
            <el-table-column prop="operator" label="操作人" width="100" />
            <el-table-column prop="time" label="时间" width="170" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus, Download } from '@element-plus/icons-vue'
const tab = ref('stock')
const warehouse = ref('')
const warn = ref('')
const keyword = ref('')
const flowType = ref('')
const warehouses = ['PACK成品仓(深圳)','半成品仓(宜春)','原材料仓A','原材料仓B','保税仓(盐田)','RMA退换仓']
const stockRows = ref([
  { sku:'LC-18650-48V-20A', name:'18650锂电池组 48V 20Ah',   warehouse:'PACK成品仓(深圳)', batch:'B260901-08',   qty:328,  unit:'件',  safety:200, lastIn:'2026-08-30' },
  { sku:'LC-21700-60V-30A', name:'21700锂电池组 60V 30Ah',  warehouse:'PACK成品仓(深圳)', batch:'B260901-12',   qty:125,  unit:'件',  safety:180, lastIn:'2026-08-28', expire:true },
  { sku:'ESM-5Kwh-LFP',    name:'储能模组 5KWh 磷酸铁锂',    warehouse:'PACK成品仓(深圳)', batch:'B260815-01',   qty:42,   unit:'台',  safety:60,  lastIn:'2026-08-15' },
  { sku:'CELL-21700-50E',  name:'21700 动力电芯 5000mAh',    warehouse:'原材料仓A',       batch:'R260825-A1',   qty:23800,unit:'颗',  safety:8000,lastIn:'2026-08-25' },
  { sku:'POW-48V-BMS-V3',  name:'48V BMS 管理板 v3 (16S)',   warehouse:'半成品仓(宜春)',   batch:'H260810-22',   qty:96,   unit:'片',  safety:150, lastIn:'2026-08-10' },
  { sku:'PP-1500W-Sine',   name:'便携电源 1500W 纯正玄波',   warehouse:'PACK成品仓(深圳)', batch:'B260901-03',   qty:216,  unit:'台',  safety:200, lastIn:'2026-08-31' },
])
const flowRows = ref([
  { no:'IO-20260901-032', type:'入库', sku:'LC-18650-48V-20A', qty:'+ 120 件',  fromTo:'自产 · PACK封装车间', warehouse:'PACK成品仓(深圳)', operator:'admin',  time:'2026-09-01 08:36:12' },
  { no:'IO-20260901-031', type:'出库', sku:'LC-21700-60V-30A', qty:'- 80 件',   fromTo:'销售出库 · 达达电动车',  warehouse:'PACK成品仓(深圳)', operator:'op-lisi',time:'2026-09-01 09:12:47' },
  { no:'MV-20260831-008', type:'调拨', sku:'CELL-21700-50E',   qty:'6000 颗',   fromTo:'原材料仓A → 半成品仓(宜春)', warehouse:'半成品仓(宜春)', operator:'op-wang',time:'2026-08-31 17:50:21' },
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
.unit { color:#94a3b8; font-size: 12px; margin-left: 4px; }
</style>
