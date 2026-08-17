<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useCrud } from '@/composables/useCrud'
import { pageEquities } from '@/api/equity'
import {
  CARRIER_TYPE_OPTIONS,
  EQUITY_STATUS_OPTIONS,
  EquityStatus,
  type Equity,
  type EquityQuery
} from '@/types/equity'
import { formatFileUrl } from '@/utils/file'
import { formatDateTime } from '@/utils/format'

/**
 * 权益管理页（业务运营目录）。
 *
 * 综合查询本渠道每个权益的完整生命周期流转信息：
 * - 搜索栏：权益编码 / 权益状态 / 载体类型 / 关联客户编码；
 * - el-table：equityCode / equityStatus / carrierType / personCount /
 *   clientCode / activateTime / expireTime / 操作（查看详情）；
 * - 详情抽屉（el-drawer + el-descriptions + el-timeline）：基本信息 / 分配与客户 /
 *   激活与物流 / 流转时间轴，数据直接取列表 row（后端 VO 字段齐全，无需额外请求）。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  Equity,
  EquityQuery
>(
  { page: pageEquities },
  {
    initialQuery: {
      equityCode: '',
      equityStatus: undefined,
      carrierType: undefined,
      clientCode: ''
    }
  }
)

function handleReset() {
  query.equityCode = ''
  query.equityStatus = undefined
  query.carrierType = undefined
  query.clientCode = ''
  handleSearch()
}

function statusTagType(v?: number): 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  switch (v) {
    case EquityStatus.ACTIVATED:
    case EquityStatus.IN_USE:
      return 'success'
    case EquityStatus.STOCK:
    case EquityStatus.OUTBOUND:
      return 'warning'
    case EquityStatus.COMPLETED:
      return 'primary'
    case EquityStatus.EXPIRED:
    case EquityStatus.VOID:
      return 'danger'
    case EquityStatus.CHANGING_HOLDER:
      return 'info'
    default:
      return 'info'
  }
}

function statusText(v?: number) {
  const opt = EQUITY_STATUS_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

function carrierText(v?: number) {
  const opt = CARRIER_TYPE_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

onMounted(() => {
  loadPage().catch((err) => {
    console.warn('[equity] 加载权益列表失败:', err)
  })
})

// ==================== 详情抽屉 ====================

const detailVisible = ref(false)
const currentEquity = ref<Equity | null>(null)

function openDetail(row: Equity) {
  currentEquity.value = row
  detailVisible.value = true
}

/** 时间轴节点：仅收集有值的时间戳，按时间正序展示。 */
interface TimelineNode {
  time?: string
  label: string
  icon: string
  color: string
  extra?: string
}

