<script setup lang="ts">
/**
 * 接口文档 - 右侧接口介绍区。
 * 展示 method/path/描述/请求头表/参数表 + 内嵌 ApiTester + 示例响应。
 */
import { ElMessage } from 'element-plus'
import ApiTester from './ApiTester.vue'
import type { ApiItem, ApiParam } from './apis'

const props = defineProps<{ api: ApiItem }>()

function methodTagType(method: string) {
  return ({ GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' } as const)[method as 'GET'] || 'info'
}

function locationText(loc: ApiParam['location']) {
  return { path: '路径', query: '查询', header: '请求头', body: '请求体' }[loc]
}

async function copyPath() {
  try {
    await navigator.clipboard.writeText(props.api.path)
    ElMessage.success('已复制路径')
  } catch {
    ElMessage.error('复制失败')
  }
}
</script>

<template>
  <div class="api-detail">
    <!-- 标题行 -->
    <div class="api-header">
      <el-tag :type="methodTagType(api.method)" size="default">{{ api.method }}</el-tag>
      <h2>{{ api.title }}</h2>
    </div>

    <!-- path 行 -->
    <div class="api-path">
      <code>{{ api.method }} {{ api.path }}</code>
      <el-button text @click="copyPath">
        <el-icon><CopyDocument /></el-icon>
        <span>复制路径</span>
      </el-button>
    </div>

    <!-- 描述 -->
    <div v-if="api.summary || api.description" class="api-desc">
      <p v-if="api.summary"><strong>{{ api.summary }}</strong></p>
      <p v-if="api.description" class="desc-text">{{ api.description }}</p>
    </div>

    <!-- 请求头表 -->
    <div v-if="api.headers && api.headers.length" class="api-section">
      <h3>请求头</h3>
      <el-table :data="api.headers" border stripe size="small">
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="type" label="类型" width="90" />
        <el-table-column label="必填" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.required ? 'danger' : 'info'">{{ row.required ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="200" />
      </el-table>
    </div>

    <!-- 请求参数表 -->
    <div v-if="api.params && api.params.length" class="api-section">
      <h3>请求参数</h3>
      <el-table :data="api.params" border stripe size="small">
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="位置" width="90" align="center">
          <template #default="{ row }">{{ locationText(row.location) }}</template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="90" />
        <el-table-column label="必填" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.required ? 'danger' : 'info'">{{ row.required ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="200" />
      </el-table>
    </div>

    <!-- 接口测试面板（内嵌） -->
    <ApiTester :api="api" />

    <!-- 示例响应 -->
    <div class="api-section">
      <h3>示例响应</h3>
      <pre class="code-block"><code>{{ api.responseExample }}</code></pre>
    </div>
  </div>
</template>

<style scoped lang="scss">
.api-detail {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.api-header {
  display: flex;
  align-items: center;
  gap: 12px;
  h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
  }
}
.api-path {
  display: flex;
  align-items: center;
  gap: 12px;
  code {
    background: var(--el-fill-color-light);
    padding: 6px 12px;
    border-radius: 4px;
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 14px;
  }
}
.api-desc {
  .desc-text {
    color: var(--el-text-color-secondary);
    margin: 4px 0 0 0;
    line-height: 1.7;
  }
}
.api-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    border-left: 3px solid var(--el-color-primary);
    padding-left: 8px;
  }
}
.code-block {
  background: var(--el-fill-color-darker);
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 0;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
