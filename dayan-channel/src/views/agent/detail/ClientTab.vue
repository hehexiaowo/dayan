<script setup lang="ts">
/**
 * 代理人详情页 - 客户绑定 tab（只读分页列表）。
 *
 * 手动 ref 分页调 pageAgentClientRels({ agentCode, current, size })，
 * 表格列：客户编码 / 绑定类型（BIND_TYPE_OPTIONS）/ 绑定时间 / 状态，无操作列。
 * 接口失败 try/catch 降级为空列表。
 */
import { ref, watch } from 'vue'
import { pageAgentClientRels } from '@/api/agent'
import { BIND_TYPE_OPTIONS, type AgentClientRel } from '@/types/agent'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 代理人编码（路由参数） */
  agentCode: string
}>()

const loading = ref(false)
const tableData = ref<AgentClientRel[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)

async function loadPage() {
  if (!props.agentCode) return
  loading.value = true
  try {
    const res = await pageAgentClientRels({
      agentCode: props.agentCode,
      current: current.value,
      size: size.value
    })
    tableData.value = res.records ?? []
    total.value = res.total ?? 0
  } catch (err) {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[agent/detail ClientTab] 加载客户绑定列表失败（接口可能未实现）:', err)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

loadPage()

/** 翻页 */
function handlePageChange(page: number) {
  current.value = page
  loadPage()
}

/** 每页条数变化 */
function handleSizeChange(s: number) {
  size.value = s
  current.value = 1
  loadPage()
}

// 路由参数变化（同一组件实例复用）时回到第 1 页重查
watch(
  () => props.agentCode,
  () => {
    current.value = 1
    loadPage()
  }
)

/** 绑定类型文本（1 主动绑定 / 2 邀请绑定） */
function bindTypeText(v?: number): string {
  return BIND_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}

/** 绑定状态文本（对齐后端 agent_client_rel：1 服务中 / 0 已解绑） */
function relStatusText(v?: number): string {
  if (v == null) return '--'
  return v === 1 ? '服务中' : '已解绑'
}

/** 绑定状态 tag 类型 */
function relStatusTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}
</script>

<template>
  <div class="client-tab">
    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="clientCode" label="客户编码" min-width="160" show-overflow-tooltip />
      <el-table-column label="绑定类型" width="120" align="center">
        <template #default="{ row }">{{ bindTypeText(row.bindType) }}</template>
      </el-table-column>
      <el-table-column prop="bindTime" label="绑定时间" min-width="160" align="center">
        <template #default="{ row }">{{ formatDateTime(row.bindTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.status != null" :type="relStatusTagType(row.status)" size="small">
            {{ relStatusText(row.status) }}
          </el-tag>
          <span v-else>--</span>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无数据" />
      </template>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        :current-page="current"
        :page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.client-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
