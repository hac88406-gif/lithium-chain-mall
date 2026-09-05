<template>
  <div class="permission-tree">
    <el-tree
      ref="treeRef"
      :data="treeData"
      :props="defaultProps"
      show-checkbox
      node-key="id"
      default-expand-all
      :expand-on-click-node="false"
      @check-change="handleCheckChange"
    />
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  // 权限树数据（异步加载，到达后需要手动回显选中态）
  treeData: {
    type: Array,
    default: () => []
  },
  // 已选中的权限ID列表（v-model）
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const treeRef = ref(null)

const defaultProps = {
  children: 'children',
  label: 'label'
}

/**
 * 勾选变化：el-tree 内置父子联动（勾父自动选子、子全选父自动勾），
 * 直接取全量选中 key 同步给父组件即可，无需手工维护联动。
 */
const handleCheckChange = () => {
  if (!treeRef.value) return
  const keys = treeRef.value.getCheckedKeys()
  emit('update:modelValue', keys)
}

/**
 * 树数据异步到达后回显已分配权限。
 * 注意：default-checked-keys 只在 el-tree 首次渲染时生效，
 * 数据后到必须用 setCheckedKeys 命令式回显，否则已勾选状态丢失。
 */
watch(() => props.treeData, async () => {
  await nextTick()
  if (treeRef.value && Array.isArray(props.modelValue)) {
    treeRef.value.setCheckedKeys(props.modelValue)
  }
}, { immediate: true })
</script>
