<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageScenes,
  getScene,
  createScene,
  updateScene,
  deleteScene,
  submitScene,
  auditScene,
  shelvesScene,
  offshelvesScene,
  reshelvesScene,
  fullScene
} from '@/api/scene'
import type { SceneInfo, SceneInfoQuery } from '@/types/scene'
import {
  SceneType,
  SceneStatus,
  AuditStatus,
  SCENE_TYPE_OPTIONS,
  SCENE_STATUS_OPTIONS,
  AUDIT_STATUS_OPTIONS
} from '@/types/scene'

/**
 * 场景活动管理页。
 *
 * - 标准 CRUD（搜索 + 表格 + 分页 + 新增/编辑弹窗）；
 * - 审核流：提交审核 / 审核（通过或驳回）/ 上架 / 下架 / 重新上架 / 满期。
 *   操作按钮按 sceneStatus + auditStatus 组合动态显示。
 *
 * 状态约定：
 * - sceneStatus：0草稿 / 1已上架 / 2已下架 / 3已满期
 * - auditStatus：0待审核 / 1通过 / 2驳回
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
} = useCrud<SceneInfo, SceneInfoQuery>(
  { page: pageScenes },
  {
    initialQuery: {
      sceneName: '',
      sceneType: undefined,
      sceneStatus: undefined
    }
  }
)

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<SceneInfo>({
  sceneCode: undefined,
  sceneName: '',
  sceneType: SceneType.VISIT,
  parkCode: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  sceneDescription: '',
  coverImage: '',
  capacity: undefined,
  durationHours: undefined,
  minPerson: undefined,
  maxPerson: undefined,
  originalPrice: undefined,
  salePrice: undefined,
  priceUnit: '',
  isFree: 0,
  targetAudience: '',
  highlight: '',
  notice: '',
  sortOrder: 0,
  remark: ''
})

const rules: FormRules<SceneInfo> = {
  sceneName: [{ required: true, message: '请输入场景名称', trigger: 'blur' }],
  sceneType: [{ required: true, message: '请选择场景类型', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    sceneCode: undefined,
    sceneName: '',
    sceneType: SceneType.VISIT,
    parkCode: '',
    provinceCode: '',
    cityCode: '',
    districtCode: '',
    address: '',
    sceneDescription: '',
    coverImage: '',
    capacity: undefined,
    durationHours: undefined,
    minPerson: undefined,
    maxPerson: undefined,
    originalPrice: undefined,
    salePrice: undefined,
    priceUnit: '',
    isFree: 0,
    targetAudience: '',
    highlight: '',
    notice: '',
    sortOrder: 0,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: SceneInfo) {
  if (!row.sceneCode) return
  dialogType.value = 'edit'
  resetForm()
  try {
    const detail = await getScene(row.sceneCode)
    Object.assign(form, {
      sceneCode: detail.sceneCode,
      sceneName: detail.sceneName ?? '',
      sceneType: detail.sceneType ?? SceneType.VISIT,
      parkCode: detail.parkCode ?? '',
      provinceCode: detail.provinceCode ?? '',
      cityCode: detail.cityCode ?? '',
      districtCode: detail.districtCode ?? '',
      address: detail.address ?? '',
      sceneDescription: detail.sceneDescription ?? '',
      coverImage: detail.coverImage ?? '',
      capacity: detail.capacity,
      durationHours: detail.durationHours,
      minPerson: detail.minPerson,
      maxPerson: detail.maxPerson,
      originalPrice: detail.originalPrice,
      salePrice: detail.salePrice,
      priceUnit: detail.priceUnit ?? '',
      isFree: detail.isFree ?? 0,
      targetAudience: detail.targetAudience ?? '',
      highlight: detail.highlight ?? '',
      notice: detail.notice ?? '',
      sortOrder: detail.sortOrder ?? 0,
      remark: detail.remark ?? ''
    })
  } catch {
    // 拉取详情失败时回退到行数据
    Object.assign(form, {
      sceneCode: row.sceneCode,
      sceneName: row.sceneName ?? '',
      sceneType: row.sceneType ?? SceneType.VISIT,
      parkCode: row.parkCode ?? '',
      provinceCode: row.provinceCode ?? '',
      cityCode: row.cityCode ?? '',
      districtCode: row.districtCode ?? '',
      address: row.address ?? '',
      sceneDescription: row.sceneDescription ?? '',
      coverImage: row.coverImage ?? '',
      capacity: row.capacity,
      durationHours: row.durationHours,
      minPerson: row.minPerson,
      maxPerson: row.maxPerson,
      originalPrice: row.originalPrice,
      salePrice: row.salePrice,
      priceUnit: row.priceUnit ?? '',
      isFree: row.isFree ?? 0,
      targetAudience: row.targetAudience ?? '',
      highlight: row.highlight ?? '',
      notice: row.notice ?? '',
      sortOrder: row.sortOrder ?? 0,
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

  submitLoading.value = true
  try {
    if (dialogType.value === 'create') {
      await createScene(form)
      ElMessage.success('新增成功')
    } else if (form.sceneCode) {
      await updateScene(form.sceneCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

function handleReset() {
  query.sceneName = ''
  query.sceneType = undefined
  query.sceneStatus = undefined
  handleSearch()
}

// ---------- 审核流操作 ----------
async function handleSubmitAudit(row: SceneInfo) {
  if (!row.sceneCode) return
  await ElMessageBox.confirm(`确定提交「${row.sceneName}」进入审核吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await submitScene(row.sceneCode)
  ElMessage.success('已提交审核')
  loadPage()
}

async function handleShelves(row: SceneInfo) {
  if (!row.sceneCode) return
  await ElMessageBox.confirm(`确定上架「${row.sceneName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await shelvesScene(row.sceneCode)
  ElMessage.success('上架成功')
  loadPage()
}

async function handleOffshelves(row: SceneInfo) {
  if (!row.sceneCode) return
  await ElMessageBox.confirm(`确定下架「${row.sceneName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await offshelvesScene(row.sceneCode)
  ElMessage.success('已下架')
  loadPage()
}

async function handleReshelves(row: SceneInfo) {
  if (!row.sceneCode) return
  await ElMessageBox.confirm(`确定重新上架「${row.sceneName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await reshelvesScene(row.sceneCode)
  ElMessage.success('重新上架成功')
  loadPage()
}

async function handleFull(row: SceneInfo) {
  if (!row.sceneCode) return
  await ElMessageBox.confirm(`确定将「${row.sceneName}」标记为满期吗？（活动到期或名额约满）`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await fullScene(row.sceneCode)
  ElMessage.success('已标记满期')
  loadPage()
}

async function handleDeleteRow(row: SceneInfo) {
  if (!row.sceneCode) return
  await ElMessageBox.confirm(`确定删除「${row.sceneName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteScene(row.sceneCode)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 审核弹窗 ----------
const auditDialogVisible = ref(false)
const auditSubmitLoading = ref(false)
const auditForm = reactive<{ sceneCode: string; sceneName: string; auditStatus: number; auditRemark: string }>({
  sceneCode: '',
  sceneName: '',
  // 1=通过 / 2=驳回（对齐后端 SceneInfoAuditDTO，与 content 的 2/3 不同），默认通过
  auditStatus: AuditStatus.PASS,
  auditRemark: ''
})

function openAudit(row: SceneInfo) {
  if (!row.sceneCode) return
  auditForm.sceneCode = row.sceneCode
  auditForm.sceneName = row.sceneName ?? ''
  auditForm.auditStatus = AuditStatus.PASS
  auditForm.auditRemark = ''
  auditDialogVisible.value = true
}

async function handleAuditSubmit() {
  auditSubmitLoading.value = true
  try {
    await auditScene({
      sceneCode: auditForm.sceneCode,
      auditStatus: auditForm.auditStatus,
      auditRemark: auditForm.auditRemark || undefined
    })
    ElMessage.success(auditForm.auditStatus === AuditStatus.PASS ? '已通过' : '已驳回')
    auditDialogVisible.value = false
    loadPage()
  } finally {
    auditSubmitLoading.value = false
  }
}

// ---------- 辅助渲染 ----------
function sceneTypeLabel(t?: number): string {
  const found = SCENE_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function sceneStatusLabel(s?: number): string {
  const found = SCENE_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

function auditStatusLabel(s?: number): string {
  const found = AUDIT_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

/** 根据场景状态返回 el-tag type：草稿info/上架success/下架warning/满期danger。 */
function sceneStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case SceneStatus.PUBLISHED:
      return 'success'
    case SceneStatus.OFFLINE:
      return 'warning'
    case SceneStatus.FULL:
      return 'danger'
    case SceneStatus.DRAFT:
    default:
      return 'info'
  }
}

