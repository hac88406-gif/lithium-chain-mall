<template>
  <div class="table-list">
    <div class="search-bar flex gap-4 mb-6" v-if="showSearch">
      <input 
        v-model="searchKeyword" 
        type="text"
        :placeholder="searchPlaceholder"
        class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
        @keyup.enter="handleSearch"
      />
      <button 
        @click="handleSearch"
        class="px-6 py-2 bg-primary text-white rounded-lg hover:bg-secondary transition"
      >
        搜索
      </button>
      <button 
        @click="handleReset"
        class="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
      >
        重置
      </button>
    </div>
    
    <div class="table-wrapper bg-white rounded-xl shadow-sm overflow-hidden">
      <table class="w-full">
        <thead>
          <tr class="bg-gray-50">
            <th v-for="col in columns" :key="col.key" class="px-6 py-3 text-left text-sm font-medium text-gray-600">
              {{ col.label }}
            </th>
            <th class="px-6 py-3 text-left text-sm font-medium text-gray-600">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in data" :key="index" class="border-b border-gray-100 hover:bg-gray-50 transition">
            <td v-for="col in columns" :key="col.key" class="px-6 py-4 text-sm text-gray-700">
              <slot :name="`cell-${col.key}`" :item="item">
                {{ item[col.key] }}
              </slot>
            </td>
            <td class="px-6 py-4">
              <div class="flex gap-2">
                <button 
                  v-if="showEdit"
                  @click="$emit('edit', item)"
                  class="px-3 py-1 text-sm bg-blue-500 text-white rounded hover:bg-blue-600 transition"
                >
                  编辑
                </button>
                <button 
                  v-if="showDelete"
                  @click="$emit('delete', item)"
                  class="px-3 py-1 text-sm bg-red-500 text-white rounded hover:bg-red-600 transition"
                >
                  删除
                </button>
                <slot name="actions" :item="item"></slot>
              </div>
            </td>
          </tr>
          <tr v-if="data.length === 0">
            <td :colspan="columns.length + 1" class="px-6 py-12 text-center text-gray-400">
              {{ loading ? '加载中...' : '暂无数据' }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div class="pagination-bar flex items-center justify-between mt-4" v-if="total > 0">
      <div class="text-gray-500 text-sm">
        共 {{ total }} 条记录
      </div>
      <div class="flex items-center gap-2">
        <button 
          @click="handlePrev"
          :disabled="pagination.page <= 1"
          class="px-3 py-1 text-sm border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          上一页
        </button>
        <span class="text-sm text-gray-600">
          第 {{ pagination.page }} / {{ totalPages }} 页
        </span>
        <button 
          @click="handleNext"
          :disabled="pagination.page >= totalPages"
          class="px-3 py-1 text-sm border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          下一页
        </button>
        <select 
          v-model="pagination.size"
          @change="handleSizeChange"
          class="px-3 py-1 text-sm border border-gray-300 rounded outline-none"
        >
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  },
  columns: {
    type: Array,
    default: () => []
  },
  total: {
    type: Number,
    default: 0
  },
  loading: {
    type: Boolean,
    default: false
  },
  showSearch: {
    type: Boolean,
    default: true
  },
  searchPlaceholder: {
    type: String,
    default: '请输入搜索关键词'
  },
  showEdit: {
    type: Boolean,
    default: true
  },
  showDelete: {
    type: Boolean,
    default: true
  },
  pagination: {
    type: Object,
    default: () => ({
      page: 1,
      size: 10
    })
  }
})

const emit = defineEmits(['search', 'edit', 'delete', 'pagination-change'])

const searchKeyword = ref('')

const totalPages = computed(() => {
  return Math.ceil(props.total / props.pagination.size)
})

const handleSearch = () => {
  emit('search', {
    keyword: searchKeyword.value,
    page: 1,
    size: props.pagination.size
  })
}

const handleReset = () => {
  searchKeyword.value = ''
  emit('search', {
    keyword: '',
    page: 1,
    size: props.pagination.size
  })
}

const handlePrev = () => {
  if (props.pagination.page > 1) {
    emit('pagination-change', {
      page: props.pagination.page - 1,
      size: props.pagination.size
    })
  }
}

const handleNext = () => {
  if (props.pagination.page < totalPages.value) {
    emit('pagination-change', {
      page: props.pagination.page + 1,
      size: props.pagination.size
    })
  }
}

const handleSizeChange = () => {
  emit('pagination-change', {
    page: 1,
    size: props.pagination.size
  })
}

watch(searchKeyword, (val) => {
  if (!val) {
    handleSearch()
  }
})
</script>

<style scoped>
.table-list {
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.table-wrapper {
  overflow-x: auto;
}

table {
  border-collapse: collapse;
}

th {
  font-weight: 500;
}

tbody tr:last-child {
  border-bottom: none;
}
</style>