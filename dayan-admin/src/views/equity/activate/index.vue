<script setup lang="ts">
import { ref } from 'vue'
import { useCrud } from '@/composables/useCrud'
import {
  pageEquityActivates,
  getEquityActivate
} from '@/api/equity'
import type { EquityActivate, EquityActivateQuery } from '@/types/equity'
import {
  ActivateChannel,
  ACTIVATE_CHANNEL_OPTIONS
} from '@/types/equity'
import { formatDateTime, formatOption } from '@/utils/format'

/**
 * 权益激活记录管理页（只读）。
 *
 * - 激活记录由 depot 激活生命周期自动产生，管理端不提供新增/编辑/删除。
 * - 仅保留：搜索 + 表格 + 分页 + 详情查看。
 * - 激活渠道（activateChannel）：1=APP / 2=小程序 / 3=H5 / 4=管家代激活 / 5=代理人代激活。
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
} = useCrud<EquityActivate, EquityActivateQuery>(
  { page: pageEquityActivates },
  {
    initialQuery: {
      activateCode: '',
      equityCode: '',
      goodsCode: '',
      clientCode: '',
      activateChannel: undefined
    }
  }
)

function handleReset() {
  query.activateCode = ''
  query.equityCode = ''
  query.goodsCode = ''
  query.clientCode = ''
  query.activateChannel = undefined
  handleSearch()
}

// ---------- 详情弹窗 ----------
const detailVisible = ref(false)
const detail = ref<EquityActivate>({})

async function openDetail(row: EquityActivate) {
  // 后端按 equityCode 查询激活记录
  if (!row.equityCode) {
    detail.value = row
    detailVisible.value = true
    return
  }
  try {
    detail.value = await getEquityActivate(row.equityCode)
  } catch {
    detail.value = row
  }
  detailVisible.value = true
}

// ---------- 辅助渲染 ----------
function activateChannelLabel(c?: number): string {
  return c != null ? formatOption(c, ACTIVATE_CHANNEL_OPTIONS) : '--'
}

/** 激活渠道 tag：1APP primary / 2小程序 success / 3H5 info / 4管家代激活 warning / 5代理人代激活 warning */
function activateChannelTagType(c?: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (c) {
    case ActivateChannel.APP:
      return 'primary'
    case ActivateChannel.MINI_PROGRAM:
      return 'success'
    case ActivateChannel.H5:
      return 'info'
    case ActivateChannel.MANAGER:
    case ActivateChannel.AGENT:
      return 'warning'
    default:
      return 'info'
  }
}

/** 是/否标记：1=是 / 0=否 */
function yesNoLabel(v?: number): string {
  return v === 1 ? '是' : v === 0 ? '否' : '--'
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="激活码">
          <el-input v-model="query.activateCode" placeholder="激活码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="权益编码">
          <el-input v-model="query.equityCode" placeholder="权益编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="商品编码">
          <el-input v-model="query.goodsCode" placeholder="商品编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="客户编码">
          <el-input v-model="query.clientCode" placeholder="客户编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="激活渠道">
          <el-select v-model="query.activateChannel" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in ACTIVATE_CHANNEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
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
          <span>权益激活记录列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="activateCode" label="激活码" min-width="150" show-overflow-tooltip fixed="left" />
        <el-table-column prop="equityCode" label="权益编码" min-width="150" show-overflow-tooltip />
        <el-table-column prop="goodsCode" label="商品编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="clientFullName" label="客户" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.clientFullName || row.clientCode || '--' }}</template>
        </el-table-column>
        <el-table-column prop="clientPhone" label="客户手机号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="activateChannel" label="激活渠道" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="activateChannelTagType(row.activateChannel)">
              {{ activateChannelLabel(row.activateChannel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isIdCardVerified" label="实名核验" width="100" align="center">
          <template #default="{ row }">{{ yesNoLabel(row.isIdCardVerified) }}</template>
        </el-table-column>
        <el-table-column prop="isAgreementSigned" label="签署协议" width="100" align="center">
          <template #default="{ row }">{{ yesNoLabel(row.isAgreementSigned) }}</template>
        </el-table-column>
        <el-table-column prop="activateTime" label="激活时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.activateTime) }}</template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.expireTime) }}</template>
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
    <el-dialog v-model="detailVisible" title="权益激活记录详情" width="820px" :close-on-click-modal="false">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="激活码">{{ detail.activateCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="权益编码">{{ detail.equityCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="商品编码">{{ detail.goodsCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="激活来源编码">{{ detail.activateSourceCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detail.clientFullName || detail.clientCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="客户手机号">{{ detail.clientPhone || '--' }}</el-descriptions-item>
        <el-descriptions-item label="激活渠道">
          <el-tag :type="activateChannelTagType(detail.activateChannel)">
            {{ activateChannelLabel(detail.activateChannel) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="实名核验">{{ yesNoLabel(detail.isIdCardVerified) }}</el-descriptions-item>
        <el-descriptions-item label="签署协议">{{ yesNoLabel(detail.isAgreementSigned) }}</el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ detail.ipAddress || '--' }}</el-descriptions-item>
        <el-descriptions-item label="激活时间">{{ formatDateTime(detail.activateTime) }}</el-descriptions-item>
        <el-descriptions-item label="过期时间">{{ formatDateTime(detail.expireTime) }}</el-descriptions-item>
        <el-descriptions-item label="设备信息" :span="2">{{ detail.deviceInfo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '--' }}</el-descriptions-item>
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
