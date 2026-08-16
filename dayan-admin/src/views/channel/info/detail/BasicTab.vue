<script setup lang="ts">
/**
 * 渠道详情页 - 基本信息 tab。
 *
 * 只读展示 ChannelInfo 关键字段（el-descriptions），提供"编辑"按钮打开 el-dialog + el-form
 * 修改主表（提交 updateChannel）。字段集对齐主列表页编辑表单。
 *
 * 注：channelCode 服务端生成，编辑时 disabled；状态/审核状态用 el-select。
 */
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getChannel, updateChannel, listChannels } from '@/api/channel'
import { listDistributors } from '@/api/distributor'
import type { DistributorInfo } from '@/types/distributor'
import {
  ChannelType,
  ChannelStatus,
  ChannelAuditStatus,
  CHANNEL_TYPE_OPTIONS,
  CHANNEL_STATUS_OPTIONS,
  CHANNEL_AUDIT_STATUS_OPTIONS,
  CHANNEL_SETTLEMENT_CYCLE_OPTIONS,
  CHANNEL_CAN_MANAGE_OPTIONS,
  buildChannelTree
} from '@/types/channel'
import type { ChannelInfo } from '@/types/channel'
import RegionSelect from '@/components/RegionSelect.vue'
import FileUploader from '@/components/FileUploader/index.vue'
import { formatFileUrl } from '@/utils/file'

const props = defineProps<{
  /** 渠道编码（从详情页路由 prop 带入） */
  channelCode: string
}>()

const loading = ref(false)
const channelInfo = ref<ChannelInfo | null>(null)

async function loadDetail() {
  if (!props.channelCode) return
  loading.value = true
  try {
    channelInfo.value = await getChannel(props.channelCode)
    loadChannelList()
  } catch {
    channelInfo.value = null
  } finally {
    loading.value = false
  }
}

loadDetail()

// ---------- 渠道列表（父级名称映射 + 编辑树数据源） ----------
const channelList = ref<ChannelInfo[]>([])
const parentNameMap = ref<Record<string, string>>({})

async function loadChannelList() {
  try {
    channelList.value = await listChannels()
  } catch {
    channelList.value = []
  }
  const map: Record<string, string> = {}
  for (const c of channelList.value) {
    if (c.channelCode) map[c.channelCode] = c.fullName || c.channelCode
  }
  parentNameMap.value = map
}

/** 上级渠道展示名（无父级显示顶级） */
function parentName(code?: string | null): string {
  if (!code) return '（顶级）'
  return parentNameMap.value[code] || code
}

