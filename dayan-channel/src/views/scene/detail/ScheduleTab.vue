<script setup lang="ts">
/**
 * 场景详情页 - 活动日程 tab（只读分页列表）。
 *
 * 手动 ref 分页调 pageSceneSchedules({ sceneCode, current, size })，
 * 表格列：活动日期 / 时段 / 报名人数 / 价格 / 日程状态（与 scene/schedule 列表页展示对齐），
 * 无操作列。接口失败 try/catch 降级为空列表。
 */
import { ref, watch } from 'vue'
import { pageSceneSchedules } from '@/api/scene'
import {
  SCENE_SCHEDULE_STATUS_OPTIONS,
  SceneScheduleStatus,
  type SceneSchedule
} from '@/types/scene'
import { formatMoney } from '@/utils/format'

const props = defineProps<{
  /** 场景编码（路由参数） */
  sceneCode: string
}>()

const loading = ref(false)
const tableData = ref<SceneSchedule[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)

async function loadPage() {
  if (!props.sceneCode) return
  loading.value = true
  try {
    const res = await pageSceneSchedules({
      sceneCode: props.sceneCode,
      current: current.value,
      size: size.value
    })
    tableData.value = res.records ?? []
    total.value = res.total ?? 0
  } catch (err) {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[scene/detail ScheduleTab] 加载活动日程失败（接口可能未实现）:', err)
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
  () => props.sceneCode,
  () => {
    current.value = 1
    loadPage()
  }
)

/** 日程状态 tag 类型（与 scene/schedule 列表页一致） */
function statusTagType(
  v?: number
): 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  switch (v) {
    case SceneScheduleStatus.AVAILABLE:
      return 'success'
    case SceneScheduleStatus.FULL:
      return 'warning'
    case SceneScheduleStatus.IN_PROGRESS:
      return 'primary'
    case SceneScheduleStatus.ENDED:
      return 'danger'
    case SceneScheduleStatus.CANCELLED:
      return 'info'
    default:
      return 'info'
  }
}

/** 日程状态文本（0 已取消 / 1 可预约 / 2 已约满 / 3 进行中 / 4 已结束） */
function statusText(v?: number): string {
  return SCENE_SCHEDULE_STATUS_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}
</script>

<template>
  <div class="schedule-tab">
    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="scheduleDate" label="活动日期" width="120" align="center" />
      <el-table-column label="时段" width="160" align="center">
        <template #default="{ row }">
          <span>{{ row.startTime || '--' }} ~ {{ row.endTime || '--' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="报名人数" width="110" align="center">
        <template #default="{ row }">
          <span>{{ row.currentPerson ?? 0 }} / {{ row.maxPerson ?? '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="priceOverride" label="价格" width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.priceOverride) }}</template>
      </el-table-column>
      <el-table-column label="日程状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.status != null" :type="statusTagType(row.status)" size="small">
            {{ statusText(row.status) }}
          </el-tag>
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
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
.schedule-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