const timelineNodes = computed<TimelineNode[]>(() => {
  const e = currentEquity.value
  if (!e) return []
  const nodes: TimelineNode[] = [
    { time: e.produceTime, label: '入库', icon: 'Box', color: '#909399' },
    { time: e.allocateTime, label: '分配渠道', icon: 'Share', color: '#67C23A' },
    { time: e.outboundTime, label: '出库', icon: 'Van', color: '#E6A23C', extra: e.logisticsNo ? `物流单号：${e.logisticsNo}` : undefined },
    { time: e.activateTime, label: '激活', icon: 'CircleCheck', color: '#409EFF' },
    { time: e.firstUseTime, label: '首次使用', icon: 'Aim', color: '#67C23A' },
    { time: e.lastUseTime, label: '最近使用', icon: 'Refresh', color: '#67C23A' },
    { time: e.expireTime, label: '到期', icon: 'AlarmClock', color: '#F56C6C' }
  ]
  return nodes.filter((n) => n.time).sort((a, b) => (a.time! < b.time! ? -1 : 1))
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input
          v-model="query.equityCode"
          placeholder="权益编码"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.equityStatus" placeholder="权益状态" clearable style="width: 140px">
          <el-option v-for="o in EQUITY_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.carrierType" placeholder="载体类型" clearable style="width: 140px">
          <el-option v-for="o in CARRIER_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-input
          v-model="query.clientCode"
          placeholder="关联客户编码"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
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
          <span class="card-title">权益列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="equityCode">
        <el-table-column prop="equityCode" label="权益编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="equityStatus" label="权益状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.equityStatus !== undefined && row.equityStatus !== null" :type="statusTagType(row.equityStatus)">
              {{ statusText(row.equityStatus) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="商品名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.goodsName">{{ row.goodsName }}</span>
            <span v-else class="text-muted">未关联订单</span>
          </template>
        </el-table-column>
        <el-table-column prop="skuName" label="规格" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.skuName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="carrierType" label="载体类型" width="100" align="center">
          <template #default="{ row }">{{ carrierText(row.carrierType) }}</template>
        </el-table-column>
        <el-table-column prop="personCount" label="使用人数" width="100" align="center" />
        <el-table-column prop="clientCode" label="客户编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="activateTime" label="激活时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.activateTime) }}</template>
        </el-table-column>
        <el-table-column prop="expireTime" label="到期时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.expireTime) }}</template>
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

    <!-- 详情抽屉：基本信息 / 分配与客户 / 激活与物流 / 流转时间轴 -->
    <el-drawer v-model="detailVisible" title="权益生命周期" size="560px" direction="rtl">
      <template v-if="currentEquity">
        <!-- 基本信息 -->
        <div class="detail-section">
          <h4 class="detail-section-title">基本信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="权益编码">{{ currentEquity.equityCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="权益状态">
              <el-tag v-if="currentEquity.equityStatus !== undefined" :type="statusTagType(currentEquity.equityStatus)">
                {{ statusText(currentEquity.equityStatus) }}
              </el-tag>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item label="卡号">{{ currentEquity.equityNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="使用人数">
              {{ currentEquity.personCount != null ? currentEquity.personCount : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="载体类型">{{ carrierText(currentEquity.carrierType) }}</el-descriptions-item>
            <el-descriptions-item label="商品名称">
              {{ currentEquity.goodsName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="规格">
              {{ currentEquity.skuName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="成本价">
              {{ currentEquity.costPrice != null ? `¥${Number(currentEquity.costPrice).toFixed(2)}` : '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 分配与客户 -->
        <div class="detail-section">
          <h4 class="detail-section-title">分配与客户</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="渠道编码">{{ currentEquity.channelCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="代理人编码">{{ currentEquity.agentCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户编码">{{ currentEquity.clientCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="关联订单">{{ currentEquity.orderCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="批次编码">{{ currentEquity.batchCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商品编码">{{ currentEquity.goodsCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="出库渠道">{{ currentEquity.outboundChannelCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="出库代理人">{{ currentEquity.outboundAgentCode || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 激活与物流 -->
        <div class="detail-section">
          <h4 class="detail-section-title">激活与物流</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="激活码">{{ currentEquity.activateCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="绑定码">{{ currentEquity.bindCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="物流单号">{{ currentEquity.logisticsNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="库存到期">
              {{ currentEquity.shelfExpireTime ? formatDateTime(currentEquity.shelfExpireTime) : '-' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="currentEquity.qrCodeUrl" label="二维码" :span="2">
              <el-link type="primary" :href="formatFileUrl(currentEquity.qrCodeUrl)" target="_blank">
                {{ formatFileUrl(currentEquity.qrCodeUrl) }}
              </el-link>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 生命周期时间轴 -->
        <div class="detail-section">
          <h4 class="detail-section-title">流转时间轴</h4>
          <el-timeline v-if="timelineNodes.length">
            <el-timeline-item
              v-for="(node, idx) in timelineNodes"
              :key="idx"
              :timestamp="formatDateTime(node.time)"
              placement="top"
              :color="node.color"
            >
              <div class="timeline-label">{{ node.label }}</div>
              <div v-if="node.extra" class="timeline-extra">{{ node.extra }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无流转记录" :image-size="60" />
        </div>

        <!-- 作废原因 / 备注（仅在有值时展示） -->
        <div v-if="currentEquity.voidReason || currentEquity.remark" class="detail-section">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item v-if="currentEquity.voidReason" label="作废原因">
              {{ currentEquity.voidReason }}
            </el-descriptions-item>
            <el-descriptions-item v-if="currentEquity.remark" label="备注">
              {{ currentEquity.remark }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
    </el-drawer>
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

.text-muted {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.detail-section {
  margin-bottom: 24px;

  &-title {
    margin: 0 0 12px;
    font-size: 15px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
}

.timeline-label {
  font-size: 14px;
  font-weight: 500;
}

.timeline-extra {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
