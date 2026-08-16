<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { pageDepots, transitionDepot } from '@/api/equity'
import type { EquityDepot, EquityDepotQuery } from '@/types/equity'
import { EquityStatus, EQUITY_STATUS_OPTIONS, CARRIER_TYPE_OPTIONS } from '@/types/equity'
import { formatDateTime } from '@/utils/format'

/**
 * 权益仓库管理页（核心链路，最复杂）。
 *
 * 第一版简化：
 * - 列表 + 分页 + 操作列事件流转（无新增——equityCode 服务端生成，权益卡由批次生产/入库产生；无编辑/删除——后端无 PUT/DELETE 端点）；
 * - 业务端点（stock-in / outbound / activate / void）统一走 transition 通用方法
 *   （POST /transition?equityCode=&event=，event 名：stock-in / outbound / activate / void）；
 * - 操作列按钮按 equityStatus 动态显示：
 *   - status=0(库存中): 出库
 *   - status=1(已出库): 激活
 *   - status=0/1(库存中/已出库): 作废
 *
 * equityStatus：0=库存中/1=已出库/2=已激活/3=使用中/4=已完成/5=已过期/6=已作废/7=更换权益人中。
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
} = useCrud<EquityDepot, EquityDepotQuery>(
  { page: pageDepots },
  {
    initialQuery: {
      equityCode: '',
      equityNo: '',
      goodsCode: '',
      batchCode: '',
      channelCode: '',
      agentCode: '',
      clientCode: '',
      carrierType: undefined,
      equityStatus: undefined
    }
  }
)

function handleReset() {
  query.equityCode = ''
  query.equityNo = ''
  query.goodsCode = ''
  query.batchCode = ''
  query.channelCode = ''
  query.agentCode = ''
  query.clientCode = ''
  query.carrierType = undefined
  query.equityStatus = undefined
  handleSearch()
}

// ---------- 状态机流转（第一版统一走 transition） ----------
/**
 * 通用状态流转：ElMessageBox 确认后调用 transitionDepot(equityCode, event)。
 *
 * @param row 权益记录
 * @param event 状态机事件名：outbound / activate / void 等（对齐 DDL EQUITY_SM 状态机）
 * @param msg 二次确认提示文案
 */
async function handleTransition(row: EquityDepot, event: string, msg: string) {
  if (!row.equityCode) return
  await ElMessageBox.confirm(msg, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await transitionDepot(row.equityCode, event)
  ElMessage.success('操作成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function equityStatusLabel(s?: number): string {
  const found = EQUITY_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

/**
 * 权益状态 tag 颜色映射（共 8 态，对齐 DDL）。
 * 0库存中info / 1已出库warning / 2已激活primary / 3使用中success /
 * 4已完成success / 5已过期danger / 6已作废info / 7更换权益人中warning。
 */
function equityStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (status) {
    case EquityStatus.OUTBOUND:
      return 'warning'
    case EquityStatus.ACTIVATED:
      return 'primary'
    case EquityStatus.IN_USE:
    case EquityStatus.COMPLETED:
      return 'success'
    case EquityStatus.EXPIRED:
      return 'danger'
    case EquityStatus.CHANGING_HOLDER:
      return 'warning'
    case EquityStatus.STOCK:
    case EquityStatus.VOID:
    default:
      return 'info'
  }
}

function carrierTypeLabel(c?: number): string {
  const found = CARRIER_TYPE_OPTIONS.find((o) => o.value === c)
  return found ? found.label : c != null ? String(c) : '--'
}

// 初始化加载
loadPage()
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
          style="width: 150px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.equityNo"
          placeholder="权益卡号"
          clearable
          style="width: 150px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.goodsCode"
          placeholder="商品编码"
          clearable
          style="width: 140px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.batchCode"
          placeholder="批次编码"
          clearable
          style="width: 140px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.channelCode"
          placeholder="渠道编码"
          clearable
          style="width: 140px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.agentCode"
          placeholder="代理人编码"
          clearable
          style="width: 140px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.clientCode"
          placeholder="客户编码"
          clearable
          style="width: 140px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.carrierType" placeholder="载体类型" clearable style="width: 130px">
          <el-option v-for="o in CARRIER_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.equityStatus" placeholder="权益状态" clearable style="width: 130px">
          <el-option v-for="o in EQUITY_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
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
          <span class="card-title">权益仓库列表</span>
          <!-- 提示：权益卡由批次生产/入库产生，无手动新增入口 -->
          <el-tooltip content="权益卡由批次入库生成，无手动新增入口" placement="left">
            <el-icon class="hint-icon"><InfoFilled /></el-icon>
          </el-tooltip>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="equityCode">
        <el-table-column prop="equityCode" label="权益编码" min-width="150" show-overflow-tooltip fixed="left" />
        <el-table-column prop="equityNo" label="卡号" min-width="140" show-overflow-tooltip fixed="left" />
        <el-table-column prop="goodsCode" label="商品编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="batchCode" label="批次" min-width="130" show-overflow-tooltip />
        <el-table-column prop="personCount" label="使用人数" width="100" align="center" />
        <el-table-column prop="channelCode" label="渠道" min-width="110" show-overflow-tooltip />
        <el-table-column prop="agentCode" label="代理人" min-width="110" show-overflow-tooltip />
        <el-table-column prop="clientCode" label="客户" min-width="110" show-overflow-tooltip />
        <el-table-column prop="carrierType" label="载体" width="90" align="center">
          <template #default="{ row }">{{ carrierTypeLabel(row.carrierType) }}</template>
        </el-table-column>
        <el-table-column prop="logisticsNo" label="物流单号" min-width="130" show-overflow-tooltip />
        <el-table-column prop="equityStatus" label="权益状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="equityStatusTagType(row.equityStatus)">
              {{ equityStatusLabel(row.equityStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="activateTime" label="激活时间" width="160" align="center">
          <template #default="{ row }">{{ formatDateTime(row.activateTime) }}</template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="160" align="center">
          <template #default="{ row }">{{ formatDateTime(row.expireTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <!-- 出库：库存中(0) → 已出库(1) -->
            <el-button
              v-if="row.equityStatus === EquityStatus.STOCK"
              link
              type="success"
              size="small"
              @click="handleTransition(row, 'outbound', '确定出库此权益卡？')"
            >
              出库
            </el-button>
            <!-- 激活：已出库(1) → 已激活(2) -->
            <el-button
              v-if="row.equityStatus === EquityStatus.OUTBOUND"
              link
              type="warning"
              size="small"
              @click="handleTransition(row, 'activate', '确定激活此权益卡？')"
            >
              激活
            </el-button>
            <!-- 作废：库存中/已出库(0,1) → 已作废(6) -->
            <el-button
              v-if="row.equityStatus === EquityStatus.STOCK || row.equityStatus === EquityStatus.OUTBOUND"
              link
              type="danger"
              size="small"
              @click="handleTransition(row, 'void', '确定作废此权益卡？作废后不可恢复。')"
            >
              作废
            </el-button>
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
  </div>
</template>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;

  .toolbar-actions {
    display: flex;
    gap: 8px;
    margin-left: auto;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}

.hint-icon {
  font-size: 16px;
  color: #909399;
  cursor: help;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}
</style>
