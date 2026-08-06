<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageDepots,
  getDepot,
  updateDepot,
  deleteDepot,
  transitionDepot
} from '@/api/equity'
import type { EquityDepot, EquityDepotQuery } from '@/types/equity'
import {
  EquityType,
  EquityStatus,
  CarrierType,
  EQUITY_TYPE_OPTIONS,
  EQUITY_STATUS_OPTIONS,
  CARRIER_TYPE_OPTIONS
} from '@/types/equity'

/**
 * 权益仓库管理页（核心链路，最复杂）。
 *
 * 第一版简化：
 * - 标准 CRUD（搜索 + 表格 + 分页 + 编辑弹窗，无新增——equityCode 服务端生成，权益卡由批次生产/入库产生）；
 * - 业务端点（stock-in / outbound / activate / void）统一走 transition 通用方法
 *   （POST /transition?equityCode=&event=，event 名：stock-in / outbound / activate / void）；
 * - 操作列按钮按 equityStatus 动态显示：
 *   - status=0(待入库): 入库
 *   - status=1(在库): 出库
 *   - status=2(已出库): 激活
 *   - 任意状态（非已作废）: 作废
 *
 * equityStatus：0待入库/1在库/2已出库/3已激活/4已使用/5已过期/6已作废/7变更中。
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
      templateCode: '',
      batchCode: '',
      channelCode: '',
      agentCode: '',
      clientCode: '',
      carrierType: undefined,
      equityStatus: undefined
    }
  }
)

// ---------- 编辑弹窗 ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/**
 * 编辑表单：取 15 个核心字段（equityCode 服务端生成，仅展示不可编辑）。
 */
const form = reactive<EquityDepot>({
  equityCode: undefined,
  equityNo: '',
  templateCode: '',
  batchCode: '',
  equityType: EquityType.SERVICE_CARD,
  equityValue: undefined,
  channelCode: '',
  agentCode: '',
  clientCode: '',
  carrierType: CarrierType.PHYSICAL_CARD,
  equityStatus: EquityStatus.PENDING_STOCK_IN,
  activateCode: '',
  logisticsNo: '',
  remark: ''
})

const rules: FormRules<EquityDepot> = {
  equityNo: [{ required: true, message: '请输入权益卡号', trigger: 'blur' }],
  templateCode: [{ required: true, message: '请输入关联模板编码', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    equityCode: undefined,
    equityNo: '',
    templateCode: '',
    batchCode: '',
    equityType: EquityType.SERVICE_CARD,
    equityValue: undefined,
    channelCode: '',
    agentCode: '',
    clientCode: '',
    carrierType: CarrierType.PHYSICAL_CARD,
    equityStatus: EquityStatus.PENDING_STOCK_IN,
    activateCode: '',
    logisticsNo: '',
    remark: ''
  })
}