/** 根据审核状态返回 el-tag type。 */
function auditStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case AuditStatus.PASS:
      return 'success'
    case AuditStatus.PENDING:
      return 'warning'
    case AuditStatus.REJECT:
      return 'danger'
    default:
      return 'info'
  }
}

/**
 * 价格显示：isFree=1 显示「免费」，否则显示 salePrice + 单位。
 */
function priceLabel(row: SceneInfo): string {
  if (row.isFree === 1) return '免费'
  if (row.salePrice == null) return '--'
  return row.priceUnit ? `${row.salePrice} ${row.priceUnit}` : String(row.salePrice)
}

const router = useRouter()
function goDetail(row: SceneInfo) {
  if (!row.sceneCode) return
  router.push({ name: 'SceneDetail', params: { sceneCode: row.sceneCode } })
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="场景名称">
          <el-input v-model="query.sceneName" placeholder="场景名称关键字" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="场景类型">
          <el-select v-model="query.sceneType" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="o in SCENE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="场景状态">
          <el-select v-model="query.sceneStatus" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in SCENE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>场景列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增场景</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="sceneCode">
        <el-table-column prop="sceneCode" label="场景编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="sceneName" label="场景名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sceneType" label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ sceneTypeLabel(row.sceneType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="parkCode" label="机构" min-width="120" show-overflow-tooltip />
        <el-table-column label="价格" width="120" align="center">
          <template #default="{ row }">
            {{ priceLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="capacity" label="容量" width="80" align="center" />
        <el-table-column prop="bookCount" label="预约数" width="90" align="center" />
        <el-table-column prop="sceneStatus" label="场景状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="sceneStatusTagType(row.sceneStatus)">
              {{ sceneStatusLabel(row.sceneStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditStatus" label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="auditStatusTagType(row.auditStatus)">
              {{ auditStatusLabel(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.sceneStatus === SceneStatus.DRAFT && row.auditStatus === AuditStatus.PENDING"
              link
              type="warning"
              size="small"
              @click="handleSubmitAudit(row)"
            >
              提交审核
            </el-button>
            <el-button
              v-if="row.sceneStatus === SceneStatus.DRAFT && row.auditStatus === AuditStatus.PENDING"
              link
              type="primary"
              size="small"
              @click="openAudit(row)"
            >
              审核
            </el-button>
            <el-button
              v-if="row.sceneStatus === SceneStatus.DRAFT && row.auditStatus === AuditStatus.PASS"
              link
              type="success"
              size="small"
              @click="handleShelves(row)"
            >
              上架
            </el-button>
            <el-button
              v-if="row.sceneStatus === SceneStatus.PUBLISHED"
              link
              type="warning"
              size="small"
              @click="handleOffshelves(row)"
            >
              下架
            </el-button>
            <el-button
              v-if="row.sceneStatus === SceneStatus.PUBLISHED"
              link
              type="danger"
              size="small"
              @click="handleFull(row)"
            >
              满期
            </el-button>
            <el-button
              v-if="row.sceneStatus === SceneStatus.OFFLINE"
              link
              type="success"
              size="small"
              @click="handleReshelves(row)"
            >
              重新上架
            </el-button>
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增场景' : '编辑场景'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="场景名称" prop="sceneName">
              <el-input v-model="form.sceneName" placeholder="场景名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景类型" prop="sceneType">
              <el-select v-model="form.sceneType" placeholder="场景类型" style="width: 100%">
                <el-option v-for="o in SCENE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联机构">
              <el-input v-model="form.parkCode" placeholder="养老机构编码（parkCode）" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="省级编码">
              <el-input v-model="form.provinceCode" placeholder="省级编码" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="市级编码">
              <el-input v-model="form.cityCode" placeholder="市级编码" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区县编码">
              <el-input v-model="form.districtCode" placeholder="区县编码" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="form.address" placeholder="详细地址" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="场景描述">
              <el-input v-model="form.sceneDescription" type="textarea" :rows="3" placeholder="场景描述" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <el-input v-model="form.coverImage" placeholder="封面图 URL" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="容量">
              <el-input-number v-model="form.capacity" :min="0" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时长(小时)">
              <el-input-number v-model="form.durationHours" :min="0" :max="9999" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最少人数">
              <el-input-number v-model="form.minPerson" :min="0" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最多人数">
              <el-input-number v-model="form.maxPerson" :min="0" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否免费">
              <el-switch v-model="form.isFree" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原价">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="售价">
              <el-input-number v-model="form.salePrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="价格单位">
              <el-input v-model="form.priceUnit" placeholder="元/人 等" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="目标人群">
              <el-input v-model="form.targetAudience" placeholder="目标人群" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="亮点">
              <el-input v-model="form.highlight" type="textarea" :rows="2" placeholder="活动亮点" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="须知">
              <el-input v-model="form.notice" type="textarea" :rows="2" placeholder="参与须知" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="内部备注（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditDialogVisible" title="场景审核" width="520px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="场景名称">
          <span>{{ auditForm.sceneName }}</span>
        </el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" placeholder="审核备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditSubmitLoading" @click="handleAuditSubmit">确定</el-button>
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
