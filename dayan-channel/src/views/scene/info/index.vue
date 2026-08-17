<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCrud } from '@/composables/useCrud'
import { pageScenes } from '@/api/scene'
import type { SceneInfo, SceneInfoQuery } from '@/types/scene'
import { SCENE_TYPE_OPTIONS, SCENE_STATUS_OPTIONS, sceneStatusTagType } from '@/types/scene'

/**
 * 场景营销页（只读列表）。
 *
 * - 数据源：pageScenes（/channel-api/scenes，任务 6 新建）。
 * - 搜索：场景名称 / 类型 / 状态。
 * - 表格：sceneCode / sceneName / sceneType(text) / parkName / bookCount / sceneStatus(tag)。
 * - "详情"跳转场景详情路由页 /scene/detail/:sceneCode（SceneDetail，tab 式主从详情）。
 * - 后端端点未实现时降级（空表 + 控制台 warn，不弹 toast）。
 *
 * 说明：useCrud 不返回 handleReset（已核实 composables/useCrud.ts），
 * 故在本页面内自定义 handleReset（与 agent/account 等页面一致）。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  SceneInfo,
  SceneInfoQuery
>(
  { page: pageScenes },
  {
    initialQuery: {
      sceneName: '',
      sceneType: undefined,
      sceneStatus: undefined
    }
  }
)

function sceneTypeText(v?: number) {
  return SCENE_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}
function sceneStatusText(v?: number) {
  return SCENE_STATUS_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}

function handleReset() {
  query.sceneName = ''
  query.sceneType = undefined
  query.sceneStatus = undefined
  handleSearch()
}

// ---------- 详情跳转 ----------
const router = useRouter()

function goDetail(row: SceneInfo) {
  router.push({ name: 'SceneDetail', params: { sceneCode: row.sceneCode } })
}

onMounted(() => {
  loadPage().catch((err) => {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[scene/info] 加载场景列表失败（接口可能未实现）:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input v-model="query.sceneName" placeholder="场景名称" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-select v-model="query.sceneType" placeholder="类型" clearable style="width: 120px">
          <el-option v-for="o in SCENE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.sceneStatus" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="o in SCENE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header><div class="card-header"><span class="card-title">场景列表</span></div></template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="sceneCode">
        <el-table-column prop="sceneCode" label="场景编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="sceneName" label="场景名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="sceneType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ sceneTypeText(row.sceneType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="parkName" label="园区" min-width="120" show-overflow-tooltip />
        <el-table-column label="预约/容量" width="120" align="center">
          <template #default="{ row }">{{ row.bookCount ?? 0 }} / {{ row.capacity ?? '--' }}</template>
        </el-table-column>
        <el-table-column prop="sceneStatus" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="sceneStatusTagType(row.sceneStatus)">{{ sceneStatusText(row.sceneStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无数据" /></template>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
:current-page="query.current" :page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" background
          @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container { display: flex; flex-direction: column; gap: 16px; }
.search-card { :deep(.el-card__body) { padding-bottom: 2px; } }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
