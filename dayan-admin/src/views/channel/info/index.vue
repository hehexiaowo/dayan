<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listChannels,
  treeChannels,
  getChannel,
  createChannel,
  updateChannel,
  deleteChannel,
  auditChannel
} from '@/api/channel'
import { listDistributors } from '@/api/distributor'
import {
  ChannelStatus,
  CHANNEL_STATUS_OPTIONS,
  CHANNEL_SETTLEMENT_CYCLE_OPTIONS,
  CHANNEL_CAN_MANAGE_OPTIONS,
  ChannelType,
  CHANNEL_TYPE_OPTIONS,
  ChannelAuditStatus,
  CHANNEL_AUDIT_STATUS_OPTIONS,
  buildChannelTree,
  type ChannelInfo,
  type ChannelInfoQuery
} from '@/types/channel'
import type { DistributorInfo } from '@/types/distributor'
import RegionSelect from '@/components/RegionSelect.vue'
import FileUploader from '@/components/FileUploader/index.vue'

/**
 * 渠道管理页（树形表格）。
 *
 * - 后端 /channels/tree 返回树形结构，前端直接展示；
 * - 列表查询用 /channels（返回平铺 List 非 PageResult），由 buildChannelTree 组树展示；
 * - 主键 channelCode 由服务端生成，新增表单不含 channelCode；
 * - 不用 useCrud（因 list 返回 List 非 PageResult），直接 ref + loadTree 管理。
 */

const loading = ref(false)
/** 树形展示数据（默认走 /tree 接口） */
const treeData = ref<ChannelInfo[]>([])
/** 平铺数据（用于组父级树选项） */
const flatList = ref<ChannelInfo[]>([])

const query = reactive<ChannelInfoQuery>({
  fullName: '',
  channelType: undefined,
  status: undefined,
  auditStatus: undefined,
  distributorCode: ''
})

/** 分销商下拉选项 + 名称映射（后端 VO 不带 distributorName，前端自行映射） */
const distributorOptions = ref<DistributorInfo[]>([])
const distributorNameMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  for (const d of distributorOptions.value) {
    if (d.distributorCode) map[d.distributorCode] = d.fullName || d.shortName || d.distributorCode
  }
  return map
})

async function loadDistributors() {
  try {
    distributorOptions.value = await listDistributors()
  } catch {
    distributorOptions.value = []
  }
}

