<script setup lang="ts">
/**
 * 客户详情页 - 服务记录 tab（只读分页列表）。
 *
 * 手动 ref 分页调 pageServiceSessions({ clientCode, current, size })，
 * 表格列：会话编码 / 服务类型（SERVICE_TYPE_OPTIONS）/ 标题 / 状态（SESSION_STATUS_OPTIONS）/
 * 受理时间，无操作列。接口失败 try/catch 降级为空列表。
 */
import { ref, watch } from 'vue'
import { pageServiceSessions } from '@/api/service'
import { SERVICE_TYPE_OPTIONS, SESSION_STATUS_OPTIONS, type ServiceSession } from '@/types/service'
import { formatDateTime, statusTagType } from '@/utils/format'

const props = defineProps<{
  /** 客户编码（路由参数） */
  clientCode: string
}>()

const loading = ref(false)
const tableData = ref<ServiceSession[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)

async function loadPage() {
  if (!props.clientCode) return
  loading.value = true
  try {
    const res = await pageServiceSessions({
      clientCode: props.clientCode,
      current: current.value,
      size: size.value
    })
    tableData.value = res.records ?? []
    total.value = res.total ?? 0
  } catch (err) {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[client/detail ServiceTab] 加载服务记录失败（接口可能未实现）:', err)
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
  () => props.clientCode,
  () => {
    current.value = 1
    loadPage()
  }
)

/** 服务类型文本（1 电话关怀 / 2 上门探访 / 3 陪同就医 / 4 紧急救援） */
function serviceTypeText(v?: number): string {
  return SERVICE_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}

/** 会话状态文本（0-6 七主状态） */
function sessionStatusText(v?: number): string {
  return SESSION_STATUS_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}
</script>

<template>
  <div class="service-tab">
    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="sessionCode" label="会话编码" min-width="150" show-overflow-tooltip />
      <el-table-column label="服务类型" width="110" align="center">
        <template #default="{ row }">{{ serviceTypeText(row.serviceType) }}</template>
      </el-table-column>
      <el-table-column prop="serviceTitle" label="标题" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.sessionStatus != null" :type="statusTagType(row.sessionStatus)" size="small">
            {{ sessionStatusText(row.sessionStatus) }}
          </el-tag>
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column prop="acceptTime" label="受理时间" min-width="160" align="center">
        <template #default="{ row }">{{ formatDateTime(row.acceptTime) }}</template>
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
.service-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
