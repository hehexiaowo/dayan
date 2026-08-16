<script setup lang="ts">
/**
 * 供应商详情页 - 合同入口 tab。
 *
 * 不是完整 CRUD，而是"该供应商的合同概览 + 跳转独立合同页"。
 * 用 pageContracts({supplierCode}) 加载该供应商合同列表，只读展示关键字段；
 * 操作列提供"查看完整管理"，跳转独立合同页（路由 SupplierContract）。
 *
 * 顶部放"前往合同管理"按钮 → router.push('/resource/supplier/contract')，
 * 也可携带 supplierCode 作为初始过滤。
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pageContracts } from '@/api/supplier-contract'
import {
  CONTRACT_STATUS_OPTIONS,
  CONTRACT_TYPE_OPTIONS,
  ContractStatus
} from '@/types/supplier'
import type { SupplierContract, SupplierContractQuery } from '@/types/supplier'
import { formatMoney } from '@/utils/format'

const props = defineProps<{
  supplierCode: string
}>()

const router = useRouter()
const loading = ref(false)
const tableData = ref<SupplierContract[]>([])
const total = ref(0)

const query = reactive<SupplierContractQuery>({
  current: 1,
  size: 10,
  supplierCode: props.supplierCode
})

async function loadPage() {
  if (!props.supplierCode) return
  loading.value = true
  try {
    query.supplierCode = props.supplierCode
    const res = await pageContracts(query)
    tableData.value = res.records
    total.value = res.total
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

loadPage()

function handlePageChange(page: number) {
  query.current = page
  loadPage()
}
function handleSizeChange(size: number) {
  query.size = size
  query.current = 1
  loadPage()
}

// ---------- 跳转独立合同页 ----------
function goContractManage() {
  router.push({
    path: '/resource/supplier/contract',
    query: { supplierCode: props.supplierCode }
  })
}

function goContractDetail(row: SupplierContract) {
  if (!row.contractCode) return
  // 独立合同页支持 contractCode / supplierCode 过滤；查看单条用 contractCode
  router.push({
    path: '/resource/supplier/contract',
    query: { contractCode: row.contractCode }
  })
}

function refreshFailed() {
  ElMessage.info('刷新失败请稍后重试')
}

// ---------- 辅助渲染 ----------
function contractStatusLabel(v?: number): string {
  const found = CONTRACT_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}
function contractStatusTagType(v?: number): 'info' | 'warning' | 'success' | 'danger' {
  switch (v) {
    case ContractStatus.DRAFT:
      return 'info'
    case ContractStatus.PENDING_AUDIT:
      return 'warning'
    case ContractStatus.EFFECTIVE:
      return 'success'
    case ContractStatus.EXPIRED:
      return 'info'
    case ContractStatus.TERMINATED:
      return 'danger'
    case ContractStatus.VOID:
      return 'info'
    default:
      return 'info'
  }
}
function contractTypeLabel(v?: number): string {
  const found = CONTRACT_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}
function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}
function dateRange(a?: string, b?: string): string {
  return `${formatDate(a)} ~ ${formatDate(b)}`
}
defineExpose({ loadPage, refreshFailed })
</script>

<template>
  <div class="contract-tab">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="tab-tip"
      title="此处展示该供应商的合同概览，完整合同管理（新增/编辑/状态流转）请前往独立合同页。"
    >
      <template #default>
        <div class="tip-row">
          <span>此处展示该供应商的合同概览，完整合同管理（新增/编辑/状态流转）请前往独立合同页。</span>
          <el-button type="primary" size="small" :icon="'Link'" @click="goContractManage">前往合同管理</el-button>
        </div>
      </template>
    </el-alert>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="contractCode">
      <el-table-column prop="contractCode" label="合同编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="contractName" label="合同名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="contractType" label="类型" width="110" align="center">
        <template #default="{ row }">{{ contractTypeLabel(row.contractType) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="contractStatusTagType(row.status)" size="small">
            {{ contractStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="有效期" min-width="200" align="center">
        <template #default="{ row }">{{ dateRange(row.effectiveDate, row.expireDate) }}</template>
      </el-table-column>
      <el-table-column prop="contractAmount" label="合同金额" width="130" align="right">
        <template #default="{ row }">{{ formatMoney(row.contractAmount) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="goContractDetail(row)">查看完整管理</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="该供应商暂无合同，可前往合同管理新增" />
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
  </div>
</template>

<style scoped lang="scss">
.contract-tab {
  .tab-tip {
    margin-bottom: 16px;
  }
  .tip-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