async function loadTree() {
  loading.value = true
  try {
    // 同时拉平铺列表（用于父级选择）与树数据
    const [list, tree] = await Promise.all([listChannels(query), treeChannels(query)])
    flatList.value = list
    treeData.value = tree
  } catch {
    flatList.value = []
    treeData.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadTree()
}

const router = useRouter()

/** 跳转渠道详情页 */
function goDetail(row: ChannelInfo) {
  if (!row.channelCode) return
  router.push({ name: 'ChannelDetail', params: { channelCode: row.channelCode } })
}

function handleReset() {
  query.fullName = ''
  query.channelType = undefined
  query.status = undefined
  query.auditStatus = undefined
  query.distributorCode = ''
  loadTree()
}

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ChannelInfo>({
  channelCode: undefined,
  fullName: '',
  shortName: '',
  channelType: ChannelType.INSURANCE,
  parentCode: null,
  ancestors: '',
  level: undefined,
  unifiedCreditCode: '',
  legalPerson: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  logoUrl: '',
  description: '',
  distributorCode: '',
  settlementCycle: undefined,
  cooperationStartDate: undefined,
  canManage: 0,
  sortOrder: 0,
  status: ChannelStatus.PENDING,
  auditStatus: ChannelAuditStatus.PENDING,
  remark: ''
})

// 注：不使用 FormRules<ChannelInfo>，因 ChannelInfo.children 自引用会触发循环类型推导
const rules: FormRules = {
  fullName: [{ required: true, message: '请输入渠道全称', trigger: 'blur' }],
  channelType: [{ required: true, message: '请选择渠道类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

/** 供父级选择用的树（基于平铺列表组树） */
const parentTreeOptions = ref<ChannelInfo[]>([])

function buildParentOptions(excludeCode?: string): ChannelInfo[] {
  const tree = buildChannelTree(flatList.value)
  if (excludeCode) {
    const filterNode = (nodes: ChannelInfo[]): ChannelInfo[] => {
      const result: ChannelInfo[] = []
      for (const n of nodes) {
        if (n.channelCode === excludeCode) continue
        const children = n.children ? filterNode(n.children) : undefined
        result.push(children ? { ...n, children } : { ...n, children: undefined })
      }
      return result
    }
    return filterNode(tree)
  }
  return tree
}

function resetForm() {
  Object.assign(form, {
    channelCode: undefined,
    fullName: '',
    shortName: '',
    channelType: ChannelType.INSURANCE,
    parentCode: null,
    ancestors: '',
    level: undefined,
    unifiedCreditCode: '',
    legalPerson: '',
    provinceCode: '',
    cityCode: '',
    districtCode: '',
    address: '',
    contactPerson: '',
    contactPhone: '',
    contactEmail: '',
    logoUrl: '',
    description: '',
    distributorCode: '',
    settlementCycle: undefined,
    cooperationStartDate: undefined,
    canManage: 0,
    sortOrder: 0,
    status: ChannelStatus.PENDING,
    auditStatus: ChannelAuditStatus.PENDING,
    remark: ''
  })
}

function openCreate(parent?: ChannelInfo) {
  dialogType.value = 'create'
  resetForm()
  if (parent) {
    form.parentCode = parent.channelCode ?? null
  }
  parentTreeOptions.value = buildParentOptions()
  dialogVisible.value = true
}

async function openEdit(row: ChannelInfo) {
  if (!row.channelCode) return
  dialogType.value = 'edit'
  resetForm()
  parentTreeOptions.value = buildParentOptions(row.channelCode)
  try {
    const detail = await getChannel(row.channelCode)
    Object.assign(form, {
      channelCode: detail.channelCode,
      fullName: detail.fullName ?? '',
      shortName: detail.shortName ?? '',
      channelType: detail.channelType ?? ChannelType.INSURANCE,
      parentCode: detail.parentCode ?? null,
      ancestors: detail.ancestors ?? '',
      level: detail.level,
      unifiedCreditCode: detail.unifiedCreditCode ?? '',
      legalPerson: detail.legalPerson ?? '',
      provinceCode: detail.provinceCode ?? '',
      cityCode: detail.cityCode ?? '',
      districtCode: detail.districtCode ?? '',
      address: detail.address ?? '',
      contactPerson: detail.contactPerson ?? '',
      contactPhone: detail.contactPhone ?? '',
      contactEmail: detail.contactEmail ?? '',
      logoUrl: detail.logoUrl ?? '',
      description: detail.description ?? '',
      distributorCode: detail.distributorCode ?? '',
      settlementCycle: detail.settlementCycle,
      cooperationStartDate: detail.cooperationStartDate,
      canManage: detail.canManage ?? 0,
      sortOrder: detail.sortOrder ?? 0,
      status: detail.status ?? ChannelStatus.PENDING,
      auditStatus: detail.auditStatus ?? ChannelAuditStatus.PENDING,
      remark: detail.remark ?? ''
    })
  } catch {
    Object.assign(form, {
      channelCode: row.channelCode,
      fullName: row.fullName ?? '',
      shortName: row.shortName ?? '',
      channelType: row.channelType ?? ChannelType.INSURANCE,
      parentCode: row.parentCode ?? null,
      ancestors: row.ancestors ?? '',
      level: row.level,
      unifiedCreditCode: row.unifiedCreditCode ?? '',
      legalPerson: row.legalPerson ?? '',
      provinceCode: row.provinceCode ?? '',
      cityCode: row.cityCode ?? '',
      districtCode: row.districtCode ?? '',
      address: row.address ?? '',
      contactPerson: row.contactPerson ?? '',
      contactPhone: row.contactPhone ?? '',
      contactEmail: row.contactEmail ?? '',
      logoUrl: row.logoUrl ?? '',
      description: row.description ?? '',
      distributorCode: row.distributorCode ?? '',
      settlementCycle: row.settlementCycle,
      cooperationStartDate: row.cooperationStartDate,
      canManage: row.canManage ?? 0,
      sortOrder: row.sortOrder ?? 0,
      status: row.status ?? ChannelStatus.PENDING,
      auditStatus: row.auditStatus ?? ChannelAuditStatus.PENDING,
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
      await createChannel(form)
      ElMessage.success('新增成功')
    } else if (form.channelCode) {
      await updateChannel(form.channelCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadTree()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: ChannelInfo) {
  if (!row.channelCode) return
  await ElMessageBox.confirm(`确定删除渠道「${row.fullName}」吗？若存在子渠道将一并影响。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteChannel(row.channelCode)
  ElMessage.success('删除成功')
  loadTree()
}

// ---------- 辅助渲染 ----------
function channelTypeLabel(t?: number): string {
  const found = CHANNEL_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function channelTypeTagType(t?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (t) {
    case ChannelType.INSURANCE:
      return 'danger'
    case ChannelType.BANK:
      return 'warning'
    case ChannelType.INTERMEDIARY:
      return 'success'
    case ChannelType.ENTERPRISE:
      return 'info'
    default:
      return 'info'
  }
}

function auditStatusLabel(s?: number): string {
  const found = CHANNEL_AUDIT_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

function auditStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case ChannelAuditStatus.PASS:
      return 'success'
    case ChannelAuditStatus.PENDING:
      return 'warning'
    case ChannelAuditStatus.REJECT:
      return 'danger'
    default:
      return 'info'
  }
}

function statusLabel(s?: number): string {
  const found = CHANNEL_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

function statusTagType(s?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (s) {
    case ChannelStatus.PENDING:
      return 'warning'
    case ChannelStatus.COOPERATING:
      return 'success'
    case ChannelStatus.PAUSED:
      return 'danger'
    case ChannelStatus.TERMINATED:
      return 'info'
    default:
      return 'info'
  }
}

// ---------- 审核流 ----------
const auditDialogVisible = ref(false)
const auditSubmitLoading = ref(false)
const auditForm = reactive<{ channelCode: string; fullName: string; auditStatus: number; auditRemark: string }>({
  channelCode: '',
  fullName: '',
  // 1=通过 / 2=驳回（对齐后端 ChannelAuditDTO），默认通过
  auditStatus: 1,
  auditRemark: ''
})

function openAudit(row: ChannelInfo) {
  if (!row.channelCode) return
  auditForm.channelCode = row.channelCode
  auditForm.fullName = row.fullName ?? ''
  auditForm.auditStatus = 1
  auditForm.auditRemark = ''
  auditDialogVisible.value = true
}

async function handleAuditSubmit() {
  auditSubmitLoading.value = true
  try {
    await auditChannel({
      channelCode: auditForm.channelCode,
      auditStatus: auditForm.auditStatus,
      auditRemark: auditForm.auditRemark || undefined
    })
    ElMessage.success(auditForm.auditStatus === 1 ? '已通过' : '已驳回')
    auditDialogVisible.value = false
    loadTree()
  } finally {
    auditSubmitLoading.value = false
  }
}

// 初始化加载
onMounted(() => {
  loadDistributors()
  loadTree()
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input
          v-model="query.fullName"
          placeholder="渠道全称关键字"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.channelType" placeholder="渠道类型" clearable style="width: 140px">
          <el-option v-for="o in CHANNEL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="o in CHANNEL_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.auditStatus" placeholder="审核状态" clearable style="width: 120px">
          <el-option v-for="o in CHANNEL_AUDIT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.distributorCode" placeholder="分销商" clearable filterable style="width: 200px">
          <el-option
            v-for="d in distributorOptions"
            :key="d.distributorCode"
            :label="d.fullName || d.shortName || d.distributorCode"
            :value="d.distributorCode!"
          />
        </el-select>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
          <el-button :icon="'Refresh'" @click="loadTree">刷新</el-button>
          <el-button type="primary" :icon="'Plus'" @click="openCreate()">新增渠道</el-button>
        </div>
      </div>
    </el-card>

    <!-- 树形表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">渠道列表</span>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="treeData"
        border
        stripe
        row-key="channelCode"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="fullName" label="渠道全称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="channelCode" label="渠道编码" min-width="140" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="channelTypeTagType(row.channelType)">
              {{ channelTypeLabel(row.channelType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.contactPerson || '--' }}</template>
        </el-table-column>
        <el-table-column prop="agentCount" label="队伍数量" width="90" align="center" />
        <el-table-column label="分销商" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ distributorNameMap[row.distributorCode] || row.distributorCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="合作状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="auditStatusTagType(row.auditStatus)">
              {{ auditStatusLabel(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
            <el-button v-if="row.auditStatus === ChannelAuditStatus.PENDING" link type="warning" size="small" @click="openAudit(row)">审核</el-button>
            <el-button link type="primary" size="small" @click="openCreate(row)">新增子级</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增渠道' : '编辑渠道'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="渠道全称" prop="fullName">
              <el-input v-model="form.fullName" placeholder="渠道全称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="渠道简称">
              <el-input v-model="form.shortName" placeholder="渠道简称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上级渠道">
              <el-tree-select
                v-model="form.parentCode"
                :data="parentTreeOptions"
                :props="{ label: 'fullName', value: 'channelCode', children: 'children' }"
                check-strictly
                clearable
                placeholder="顶级（不选）"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="渠道类型" prop="channelType">
              <el-select v-model="form.channelType" placeholder="渠道类型" style="width: 100%">
                <el-option v-for="o in CHANNEL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="社会信用代码">
              <el-input v-model="form.unifiedCreditCode" placeholder="统一社会信用代码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="法人代表">
              <el-input v-model="form.legalPerson" placeholder="法人代表" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所在地区">
              <RegionSelect
                v-model:province-code="form.provinceCode"
                v-model:city-code="form.cityCode"
                v-model:district-code="form.districtCode"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址">
              <el-input v-model="form.address" placeholder="详细地址" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人">
              <el-input v-model="form.contactPerson" placeholder="联系人" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" placeholder="联系电话" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系邮箱">
              <el-input v-model="form.contactEmail" placeholder="联系邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分销商">
              <el-select v-model="form.distributorCode" placeholder="选择分销商" clearable filterable style="width: 100%">
                <el-option
                  v-for="d in distributorOptions"
                  :key="d.distributorCode"
                  :label="d.fullName || d.shortName || d.distributorCode"
                  :value="d.distributorCode!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结算周期">
              <el-select v-model="form.settlementCycle" clearable placeholder="请选择" style="width: 100%">
                <el-option v-for="o in CHANNEL_SETTLEMENT_CYCLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合作开始日期">
              <el-date-picker
                v-model="form.cooperationStartDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="合作开始日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管理配置能力">
              <el-select v-model="form.canManage" style="width: 100%">
                <el-option v-for="o in CHANNEL_CAN_MANAGE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Logo">
              <FileUploader v-model="form.logoUrl" type="image" module="channel" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in CHANNEL_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="渠道描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="渠道描述" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
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
    <el-dialog v-model="auditDialogVisible" title="渠道审核" width="520px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="渠道">
          <span>{{ auditForm.fullName }}</span>
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

  .card-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2329;
  }
}
</style>
