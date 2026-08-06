<script setup lang="ts">
/**
 * 代理人详情页 - 基本信息 tab。
 *
 * 只读展示 AgentInfo 关键字段（el-descriptions），提供"编辑"按钮打开 el-dialog + el-form
 * 修改主表（提交 updateAgent）。字段集与主列表页编辑表单一致。
 *
 * 枚举字段用 el-select + OPTIONS（agentLevel 4 档 / status 3 态 / gender / isCertified）。
 * agentCode/channelCode/clientCount/totalOrderCount/totalOrderAmount 只读展示，
 * 统计字段不进编辑表单。
 */
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getAgent, updateAgent } from '@/api/agent'
import {
  GENDER_OPTIONS,
  AGENT_LEVEL_OPTIONS,
  AGENT_STATUS_OPTIONS,
  CERTIFIED_OPTIONS,
  agentLevelLabel,
  agentLevelTagType,
  agentStatusLabel,
  agentStatusTagType,
  AgentLevel,
  AgentStatus,
  Gender,
  CertifiedFlag
} from '@/types/agent'
import type { AgentInfo } from '@/types/agent'
import RegionSelect from '@/components/RegionSelect.vue'

const props = defineProps<{
  /** 代理人编码（从详情页路由 prop 带入） */
  agentCode: string
}>()

const loading = ref(false)
const agentInfo = ref<AgentInfo | null>(null)

async function loadDetail() {
  if (!props.agentCode) return
  loading.value = true
  try {
    agentInfo.value = await getAgent(props.agentCode)
  } catch {
    agentInfo.value = null
  } finally {
    loading.value = false
  }
}

loadDetail()

// ---------- 辅助渲染 ----------
/** 性别文本（兼容数字直传） */
function genderText(v?: number): string {
  const found = GENDER_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function certifiedLabel(v?: number): string {
  return v === CertifiedFlag.YES ? '已认证' : '未认证'
}

function formatDateTime(s?: string): string {
  if (!s) return '--'
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

// ---------- 编辑弹窗 ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<AgentInfo>({
  agentCode: undefined,
  fullName: '',
  gender: Gender.UNKNOWN,
  avatar: '',
  phone: '',
  email: '',
  idCard: '',
  channelCode: '',
  companyName: '',
  branchName: '',
  department: '',
  position: '',
  employeeNo: '',
  licenseNo: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  serviceIntro: '',
  agentLevel: AgentLevel.NORMAL,
  isCertified: CertifiedFlag.NO,
  status: AgentStatus.NORMAL,
  remark: ''
})

const rules: FormRules<AgentInfo> = {
  fullName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ pattern: /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/, message: '邮箱格式不正确', trigger: 'blur' }]
}

function openEdit() {
  if (!agentInfo.value) return
  Object.assign(form, {
    agentCode: agentInfo.value.agentCode,
    fullName: agentInfo.value.fullName ?? '',
    gender: agentInfo.value.gender ?? Gender.UNKNOWN,
    avatar: agentInfo.value.avatar ?? '',
    phone: agentInfo.value.phone ?? '',
    email: agentInfo.value.email ?? '',
    idCard: agentInfo.value.idCard ?? '',
    channelCode: agentInfo.value.channelCode ?? '',
    companyName: agentInfo.value.companyName ?? '',
    branchName: agentInfo.value.branchName ?? '',
    department: agentInfo.value.department ?? '',
    position: agentInfo.value.position ?? '',
    employeeNo: agentInfo.value.employeeNo ?? '',
    licenseNo: agentInfo.value.licenseNo ?? '',
    provinceCode: agentInfo.value.provinceCode ?? '',
    cityCode: agentInfo.value.cityCode ?? '',
    districtCode: agentInfo.value.districtCode ?? '',
    address: agentInfo.value.address ?? '',
    serviceIntro: agentInfo.value.serviceIntro ?? '',
    agentLevel: agentInfo.value.agentLevel ?? AgentLevel.NORMAL,
    isCertified: agentInfo.value.isCertified ?? CertifiedFlag.NO,
    status: agentInfo.value.status ?? AgentStatus.NORMAL,
    remark: agentInfo.value.remark ?? ''
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (!form.agentCode) return
  submitLoading.value = true
  try {
    await updateAgent(form.agentCode, form)
    ElMessage.success('修改成功')
    dialogVisible.value = false
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

/** 暴露刷新方法，供详情页外部刷新 */
defineExpose({ loadDetail })
</script>

<template>
  <div v-loading="loading">
    <template v-if="agentInfo">
      <div class="basic-toolbar">
        <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑基本信息</el-button>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="代理人编码">{{ agentInfo.agentCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ agentInfo.fullName }}</el-descriptions-item>
        <el-descriptions-item label="所属渠道">{{ agentInfo.channelCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ genderText(agentInfo.gender) }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">{{ agentInfo.phone ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ agentInfo.email ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ agentInfo.idCard ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="保险公司">{{ agentInfo.companyName ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="分公司">{{ agentInfo.branchName ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ agentInfo.department ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="职位">{{ agentInfo.position ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="保险公司工号">{{ agentInfo.employeeNo ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="资格证号">{{ agentInfo.licenseNo ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="代理人等级">
          <el-tag size="small" :type="agentLevelTagType(agentInfo.agentLevel)">
            {{ agentLevelLabel(agentInfo.agentLevel) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="agentStatusTagType(agentInfo.status)" effect="light">
            {{ agentStatusLabel(agentInfo.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="是否认证">{{ certifiedLabel(agentInfo.isCertified) }}</el-descriptions-item>
        <el-descriptions-item label="客户数">{{ agentInfo.clientCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="订单总数">{{ agentInfo.totalOrderCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="订单总金额">{{ agentInfo.totalOrderAmount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="所在地区" :span="3">
          {{ agentInfo.provinceCode }}{{ agentInfo.cityCode }}{{ agentInfo.districtCode }}
        </el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="3">{{ agentInfo.address ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="服务介绍" :span="3">{{ agentInfo.serviceIntro ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(agentInfo.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(agentInfo.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ agentInfo.remark ?? '--' }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <el-empty v-else-if="!loading" description="未加载到代理人信息" />

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑代理人基本信息"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="代理人编码">
              <el-input v-model="form.agentCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属渠道">
              <!-- channelCode 后端取登录上下文填充，UpdateDTO 无此字段，前端只读展示 -->
              <el-input v-model="form.channelCode" disabled placeholder="所属渠道编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="fullName">
              <el-input v-model="form.fullName" placeholder="姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" placeholder="性别" style="width: 100%">
                <el-option v-for="o in GENDER_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="手机号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号">
              <el-input v-model="form.idCard" placeholder="身份证号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保险公司">
              <el-input v-model="form.companyName" placeholder="保险公司名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分公司">
              <el-input v-model="form.branchName" placeholder="分公司名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-input v-model="form.department" placeholder="部门" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位">
              <el-input v-model="form.position" placeholder="职位" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保险公司工号">
              <el-input v-model="form.employeeNo" placeholder="工号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资格证号">
              <el-input v-model="form.licenseNo" placeholder="从业资格证号" maxlength="50" />
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
            <el-form-item label="代理人等级">
              <el-select v-model="form.agentLevel" placeholder="等级" style="width: 100%">
                <el-option v-for="o in AGENT_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否认证">
              <el-select v-model="form.isCertified" style="width: 100%">
                <el-option v-for="o in CERTIFIED_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in AGENT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="服务介绍">
              <el-input v-model="form.serviceIntro" type="textarea" :rows="2" placeholder="服务介绍" />
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
  margin-bottom: 16px;
}
</style>
