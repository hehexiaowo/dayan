<script setup lang="ts">
import { ref } from 'vue'
import { useCrud } from '@/composables/useCrud'
import {
  pageEquityChangeHolders,
  getEquityChangeHolder
} from '@/api/equity'
import type { EquityChangeHolder, EquityChangeHolderQuery } from '@/types/equity'
import {
  ChangeHolderStatus,
  CHANGE_HOLDER_STATUS_OPTIONS
} from '@/types/equity'
import { formatDateTime, formatOption } from '@/utils/format'

/**
 * 权益更换权益人记录管理页（只读）。
 *
 * - 更换记录由 depot 换持有人生命周期自动产生（发起/完成/回滚在 /equity/depot 下），
 *   管理端不提供新增/编辑/删除。
 * - 仅保留：搜索 + 表格 + 分页 + 详情查看。
 * - 更换状态（changeStatus）：0=待处理 / 1=已完成 / 2=已回滚。
 */

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<EquityChangeHolder, EquityChangeHolderQuery>(
  { page: pageEquityChangeHolders },
  {
    initialQuery: {
      equityCode: '',
      changeStatus: undefined,
      operatorCode: ''
    }
  }
)

function handleReset() {
  query.equityCode = ''
  query.changeStatus = undefined
  query.operatorCode = ''
  handleSearch()
}

// ---------- 详情弹窗 ----------
const detailVisible = ref(false)
const detail = ref<EquityChangeHolder>({})

async function openDetail(row: EquityChangeHolder) {
  // 后端按 id 查询更换记录详情
  if (!row.id) {
    detail.value = row
    detailVisible.value = true
    return
  }
  try {
    detail.value = await getEquityChangeHolder(String(row.id))
  } catch {
    detail.value = row
  }
  detailVisible.value = true
}

// ---------- 辅助渲染 ----------
function changeStatusLabel(s?: number): string {
  return s != null ? formatOption(s, CHANGE_HOLDER_STATUS_OPTIONS) : '--'
}

/** 更换状态 tag：0待处理 warning / 1已完成 success / 2已回滚 info */
function changeStatusTagType(s?: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (s) {
    case ChangeHolderStatus.PENDING:
      return 'warning'
    case ChangeHolderStatus.DONE:
      return 'success'
    case ChangeHolderStatus.ROLLED_BACK:
      return 'info'
    default:
      return 'info'
  }
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="权益编码">
          <el-input v-model="query.equityCode" placeholder="权益编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="更换状态">
          <el-select v-model="query.changeStatus" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in CHANGE_HOLDER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人编码">
          <el-input v-model="query.operatorCode" placeholder="操作人编码" clearable @keyup.enter="handleSearch" />
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
          <span>权益更换权益人记录列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="equityCode" label="权益编码" min-width="150" show-overflow-tooltip fixed="left" />
        <el-table-column label="原权益人" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.oldPersonName || row.oldUsePersonCode || '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="oldPersonIdCard" label="原身份证号" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.oldPersonIdCard || '--' }}</template>
        </el-table-column>
        <el-table-column label="新权益人" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.newPersonName || row.newUsePersonCode || '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="newPersonIdCard" label="新身份证号" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.newPersonIdCard || '--' }}</template>
        </el-table-column>
        <el-table-column prop="changeReason" label="更换原因" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.changeReason || '--' }}</template>
        </el-table-column>
        <el-table-column prop="changeStatus" label="更换状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="changeStatusTagType(row.changeStatus)">
              {{ changeStatusLabel(row.changeStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorCode" label="操作人" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.operatorCode || '--' }}</template>
        </el-table-column>
        <el-table-column prop="operateTime" label="操作时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.operateTime) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
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
    <el-dialog v-model="detailVisible" title="权益更换权益人记录详情" width="780px" :close-on-click-modal="false">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="记录ID">{{ detail.id ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="权益编码">{{ detail.equityCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="原权益人">{{ detail.oldPersonName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="原使用人编码">{{ detail.oldUsePersonCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="原身份证号" :span="2">{{ detail.oldPersonIdCard || '--' }}</el-descriptions-item>
        <el-descriptions-item label="新权益人">{{ detail.newPersonName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="新使用人编码">{{ detail.newUsePersonCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="新身份证号" :span="2">{{ detail.newPersonIdCard || '--' }}</el-descriptions-item>
        <el-descriptions-item label="更换原因" :span="2">{{ detail.changeReason || '--' }}</el-descriptions-item>
        <el-descriptions-item label="更换状态">
          <el-tag :type="changeStatusTagType(detail.changeStatus)">
            {{ changeStatusLabel(detail.changeStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人编码">{{ detail.operatorCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatDateTime(detail.operateTime) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
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
