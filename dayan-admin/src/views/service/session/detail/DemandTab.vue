<script setup lang="ts">
/**
 * 服务会话详情页 - 权益需求 tab。
 *
 * 架构（useCrud 分页 + 弹窗 CRUD）：
 * 1. 搜索条（demandType + status）+ 新增按钮
 * 2. 主表格：useCrud（idKey:'id', fixedParams:{sessionCode}）分页加载
 * 3. 新增/编辑 el-dialog：demandCode 服务端生成（编辑 disabled），status create 时固定 0
 *
 * 红线遵守：
 * - 主键 id 雪花 Long，useCrud 传 idKey:'id'（number，非业务 code）
 * - demandCode 服务端生成（DM 前缀），新增时不展示，编辑 disabled
 * - demandType / contactPreference / collectMethod / status 用 el-select + OPTIONS
 * - budgetMin/Max/usePersonAge/careLevelNeed 才用 el-input-number
 * - status create 时由后端固定为 0，表单不展示
 */
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageServiceEquityDemands,
  createServiceEquityDemand,
  updateServiceEquityDemand,
  deleteServiceEquityDemand
} from '@/api/service-sub'
import {
  DEMAND_TYPE_OPTIONS,
  CONTACT_PREFERENCE_OPTIONS,
  COLLECT_METHOD_OPTIONS,
  DEMAND_STATUS_OPTIONS
} from '@/types/service'
import { GENDER_OPTIONS } from '@/types/client'
import type { ServiceEquityDemand, ServiceEquityDemandQuery } from '@/types/service'
import FileUploader from '@/components/FileUploader/index.vue'

const props = defineProps<{
  sessionCode: string
  /** 会话客户编码（从会话详情带入，新增时回填，客户端不可改） */
  clientCode?: string
}>()

// ---------- 列表（useCrud，主键 demandCode） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ServiceEquityDemand,
  ServiceEquityDemandQuery
>(
  {
    page: pageServiceEquityDemands,
    create: createServiceEquityDemand,
    update: (code, data) => updateServiceEquityDemand(code, data),
    remove: deleteServiceEquityDemand
  },
  {
    initialQuery: { demandType: undefined, status: undefined },
    idKey: 'demandCode',
    fixedParams: { sessionCode: props.sessionCode }
  }
)

loadPage()

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ServiceEquityDemand>({
  id: undefined,
  sessionCode: '',
  demandCode: '',
  clientCode: '',
  butlerCode: '',
  demandType: 1,
  usePersonName: '',
  usePersonAge: undefined,
  usePersonGender: undefined,
  healthSummary: '',
  careLevelNeed: undefined,
  cityPreference: '',
  areaPreference: '',
  budgetMin: undefined,
  budgetMax: undefined,
  roomPreference: '',
  foodPreference: '',
  specialNeeds: '',
  expectedTime: '',
  contactPreference: 1,
  collectMethod: 1,
  demandSummary: '',
  demandImages: '',
  status: 0,
  remark: ''
})

const rules: FormRules<ServiceEquityDemand> = {
  demandType: [{ required: true, message: '请选择需求类型', trigger: 'change' }],
  usePersonName: [{ required: true, message: '请输入使用人姓名', trigger: 'blur' }],
  contactPreference: [{ required: true, message: '请选择联系偏好', trigger: 'change' }],
  collectMethod: [{ required: true, message: '请选择收集方式', trigger: 'change' }]
}

/** demandImages：后端是 string（JSON 数组），FileUploader 多图用 string[] */
const demandImagesModel = computed<string[]>({
  get() {
    const raw = form.demandImages
    if (!raw) return []
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) return parsed.filter((x) => typeof x === 'string')
    } catch {
      // 非 JSON，按逗号分隔
    }
    return raw.split(',').map((s) => s.trim()).filter(Boolean)
  },
  set(val: string[]) {
    form.demandImages = val.length > 0 ? JSON.stringify(val) : ''
  }
})

