<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">供应商管理 <el-tag size="small" type="warning" effect="light">采购中心</el-tag></h2>
        <p class="page-sub">锂电上游正极材料、电解液、隔膜、铜箔等核心供应商准入、评级与合同管理</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" plain><el-icon><Download /></el-icon>导出供应商清单</el-button>
        <el-button type="primary"><el-icon><Plus /></el-icon>新增供应商</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6"><div class="stat-card"><span class="stat-label">签约供应商</span><span class="stat-value val-green">86</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">A 级战略</span><span class="stat-value val-cyan">12</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">待审核准入</span><span class="stat-value val-amber">5</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">本月对账中</span><span class="stat-value val-red">8</span></div></el-col>
    </el-row>

    <div class="content-card">
      <div class="toolbar">
        <el-select v-model="level" placeholder="供应商评级" clearable style="width: 160px">
          <el-option label="A 战略" value="A" /><el-option label="B 合作" value="B" />
          <el-option label="C 普通" value="C" /><el-option label="D 淘汰" value="D" />
        </el-select>
        <el-select v-model="category" placeholder="供应品类" clearable style="width: 180px">
          <el-option v-for="c in ['正极材料','负极材料','电解液','隔膜','铜箔','铝壳','结构件','设备']" :key="c" :label="c" :value="c" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索供应商名 / 联系人 / 统一社会信用代码" style="width: 320px" clearable />
        <el-button type="primary" style="margin-left:auto">查询</el-button>
      </div>

      <el-table :data="rows" stripe style="width: 100%">
        <el-table-column prop="code" label="编号" width="110" />
        <el-table-column prop="name" label="供应商名称" min-width="220" />
        <el-table-column prop="category" label="主营品类" width="120" />
        <el-table-column label="评级" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.level==='A'" type="success" round effect="dark">A</el-tag>
            <el-tag v-else-if="row.level==='B'" type="primary" round>B</el-tag>
            <el-tag v-else-if="row.level==='C'" type="info" round>C</el-tag>
            <el-tag v-else type="danger" round>D</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contact" label="联系人" width="110" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="region" label="所在地区" width="120" />
        <el-table-column prop="signDate" label="签约日期" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status==='合作中'?'success':(row.status==='准入审核'?'warning':'info')">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default>
            <el-button size="small" link type="primary">详情</el-button>
            <el-button size="small" link type="primary">报价</el-button>
            <el-button size="small" link type="primary">编辑</el-button>
            <el-button size="small" link type="danger">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus, Download } from '@element-plus/icons-vue'
const keyword=ref(''); const level=ref(''); const category=ref('')
const rows = ref([
  { code:'SUP-0001', name:'宁德时代新能源材料（宜春）有限公司', category:'正极材料',   level:'A', contact:'张经理', phone:'138****8801', region:'江西宜春', signDate:'2024-06-12', status:'合作中' },
  { code:'SUP-0007', name:'恩捷股份 · 隔膜事业部',        category:'隔膜',       level:'A', contact:'李主任', phone:'139****2104', region:'云南玉溪', signDate:'2023-11-03', status:'合作中' },
  { code:'SUP-0015', name:'天赐材料（九江）基地',          category:'电解液',     level:'B', contact:'王先生', phone:'136****0912', region:'江西九江', signDate:'2025-01-09', status:'合作中' },
  { code:'SUP-0028', name:'铜箔科技有限公司',                category:'铜箔',       level:'C', contact:'赵小姐', phone:'135****8776', region:'广东深圳', signDate:'2025-03-22', status:'准入审核' },
  { code:'SUP-0044', name:'嘉元科技股份',                   category:'铜箔',       level:'B', contact:'陈工',   phone:'137****3421', region:'广东梅州', signDate:'2024-09-15', status:'合作中' },
  { code:'SUP-0052', name:'科达利精密结构件',               category:'结构件',     level:'B', contact:'周总',   phone:'138****6631', region:'江苏苏州', signDate:'2024-08-20', status:'合作中' },
  { code:'SUP-0063', name:'待审核 · 某新供应商 XX',          category:'设备',       level:'D', contact:'—',      phone:'—',           region:'—',          signDate:'—',           status:'准入审核' },
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
