<script setup lang="ts">
/**
 * 接口文档 - 左侧目录树。
 * el-menu 渲染 4 分组（可折叠，unique-opened）+ 组内接口项。
 * 点击接口项 emit update:modelValue（apiId）。不加 router 属性（页内切换非路由跳转）。
 */
import type { ApiGroup } from './apis'

defineProps<{
  groups: ApiGroup[]
  modelValue: string
}>()
const emit = defineEmits<{
  'update:modelValue': [apiId: string]
}>()

function methodTagType(method: string) {
  return ({ GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' } as const)[method as 'GET'] || 'info'
}

function handleSelect(index: string) {
  emit('update:modelValue', index)
}
</script>

<template>
  <div class="api-catalog">
    <div class="catalog-header">
      <span>接口文档</span>
      <el-tag size="small" type="info">v1 预览</el-tag>
    </div>
    <el-menu
      :default-active="modelValue"
      :default-openeds="['auth']"
      unique-opened
      @select="handleSelect"
    >
      <el-sub-menu v-for="group in groups" :key="group.id" :index="group.id">
        <template #title>
          <span>{{ group.title }}</span>
          <el-tag size="small" type="info" effect="plain" class="group-count">{{ group.apis.length }}</el-tag>
        </template>
        <el-menu-item v-for="api in group.apis" :key="api.id" :index="api.id">
          <span class="api-title">{{ api.title }}</span>
          <el-tag size="small" :type="methodTagType(api.method)" class="api-method">{{ api.method }}</el-tag>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<style scoped lang="scss">
.api-catalog {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
}
.catalog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  font-weight: 600;
  font-size: 15px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.group-count {
  margin-left: 8px;
}
:deep(.el-menu) {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}
:deep(.el-menu-item) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.api-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.api-method {
  margin-left: 8px;
  flex-shrink: 0;
}
</style>
