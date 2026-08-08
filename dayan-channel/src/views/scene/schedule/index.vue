<script setup lang="ts">
import { onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { getSceneSchedule, pageSceneSchedules } from '@/api/scene'
import {
  SCENE_SCHEDULE_STATUS_OPTIONS,
  SceneScheduleStatus,
  type SceneSchedule,
  type SceneScheduleQuery
} from '@/types/scene'

/**
 * 场景管理页（业务运营目录）。
 *
 * 管理本渠道所有场景活动日程（scene_schedule）的记录+流转：
 * - 搜索栏：场景编码 / 日程状态 / 日程日期范围；
 * - el-table：sceneCode / sceneName / scheduleDate / 时段 / 报名人数 / 价格 / 状态(5态tag) / 操作(详情)；
 * - 详情：沿用渠道端主流模式 ElMessageBox.alert 展示结构化 JSON（日程字段简单，
 *   与 order-manage/invoice 的详情体验一致）。
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
import { ref } from 'vue'
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

/** HTML 转义，防止 JSON 字段在 dangerouslyUseHTMLString 下注入 */
function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => {
    switch (c) {
      case '&':
        return '&amp;'
      case '<':
        return '&lt;'
      case '>':
        return '&gt;'
      case '"':
        return '&quot;'
      default:
        return '&#39;'
    }
  })
}

async function viewDetail(row: SceneSchedule) {
  if (!row.id) return
  try {
    const vo = await getSceneSchedule(row.id)
    await ElMessageBox.alert(
      `<pre class="scene-schedule-detail-pre">${escapeHtml(JSON.stringify(vo, null, 2))}</pre>`,
      `场景日程详情 · ${row.sceneName || row.sceneCode}`,
      {
        confirmButtonText: '关闭',
        dangerouslyUseHTMLString: true,
        customClass: 'scene-schedule-detail-msgbox'
      }
    )
  } catch (err) {
    // 用户关闭弹窗或接口报错：静默（错误已由响应拦截器统一提示）
    void err
  }
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
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="场景编码">
          <el-input v-model="query.sceneCode" placeholder="场景编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="日程状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in SCENE_SCHEDULE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="日程日期">
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
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>场景活动日程列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="sceneCode" label="场景编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="sceneName" label="场景名称" min-width="140" show-overflow-tooltip />
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
        <el-table-column prop="priceOverride" label="价格（元）" width="110" align="right">
          <template #default="{ row }">
            {{ row.priceOverride != null ? Number(row.priceOverride).toFixed(2) : '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="日程状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status !== undefined && row.status !== null" :type="statusTagType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">查看详情</el-button>
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

<style lang="scss">
.scene-schedule-detail-msgbox {
  .scene-schedule-detail-pre {
    margin: 0;
    max-height: 60vh;
    overflow: auto;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
    font-size: 12px;
    line-height: 1.6;
    color: #303133;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