async function openEdit(row: EquityDepot) {
  if (!row.equityCode) return
  resetForm()
  try {
    const detail = await getDepot(row.equityCode)
    Object.assign(form, {
      equityCode: detail.equityCode,
      equityNo: detail.equityNo ?? '',
      templateCode: detail.templateCode ?? '',
      batchCode: detail.batchCode ?? '',
      equityType: detail.equityType ?? EquityType.SERVICE_CARD,
      equityValue: detail.equityValue,
      channelCode: detail.channelCode ?? '',
      agentCode: detail.agentCode ?? '',
      clientCode: detail.clientCode ?? '',
      carrierType: detail.carrierType ?? CarrierType.PHYSICAL_CARD,
      equityStatus: detail.equityStatus ?? EquityStatus.PENDING_STOCK_IN,
      activateCode: detail.activateCode ?? '',
      logisticsNo: detail.logisticsNo ?? '',
      remark: detail.remark ?? ''
    })
  } catch {
    Object.assign(form, {
      equityCode: row.equityCode,
      equityNo: row.equityNo ?? '',
      templateCode: row.templateCode ?? '',
      batchCode: row.batchCode ?? '',
      equityType: row.equityType ?? EquityType.SERVICE_CARD,
      equityValue: row.equityValue,
      channelCode: row.channelCode ?? '',
      agentCode: row.agentCode ?? '',
      clientCode: row.clientCode ?? '',
      carrierType: row.carrierType ?? CarrierType.PHYSICAL_CARD,
      equityStatus: row.equityStatus ?? EquityStatus.PENDING_STOCK_IN,
      activateCode: row.activateCode ?? '',
      logisticsNo: row.logisticsNo ?? '',
      remark: row.remark ?? ''
    })
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (!form.equityCode) return

  submitLoading.value = true
  try {
    await updateDepot(form.equityCode, form)
    ElMessage.success('修改成功')
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

function handleReset() {
  query.equityCode = ''
  query.equityNo = ''
  query.templateCode = ''
  query.batchCode = ''
  query.channelCode = ''
  query.agentCode = ''
  query.clientCode = ''
  query.carrierType = undefined
  query.equityStatus = undefined
  handleSearch()
}

async function handleDeleteRow(row: EquityDepot) {
  if (!row.equityCode) return
  await ElMessageBox.confirm(`确定删除权益卡「${row.equityNo ?? row.equityCode}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteDepot(row.equityCode)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 状态机流转（第一版统一走 transition） ----------
/**
 * 通用状态流转：ElMessageBox 确认后调用 transitionDepot(equityCode, event)。
 *
 * @param row 权益记录
 * @param event 状态机事件名：stock-in / outbound / activate / void 等
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
 * 权益状态 tag 颜色映射（共 8 态）。
 * 0待入库info / 1在库success / 2已出库warning / 3已激活primary /
 * 4已使用info / 5已过期danger / 6已作废danger / 7变更中warning。
 */
function equityStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (status) {
    case EquityStatus.IN_STOCK:
      return 'success'
    case EquityStatus.OUT_BOUND:
      return 'warning'
    case EquityStatus.ACTIVATED:
      return 'primary'
    case EquityStatus.EXPIRED:
    case EquityStatus.VOIDED:
      return 'danger'
    case EquityStatus.CHANGING:
      return 'warning'
    case EquityStatus.PENDING_STOCK_IN:
    case EquityStatus.USED:
    default:
      return 'info'
  }
}

function equityTypeLabel(t?: number): string {
  const found = EQUITY_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
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
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="权益编码">
          <el-input v-model="query.equityCode" placeholder="权益编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="卡号">
          <el-input v-model="query.equityNo" placeholder="权益卡号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="模板编码">
          <el-input v-model="query.templateCode" placeholder="模板编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="批次编码">
          <el-input v-model="query.batchCode" placeholder="批次编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="渠道编码">
          <el-input v-model="query.channelCode" placeholder="渠道编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="代理人">
          <el-input v-model="query.agentCode" placeholder="代理人编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="客户">
          <el-input v-model="query.clientCode" placeholder="客户编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="载体类型">
          <el-select v-model="query.carrierType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in CARRIER_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="权益状态">
          <el-select v-model="query.equityStatus" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in EQUITY_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>权益仓库列表</span>
          <!-- 提示：权益卡由批次生产/入库产生，无手动新增入口 -->
          <el-tooltip content="权益卡由批次入库生成，无手动新增入口" placement="left">
            <el-icon class="hint-icon"><InfoFilled /></el-icon>
          </el-tooltip>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="equityCode">
        <el-table-column prop="equityCode" label="权益编码" min-width="150" show-overflow-tooltip fixed="left" />
        <el-table-column prop="equityNo" label="卡号" min-width="140" show-overflow-tooltip fixed="left" />
        <el-table-column prop="templateCode" label="模板" min-width="130" show-overflow-tooltip />
        <el-table-column prop="batchCode" label="批次" min-width="130" show-overflow-tooltip />
        <el-table-column prop="equityType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ equityTypeLabel(row.equityType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="equityValue" label="面值" width="90" align="right" />
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
        <el-table-column prop="activateTime" label="激活时间" width="160" align="center" />
        <el-table-column prop="expireTime" label="过期时间" width="160" align="center" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <!-- 入库：仅 status=0 待入库 -->
            <el-button
              v-if="row.equityStatus === EquityStatus.PENDING_STOCK_IN"
              link
              type="primary"
              size="small"
              @click="handleTransition(row, 'stock-in', '确定入库此权益卡？')"
            >
              入库
            </el-button>
            <!-- 出库：仅 status=1 在库 -->
            <el-button
              v-if="row.equityStatus === EquityStatus.IN_STOCK"
              link
              type="success"
              size="small"
              @click="handleTransition(row, 'outbound', '确定出库此权益卡？')"
            >
              出库
            </el-button>
            <!-- 激活：仅 status=2 已出库 -->
            <el-button
              v-if="row.equityStatus === EquityStatus.OUT_BOUND"
              link
              type="warning"
              size="small"
              @click="handleTransition(row, 'activate', '确定激活此权益卡？')"
            >
              激活
            </el-button>
            <!-- 作废：任意状态（非已作废 status!=6） -->
            <el-button
              v-if="row.equityStatus !== EquityStatus.VOIDED"
              link
              type="danger"
              size="small"
              @click="handleTransition(row, 'void', '确定作废此权益卡？作废后不可恢复。')"
            >
              作废
            </el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
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

    <!-- 编辑弹窗（无新增，equityCode 服务端生成） -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑权益卡"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="权益编码">
              <el-input :model-value="form.equityCode" disabled placeholder="服务端生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权益卡号" prop="equityNo">
              <el-input v-model="form.equityNo" placeholder="权益卡号" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="form.templateCode" placeholder="权益模板编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="批次编码">
              <el-input v-model="form.batchCode" placeholder="批次编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权益类型">
              <el-select v-model="form.equityType" placeholder="权益类型" style="width: 100%">
                <el-option v-for="o in EQUITY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="权益面值">
              <el-input-number v-model="form.equityValue" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="载体类型">
              <el-select v-model="form.carrierType" placeholder="载体类型" style="width: 100%">
                <el-option v-for="o in CARRIER_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="权益状态">
              <el-select v-model="form.equityStatus" placeholder="权益状态" style="width: 100%">
                <el-option v-for="o in EQUITY_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="渠道编码">
              <el-input v-model="form.channelCode" placeholder="分配渠道" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="代理人">
              <el-input v-model="form.agentCode" placeholder="分配代理人" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户">
              <el-input v-model="form.clientCode" placeholder="领取客户" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="激活码">
              <el-input v-model="form.activateCode" placeholder="激活码" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物流单号">
              <el-input v-model="form.logisticsNo" placeholder="物流单号" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
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
</style>
