<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageSceneSchedules } from '@/api/scene'
import {
  SCENE_SCHEDULE_STATUS_OPTIONS,
  SceneScheduleStatus,
  type SceneSchedule,
  type SceneScheduleQuery
} from '@/types/scene'
import { formatDateTime, formatMoney } from '@/utils/format'

/**
 * 场景管理页（业务运营目录）。
 *
 * 管理本渠道所有场景活动日程（scene_schedule）的记录+流转：
 * - 搜索栏：场景编码 / 日程状态 / 日程日期范围；
 * - el-table：sceneCode / sceneName / scheduleDate / 时段 / 报名人数 / 价格 / 状态(5态tag) / 操作(详情)；
 * - 详情：el-dialog + el-descriptions 结构化弹窗展示行数据全字段
 *   （对齐 order-manage 详情模式；日程字段与列表 VO 一致，不再单独请求详情接口）。
 *
 * 数据隔离：后端 ChannelSceneScheduleController 通过 channel_config_scene → scene_code
 * 间接过滤，只返回本渠道已配置场景的日程。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  SceneSchedule,
  SceneScheduleQuery
>(
  { page: pageSceneSchedules },
  {
    initialQuery: {
      sceneCode: '',
      status: undefined,
      scheduleDateStart: '',
      scheduleDateEnd: ''
    }
  }
)

/** 日期范围（el-date-picker type=daterange 绑定用） */
const dateRange = ref<[string, string] | null>(null)

function handleDateRangeChange(val: [string, string] | null) {
  if (val) {
    query.scheduleDateStart = val[0]
    query.scheduleDateEnd = val[1]
  } else {
    query.scheduleDateStart = ''
    query.scheduleDateEnd = ''
  }
}

function handleReset() {
  query.sceneCode = ''
  query.status = undefined
  query.scheduleDateStart = ''
  query.scheduleDateEnd = ''
  dateRange.value = null
  handleSearch()
}

function statusTagType(v?: number): 'success' | 'warning' | 'info' | 'danger' | 'primary' {
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

function statusText(v?: number) {
  const opt = SCENE_SCHEDULE_STATUS_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

// ==================== 查看详情（el-dialog + el-descriptions） ====================

const detailVisible = ref(false)
const currentRow = ref<SceneSchedule | null>(null)

/** 打开详情弹窗：展示行数据全字段 */
function openDetail(row: SceneSchedule) {
  currentRow.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadPage().catch((err) => {
    console.warn('[scene-schedule] 加载场景日程列表失败:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input v-model="query.sceneCode" placeholder="场景编码" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-select v-model="query.status" placeholder="日程状态" clearable style="width: 140px">
          <el-option v-for="o in SCENE_SCHEDULE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="handleDateRangeChange"
        />
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">场景活动日程列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="sceneCode" label="场景编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="sceneName" label="场景名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="scheduleDate" label="活动日期" width="120" align="center" />
        <el-table-column label="时段" width="160" align="center" show-overflow-tooltip>
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
        <el-table-column prop="status" label="日程状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status !== undefined && row.status !== null" :type="statusTagType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          :current-page="query.current"
          :page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="场景日程详情" width="720px">
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="场景编码">{{ currentRow.sceneCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="场景名称">{{ currentRow.sceneName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="活动日期">{{ currentRow.scheduleDate || '--' }}</el-descriptions-item>
        <el-descriptions-item label="日程状态">
          <el-tag
            v-if="currentRow.status !== undefined && currentRow.status !== null"
            :type="statusTagType(currentRow.status)"
          >
            {{ statusText(currentRow.status) }}
          </el-tag>
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentRow.startTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentRow.endTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="报名人数">
          {{ currentRow.currentPerson ?? 0 }} / {{ currentRow.maxPerson ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="价格">{{ formatMoney(currentRow.priceOverride) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(currentRow.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(currentRow.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remark || '--' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
