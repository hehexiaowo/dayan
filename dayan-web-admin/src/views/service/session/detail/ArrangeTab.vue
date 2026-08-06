<script setup lang="ts">
/**
 * 服务会话详情页 - 全程安排 tab。
 *
 * 架构（useCrud 分页 + 弹窗 CRUD + confirm 确认）：
 * 1. 搜索条（arrangeType + isConfirmed + status）+ 新增按钮
 * 2. 主表格：useCrud（idKey:'id', fixedParams:{sessionCode}）分页加载
 * 3. 新增/编辑 el-dialog：arrangeCode 服务端生成，solutionCode 可选（软关联方案）
 * 4. 操作列额外「确认」按钮：调 confirmServiceEquityArrange(id, isConfirmed)，
 *    用于业务链：会话 start_service 前须存在 isConfirmed=1 的安排。
 *
 * 红线遵守：
 * - 主键 id 雪花 Long，useCrud 传 idKey:'id'
 * - arrangeType / isConfirmed / status 用 el-select + OPTIONS
 * - arrangeDate / timeStart / timeEnd 用 el-date-picker / el-time-picker
 * - participantCount 才用 el-input-number
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageServiceEquityArranges,
  createServiceEquityArrange,
  updateServiceEquityArrange,
  deleteServiceEquityArrange,
  confirmServiceEquityArrange
} from '@/api/service-sub'
import {
  ARRANGE_TYPE_OPTIONS,
  ARRANGE_STATUS_OPTIONS,
  FOLLOWUP_YES_NO_OPTIONS
} from '@/types/service'
import type { ServiceEquityArrange, ServiceEquityArrangeQuery } from '@/types/service'

const props = defineProps<{
  sessionCode: string
}>()

// ---------- 列表（useCrud，主键 id） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ServiceEquityArrange,
  ServiceEquityArrangeQuery,
  number
>(
  {
    page: pageServiceEquityArranges,
    create: createServiceEquityArrange,
    update: (id, data) => updateServiceEquityArrange(id, data),
    remove: deleteServiceEquityArrange
  },
  {
    initialQuery: { arrangeType: undefined, isConfirmed: undefined, status: undefined },
    idKey: 'id',
    fixedParams: { sessionCode: props.sessionCode }
  }
)

loadPage()

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ServiceEquityArrange>({
  id: undefined,
  sessionCode: '',
  solutionCode: '',
  clientCode: '',
  butlerCode: '',
  arrangeCode: '',
  arrangeType: 1,
  parkCode: '',
  parkFullName: '',
  arrangeDate: '',
  arrangeTimeStart: '',
  arrangeTimeEnd: '',
  arrangeAddress: '',
  contactPerson: '',
  contactPhone: '',
  participantCount: undefined,
  prepareItems: '',
  progressNotes: '',
  isConfirmed: 0,
  status: 0,
  cancelReason: '',
  remark: ''
})

const rules: FormRules<ServiceEquityArrange> = {
  arrangeType: [{ required: true, message: '请选择安排类型', trigger: 'change' }],
  arrangeDate: [{ required: true, message: '请选择安排日期', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    sessionCode: '',
    solutionCode: '',
    clientCode: '',
    butlerCode: '',
    arrangeCode: '',
    arrangeType: 1,
    parkCode: '',
    parkFullName: '',
    arrangeDate: '',
    arrangeTimeStart: '',
    arrangeTimeEnd: '',
    arrangeAddress: '',
    contactPerson: '',
    contactPhone: '',
    participantCount: undefined,
    prepareItems: '',
    progressNotes: '',
    isConfirmed: 0,
    status: 0,
    cancelReason: '',
    remark: ''
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.sessionCode = props.sessionCode
  dialogVisible.value = true
}

function openEdit(row: ServiceEquityArrange) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  // 时间校验：timeStart < timeEnd（后端也校验）
  if (form.arrangeTimeStart && form.arrangeTimeEnd && form.arrangeTimeStart >= form.arrangeTimeEnd) {
    ElMessage.warning('开始时间需早于结束时间')
    return
  }
  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createServiceEquityArrange(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateServiceEquityArrange(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ServiceEquityArrange) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除安排「${row.arrangeCode || row.id}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteServiceEquityArrange(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 确认安排（业务链端点） ----------
const confirmLoading = ref(false)

async function handleConfirm(row: ServiceEquityArrange) {
  if (!row.id) return
  await ElMessageBox.confirm(
    `确认安排「${row.arrangeCode || row.id}」吗？确认后将自动写入确认时间，且方可触发会话开始服务。`,
    '确认安排',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  confirmLoading.value = true
  try {
    await confirmServiceEquityArrange(row.id, 1)
    ElMessage.success('已确认安排')
    loadPage()
  } finally {
    confirmLoading.value = false
  }
}

// ---------- 辅助渲染 ----------
function arrangeTypeLabel(v?: number): string {
  const found = ARRANGE_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function isConfirmedLabel(v?: number): string {
  const found = FOLLOWUP_YES_NO_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function isConfirmedTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}

function statusLabel(v?: number): string {
  const found = ARRANGE_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function statusTagType(v?: number): 'info' | 'warning' | 'success' | 'danger' | 'primary' {
  switch (v) {
    case 3:
      return 'success'
    case 4:
      return 'danger'
    case 2:
      return 'primary'
    case 1:
      return 'warning'
    case 0:
    default:
      return 'info'
  }
}

function formatDateTime(s?: string): string {
  if (!s) return '--'
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

defineExpose({ loadPage })
</script>

<template>
  <div class="arrange-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="安排类型">
        <el-select v-model="query.arrangeType" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="o in ARRANGE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="确认状态">
        <el-select v-model="query.isConfirmed" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="o in FOLLOWUP_YES_NO_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="安排状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="o in ARRANGE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增安排</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="arrangeCode" label="安排编码" min-width="150" show-overflow-tooltip />
      <el-table-column prop="arrangeType" label="类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ arrangeTypeLabel(row.arrangeType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="parkFullName" label="关联机构" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.parkFullName || row.parkCode || '--' }}</template>
      </el-table-column>
      <el-table-column prop="arrangeDate" label="日期" width="120" align="center" />
      <el-table-column label="时间段" width="160" align="center">
        <template #default="{ row }">
          <span v-if="row.arrangeTimeStart || row.arrangeTimeEnd">
            {{ row.arrangeTimeStart || '?' }} ~ {{ row.arrangeTimeEnd || '?' }}
          </span>
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column prop="participantCount" label="人数" width="80" align="center">
        <template #default="{ row }">{{ row.participantCount != null ? row.participantCount : '--' }}</template>
      </el-table-column>
      <el-table-column prop="isConfirmed" label="确认" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="isConfirmedTagType(row.isConfirmed)" size="small">{{ isConfirmedLabel(row.isConfirmed) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="140" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-if="row.isConfirmed !== 1"
            link
            type="success"
            size="small"
            :disabled="confirmLoading"
            @click="handleConfirm(row)"
          >
            确认
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增安排' : '编辑安排'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col v-if="dialogMode === 'edit'" :span="12">
            <el-form-item label="安排编码">
              <el-input v-model="form.arrangeCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安排类型" prop="arrangeType">
              <el-select v-model="form.arrangeType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in ARRANGE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联方案">
              <el-input v-model="form.solutionCode" placeholder="方案编码（软关联，可选）" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构编码">
              <el-input v-model="form.parkCode" placeholder="养老机构编码（可选）" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构名称">
              <el-input v-model="form.parkFullName" placeholder="养老机构名称快照（可选）" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安排日期" prop="arrangeDate">
              <el-date-picker
                v-model="form.arrangeDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="开始时间">
              <el-time-picker
                v-model="form.arrangeTimeStart"
                value-format="HH:mm:ss"
                placeholder="HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="结束时间">
              <el-time-picker
                v-model="form.arrangeTimeEnd"
                value-format="HH:mm:ss"
                placeholder="HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参与人数">
              <el-input-number v-model="form.participantCount" :min="1" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="安排地点">
              <el-input v-model="form.arrangeAddress" placeholder="安排地点（可选）" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactPerson" placeholder="联系人（可选）" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" placeholder="联系电话（可选）" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="准备事项">
              <el-input v-model="form.prepareItems" type="textarea" :rows="2" placeholder="准备事项 JSON 数组（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="进度记录">
              <el-input v-model="form.progressNotes" type="textarea" :rows="2" placeholder="进度记录（可选）" />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="12">
            <el-form-item label="确认标记">
              <el-select v-model="form.isConfirmed" style="width: 100%">
                <el-option v-for="o in FOLLOWUP_YES_NO_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in ARRANGE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit' && form.status === 4" :span="24">
            <el-form-item label="取消原因">
              <el-input v-model="form.cancelReason" type="textarea" :rows="2" placeholder="取消原因（可选）" />
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

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
