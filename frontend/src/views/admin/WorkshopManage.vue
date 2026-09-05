<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h2 class="page-title">车间工艺管理</h2>
        <p class="page-sub">管理 Neo4j 中的锂电 14 道工序节点、流转关系与车间工位分配</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" plain><el-icon><Refresh /></el-icon>同步 Neo4j 工序</el-button>
        <el-button type="primary"><el-icon><Plus /></el-icon>新增工序节点</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6"><div class="stat-card"><span class="stat-label">工序节点</span><span class="stat-value val-green">{{ nodeCount }}</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">流转关系</span><span class="stat-value val-cyan">{{ relCount }}</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">车间数量</span><span class="stat-value val-amber">{{ workshopCount }}</span></div></el-col>
      <el-col :span="6"><div class="stat-card"><span class="stat-label">关键工序</span><span class="stat-value val-red">{{ criticalCount }}</span></div></el-col>
    </el-row>

    <div class="content-card">
      <div class="toolbar">
        <el-select v-model="filterWorkshop" placeholder="按车间筛选" clearable style="width: 200px">
          <el-option v-for="w in workshops" :key="w" :label="w" :value="w" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索工序名称 / type" style="width: 280px" clearable :prefix-icon="Search" />
        <el-button type="primary" :icon="Search" style="margin-left:auto">查询</el-button>
      </div>

      <el-table :data="tableData" stripe style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="id" label="工序编号" width="110" />
        <el-table-column prop="name" label="工序名称" min-width="140">
          <template #default="{ row }">
            <el-tag v-if="row.critical" type="success" effect="light" round>关键</el-tag>
            <span style="margin-left: 6px">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型枚举" width="180">
          <template #default="{ row }"><code class="type-code">{{ row.type }}</code></template>
        </el-table-column>
        <el-table-column prop="workshop" label="归属车间" width="160" />
        <el-table-column prop="duration" label="耗时(h)" width="100" />
        <el-table-column prop="nextStep" label="下游工序" min-width="160" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default>
            <el-button size="small" link type="primary">图谱定位</el-button>
            <el-button size="small" link type="primary">编辑</el-button>
            <el-button size="small" link type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh, Plus, Search } from '@element-plus/icons-vue'
const loading = ref(false)
const keyword = ref('')
const filterWorkshop = ref('')
const nodeCount = ref(14)
const relCount = ref(13)
const workshopCount = ref(5)
const criticalCount = ref(7)
const workshops = ['原料预处理车间','涂布车间','电芯卷绕车间','化成车间','PACK封装车间','质量检测车间']
const tableData = ref([
  { id:'P001', name:'原料预处理', type:'PREPROCESS',  workshop:'原料预处理车间', duration:4, nextStep:'涂布(P002)',   critical:true  },
  { id:'P002', name:'涂布',       type:'COATING',     workshop:'涂布车间',        duration:2, nextStep:'辊压(P003)',   critical:true  },
  { id:'P003', name:'辊压',       type:'ROLLING',     workshop:'涂布车间',        duration:1, nextStep:'分切(P004)',   critical:false },
  { id:'P004', name:'分切',       type:'SLITTING',    workshop:'涂布车间',        duration:1, nextStep:'卷绕(P005)',   critical:false },
  { id:'P005', name:'卷绕',       type:'WINDING',     workshop:'电芯卷绕车间',    duration:3, nextStep:'入壳(P006)',   critical:true  },
  { id:'P008', name:'化成',       type:'FORMATION',   workshop:'化成车间',        duration:8, nextStep:'分容(P009)',   critical:true  },
  { id:'P011', name:'模组组装',   type:'MODULE_ASSEMBLY', workshop:'PACK封装车间', duration:4, nextStep:'电池包封装(P012)', critical:true },
])
onMounted(()=>{})
</script>

<style scoped>
.page-shell { padding: 4px 2px; }
.page-header { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom: 18px; }
.page-title { font-size: 22px; font-weight: 700; color:#0f172a; margin:0 0 4px; }
.page-sub { color:#64748b; font-size: 13px; margin:0; }
.page-actions { display:flex; gap:10px; }

.stat-row { margin-bottom: 18px; }
.stat-card { background: #fff; border-radius: 12px; padding: 16px 18px; box-shadow: 0 2px 8px rgba(15,23,42,.05); display:flex; flex-direction:column; gap: 6px; }
.stat-label { color:#64748b; font-size: 12px; }
.stat-value { font-size: 26px; font-weight: 800; }
.val-green { color:#0d9488; } .val-cyan  { color:#0891b2; }
.val-amber { color:#d97706; } .val-red   { color:#dc2626; }

.content-card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 2px 8px rgba(15,23,42,.05); }
.toolbar { display:flex; align-items:center; gap: 10px; margin-bottom: 12px; }
.type-code { background:#f1f5f9; padding: 2px 8px; border-radius: 4px; font-size: 12px; color:#475569; font-family: Menlo, Consolas, monospace; }
</style>