// ---------- 辅助渲染 ----------
function channelTypeLabel(v?: number): string {
  const found = CHANNEL_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function channelStatusLabel(v?: number): string {
  const found = CHANNEL_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function auditStatusLabel(v?: number): string {
  const found = CHANNEL_AUDIT_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}

function settlementCycleLabel(v?: number): string {
  const found = CHANNEL_SETTLEMENT_CYCLE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

function canManageLabel(v?: number): string {
  const found = CHANNEL_CAN_MANAGE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function levelLabel(v?: number): string {
  if (v === 1) return '一级'
  if (v === 2) return '二级'
  if (v === 3) return '三级'
  return v != null ? String(v) : '--'
}

function formatAmount(v?: number): string {
  return v != null
    ? Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '--'
}

// ---------- 编辑弹窗 ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ChannelInfo>({
  channelCode: undefined,
  fullName: '',
  shortName: '',
  channelType: ChannelType.INSURANCE,
  parentCode: null,
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
  distributorCode: '',
  settlementCycle: undefined,
  cooperationStartDate: undefined,
  canManage: 0,
  sortOrder: 0,
  status: ChannelStatus.PENDING,
  auditStatus: ChannelAuditStatus.PENDING,
  logoUrl: '',
  featureConfig: '',
  description: '',
  remark: ''
})

/** 上级渠道树（编辑时排除自身），复用 loadChannelList 的列表数据 */
const parentTreeOptions = ref<ChannelInfo[]>([])

function buildParentTree(excludeCode?: string): ChannelInfo[] {
  const tree = buildChannelTree(channelList.value)
  if (!excludeCode) return tree
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

/** 分销商下拉选项 */
const distributorOptions = ref<DistributorInfo[]>([])

async function loadDistributors() {
  try {
    distributorOptions.value = await listDistributors()
  } catch {
    distributorOptions.value = []
  }
}

// 不带泛型：ChannelInfo.children 自引用会触发 FormRules 映射类型的循环引用（TS2615）
const rules: FormRules = {
  fullName: [{ required: true, message: '请输入渠道全称', trigger: 'blur' }]
}

function openEdit() {
  if (!channelInfo.value) return
  Object.assign(form, {
    channelCode: channelInfo.value.channelCode,
    fullName: channelInfo.value.fullName ?? '',
    shortName: channelInfo.value.shortName ?? '',
    channelType: channelInfo.value.channelType,
    parentCode: channelInfo.value.parentCode ?? null,
    level: channelInfo.value.level,
    unifiedCreditCode: channelInfo.value.unifiedCreditCode ?? '',
    legalPerson: channelInfo.value.legalPerson ?? '',
    provinceCode: channelInfo.value.provinceCode ?? '',
    cityCode: channelInfo.value.cityCode ?? '',
    districtCode: channelInfo.value.districtCode ?? '',
    address: channelInfo.value.address ?? '',
    contactPerson: channelInfo.value.contactPerson ?? '',
    contactPhone: channelInfo.value.contactPhone ?? '',
    contactEmail: channelInfo.value.contactEmail ?? '',
    distributorCode: channelInfo.value.distributorCode ?? '',
    settlementCycle: channelInfo.value.settlementCycle,
    cooperationStartDate: channelInfo.value.cooperationStartDate,
    canManage: channelInfo.value.canManage ?? 0,
    sortOrder: channelInfo.value.sortOrder ?? 0,
    status: channelInfo.value.status ?? ChannelStatus.PENDING,
    auditStatus: channelInfo.value.auditStatus,
    logoUrl: channelInfo.value.logoUrl ?? '',
    featureConfig: channelInfo.value.featureConfig ?? '',
    description: channelInfo.value.description ?? '',
    remark: channelInfo.value.remark ?? ''
  })
  loadChannelList().then(() => {
    parentTreeOptions.value = buildParentTree(channelInfo.value?.channelCode)
  })
  loadDistributors()
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (!form.channelCode) return
  submitLoading.value = true
  try {
    await updateChannel(form.channelCode, form)
    ElMessage.success('修改成功')
    dialogVisible.value = false
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ loadDetail })
</script>

<template>
  <div v-loading="loading">
    <template v-if="channelInfo">
      <div class="basic-toolbar">
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑基本信息</el-button>
        </div>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="渠道编码">{{ channelInfo.channelCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="渠道全称">{{ channelInfo.fullName }}</el-descriptions-item>
        <el-descriptions-item label="渠道简称">{{ channelInfo.shortName ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="渠道类型">{{ channelTypeLabel(channelInfo.channelType) }}</el-descriptions-item>
        <el-descriptions-item label="上级渠道">{{ parentName(channelInfo.parentCode) }}</el-descriptions-item>
        <el-descriptions-item label="层级">{{ levelLabel(channelInfo.level) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ channelStatusLabel(channelInfo.status) }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">{{ auditStatusLabel(channelInfo.auditStatus) }}</el-descriptions-item>
        <el-descriptions-item label="排序号">{{ channelInfo.sortOrder ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="统一社会信用代码">{{ channelInfo.unifiedCreditCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="法人代表">{{ channelInfo.legalPerson ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="分销商编码">{{ channelInfo.distributorCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="所在地区" :span="3">
          {{ channelInfo.provinceCode ?? '--' }} / {{ channelInfo.cityCode ?? '--' }} / {{ channelInfo.districtCode ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="3">{{ channelInfo.address ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ channelInfo.contactPerson ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ channelInfo.contactPhone ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系邮箱">{{ channelInfo.contactEmail ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="结算周期">{{ settlementCycleLabel(channelInfo.settlementCycle) }}</el-descriptions-item>
        <el-descriptions-item label="旗下代理人">{{ channelInfo.agentCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="合作开始日期">{{ formatDate(channelInfo.cooperationStartDate) }}</el-descriptions-item>
        <el-descriptions-item label="累计订单金额">{{ formatAmount(channelInfo.totalOrderAmount) }}</el-descriptions-item>
        <el-descriptions-item label="管理配置能力">{{ canManageLabel(channelInfo.canManage) }}</el-descriptions-item>
        <el-descriptions-item label="Logo">
          <el-image
            v-if="channelInfo.logoUrl"
            :src="formatFileUrl(channelInfo.logoUrl)"
            :preview-src-list="[formatFileUrl(channelInfo.logoUrl)]"
            fit="cover"
            style="width: 60px; height: 60px"
          />
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="功能开关配置" :span="3">{{ channelInfo.featureConfig ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="渠道描述" :span="3">{{ channelInfo.description ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ channelInfo.remark ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(channelInfo.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDate(channelInfo.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <el-empty v-else-if="!loading" description="未加载到渠道信息" />

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑渠道基本信息"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="渠道编码">
              <el-input v-model="form.channelCode" disabled />
            </el-form-item>
          </el-col>
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
            <el-form-item label="渠道类型">
              <el-select v-model="form.channelType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in CHANNEL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
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
            <el-form-item label="层级">
              <el-input
                :model-value="levelLabel(form.level)"
                disabled
                placeholder="由上级渠道自动计算"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="统一信用代码">
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
          <el-col :span="8">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in CHANNEL_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="审核状态">
              <el-select v-model="form.auditStatus" style="width: 100%">
                <el-option v-for="o in CHANNEL_AUDIT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Logo">
              <FileUploader v-model="form.logoUrl" type="image" module="channel" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="功能开关配置">
              <el-input
                v-model="form.featureConfig"
                type="textarea"
                :rows="2"
                placeholder='JSON 格式，如 {"enableAgent": true, "enableClient": true}'
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="渠道描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="渠道描述" />
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
.basic-toolbar {
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
</style>