function resetForm() {
  Object.assign(form, {
    id: undefined,
    sessionCode: '',
    demandCode: '',
    clientCode: '',
    butlerCode: '',
    demandType: 1,
    usePersonName: '',
    usePersonAge: undefined,
    usePersonGender: undefined,
    healthSummary: '',
    careLevelNeed: undefined,
    cityPreference: '',
    areaPreference: '',
    budgetMin: undefined,
    budgetMax: undefined,
    roomPreference: '',
    foodPreference: '',
    specialNeeds: '',
    expectedTime: '',
    contactPreference: 1,
    collectMethod: 1,
    demandSummary: '',
    demandImages: '',
    status: 0,
    remark: ''
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.sessionCode = props.sessionCode
  // clientCode 由会话详情带入（否则新增提交空串被后端 400 拒绝）
  form.clientCode = props.clientCode ?? ''
  dialogVisible.value = true
}

function openEdit(row: ServiceEquityDemand) {
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
  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createServiceEquityDemand(form)
      ElMessage.success('新增成功')
    } else if (form.demandCode) {
      await updateServiceEquityDemand(form.demandCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ServiceEquityDemand) {
  if (!row.demandCode) return
  await ElMessageBox.confirm(`确定删除需求「${row.demandCode}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteServiceEquityDemand(row.demandCode)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function demandTypeLabel(v?: number): string {
  const found = DEMAND_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function contactPreferenceLabel(v?: number): string {
  const found = CONTACT_PREFERENCE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function collectMethodLabel(v?: number): string {
  const found = COLLECT_METHOD_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function statusLabel(v?: number): string {
  const found = DEMAND_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function statusTagType(v?: number): 'info' | 'warning' | 'success' {
  if (v === 2) return 'success'
  if (v === 1) return 'warning'
  return 'info'
}

function formatDateTime(s?: string): string {
  if (!s) return '--'
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

defineExpose({ loadPage })
</script>

<template>
  <div class="demand-tab">
    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-select v-model="query.demandType" placeholder="需求类型" clearable style="width: 140px">
        <el-option v-for="o in DEMAND_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option v-for="o in DEMAND_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增需求</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="demandCode">
      <el-table-column prop="demandCode" label="需求编码" min-width="150" show-overflow-tooltip />
      <el-table-column prop="demandType" label="类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ demandTypeLabel(row.demandType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="usePersonName" label="使用人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="usePersonAge" label="年龄" width="80" align="center">
        <template #default="{ row }">{{ row.usePersonAge != null ? row.usePersonAge : '--' }}</template>
      </el-table-column>
      <el-table-column label="预算" min-width="140" align="center">
        <template #default="{ row }">
          <span v-if="row.budgetMin != null || row.budgetMax != null">
            {{ row.budgetMin ?? '*' }} ~ {{ row.budgetMax ?? '*' }}
          </span>
          <span v-else>--</span>
        </template>
      </el-table-column>
      <el-table-column prop="contactPreference" label="联系偏好" width="100" align="center">
        <template #default="{ row }">{{ contactPreferenceLabel(row.contactPreference) }}</template>
      </el-table-column>
      <el-table-column prop="collectMethod" label="收集方式" width="110" align="center">
        <template #default="{ row }">{{ collectMethodLabel(row.collectMethod) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="demandSummary" label="需求摘要" min-width="200" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="140" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
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
      :title="dialogMode === 'create' ? '新增需求' : '编辑需求'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户编码">
              <el-input v-model="form.clientCode" disabled placeholder="会话客户编码" />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="12">
            <el-form-item label="需求编码">
              <el-input v-model="form.demandCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="需求类型" prop="demandType">
              <el-select v-model="form.demandType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in DEMAND_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="使用人姓名" prop="usePersonName">
              <el-input v-model="form.usePersonName" placeholder="使用人姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="年龄">
              <el-input-number v-model="form.usePersonAge" :min="0" :max="150" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="性别">
              <el-select v-model="form.usePersonGender" placeholder="未知" clearable style="width: 100%">
                <el-option v-for="o in GENDER_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="护理等级">
              <el-input-number v-model="form.careLevelNeed" :min="0" :max="9" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预算下限">
              <el-input-number v-model="form.budgetMin" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预算上限">
              <el-input-number v-model="form.budgetMax" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系偏好" prop="contactPreference">
              <el-select v-model="form.contactPreference" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in CONTACT_PREFERENCE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收集方式" prop="collectMethod">
              <el-select v-model="form.collectMethod" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in COLLECT_METHOD_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期望时间">
              <el-date-picker
                v-model="form.expectedTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="城市偏好">
              <el-input v-model="form.cityPreference" placeholder="JSON 数组（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="区域偏好">
              <el-input v-model="form.areaPreference" placeholder="JSON 数组（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房间偏好">
              <el-input v-model="form.roomPreference" placeholder="JSON 数组（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="健康状况">
              <el-input v-model="form.healthSummary" type="textarea" :rows="2" placeholder="健康状况摘要" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="饮食偏好">
              <el-input v-model="form.foodPreference" type="textarea" :rows="2" placeholder="饮食偏好（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="特殊需求">
              <el-input v-model="form.specialNeeds" type="textarea" :rows="2" placeholder="特殊需求（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="需求摘要">
              <el-input v-model="form.demandSummary" type="textarea" :rows="3" placeholder="需求摘要" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="需求图片">
              <FileUploader v-model="demandImagesModel" type="image" multiple module="service" />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="24">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 160px">
                <el-option v-for="o in DEMAND_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
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
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
