<script setup lang="ts">
/**
 * 客户详情页 - 基本信息 tab。
 *
 * 只读展示 ClientInfo 关键字段（el-descriptions），提供"编辑"按钮打开 el-dialog + el-form
 * 修改主表（提交 updateClient）。字段集与主列表页编辑表单一致。
 *
 * 枚举字段用 el-select + OPTIONS（status 3 态 / clientLevel 3 档 / gender / education / isVip）。
 */
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getClient, updateClient } from '@/api/client'
import { listChannels } from '@/api/channel'
import {
  GENDER_OPTIONS,
  CLIENT_LEVEL_OPTIONS,
  CLIENT_STATUS_OPTIONS,
  VIP_OPTIONS,
  EDUCATION_OPTIONS,
  clientLevelLabel,
  clientLevelTagType,
  clientStatusLabel,
  clientStatusTagType,
  ClientStatus,
  ClientLevel,
  Gender,
  VipFlag
} from '@/types/client'
import type { ClientInfo } from '@/types/client'
import { buildChannelTree, type ChannelInfo } from '@/types/channel'
import RegionSelect from '@/components/RegionSelect.vue'

const props = defineProps<{
  /** 客户编码（从详情页路由 prop 带入） */
  clientCode: string
}>()

const loading = ref(false)
const clientInfo = ref<ClientInfo | null>(null)

async function loadDetail() {
  if (!props.clientCode) return
  loading.value = true
  try {
    clientInfo.value = await getClient(props.clientCode)
  } catch {
    clientInfo.value = null
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

function educationLabel(e?: number): string {
  const found = EDUCATION_OPTIONS.find((o) => o.value === e)
  return found ? found.label : e != null ? String(e) : '--'
}

function vipLabel(v?: number): string {
  return v === VipFlag.YES ? '是' : '否'
}

function formatDate(s?: string): string {
  if (!s) return '--'
  // 截取 YYYY-MM-DD（后端返回完整时间戳时只取日期部分）
  return s.length >= 10 ? s.slice(0, 10) : s
}

// ---------- 渠道树（编辑表单的所属渠道选择） ----------
const channelTree = ref<ChannelInfo[]>([])
async function loadChannelTree() {
  try {
    const list = await listChannels()
    channelTree.value = buildChannelTree(list)
  } catch {
    channelTree.value = []
  }
}

// ---------- 编辑弹窗 ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ClientInfo>({
  clientCode: undefined,
  channelCode: '',
  fullName: '',
  gender: Gender.UNKNOWN,
  avatar: '',
  birthday: '',
  idCard: '',
  phone: '',
  email: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  education: undefined,
  clientLevel: ClientLevel.NORMAL,
  isVip: VipFlag.NO,
  status: ClientStatus.ENABLED,
  remark: ''
})

const rules: FormRules<ClientInfo> = {
  fullName: [{ required: true, message: '请输入客户姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ pattern: /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/, message: '邮箱格式不正确', trigger: 'blur' }]
}

function openEdit() {
  if (!clientInfo.value) return
  Object.assign(form, {
    clientCode: clientInfo.value.clientCode,
    channelCode: clientInfo.value.channelCode ?? '',
    fullName: clientInfo.value.fullName ?? '',
    gender: clientInfo.value.gender ?? Gender.UNKNOWN,
    avatar: clientInfo.value.avatar ?? '',
    birthday: clientInfo.value.birthday ?? '',
    idCard: clientInfo.value.idCard ?? '',
    phone: clientInfo.value.phone ?? '',
    email: clientInfo.value.email ?? '',
    provinceCode: clientInfo.value.provinceCode ?? '',
    cityCode: clientInfo.value.cityCode ?? '',
    districtCode: clientInfo.value.districtCode ?? '',
    address: clientInfo.value.address ?? '',
    education: clientInfo.value.education,
    clientLevel: clientInfo.value.clientLevel ?? ClientLevel.NORMAL,
    isVip: clientInfo.value.isVip ?? VipFlag.NO,
    status: clientInfo.value.status ?? ClientStatus.ENABLED,
    remark: clientInfo.value.remark ?? ''
  })
  loadChannelTree()
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (!form.clientCode) return
  submitLoading.value = true
  try {
    // channelCode 由后端登录上下文填充，ClientInfoUpdateDTO 无该字段，提交前剔除
    const { channelCode, ...payload } = form
    await updateClient(form.clientCode, payload)
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
    <template v-if="clientInfo">
      <div class="basic-toolbar">
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑基本信息</el-button>
        </div>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="客户编码">{{ clientInfo.clientCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ clientInfo.fullName }}</el-descriptions-item>
        <el-descriptions-item label="所属渠道">{{ clientInfo.channelCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ genderText(clientInfo.gender) }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">{{ clientInfo.phone ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ clientInfo.email ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="生日">{{ formatDate(clientInfo.birthday) }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ clientInfo.age ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ clientInfo.idCard ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="学历">{{ educationLabel(clientInfo.education) }}</el-descriptions-item>
        <el-descriptions-item label="客户等级">
          <el-tag size="small" :type="clientLevelTagType(clientInfo.clientLevel)">
            {{ clientLevelLabel(clientInfo.clientLevel) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="clientStatusTagType(clientInfo.status)" effect="light">
            {{ clientStatusLabel(clientInfo.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="是否 VIP">{{ vipLabel(clientInfo.isVip) }}</el-descriptions-item>
        <el-descriptions-item label="权益数量">{{ clientInfo.equityCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="服务次数">{{ clientInfo.serviceCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="所在地区" :span="3">
          {{ clientInfo.provinceCode }}{{ clientInfo.cityCode }}{{ clientInfo.districtCode }}
        </el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="3">{{ clientInfo.address ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(clientInfo.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ clientInfo.remark ?? '--' }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <el-empty v-else-if="!loading" description="未加载到客户信息" />

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑客户基本信息"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户编码">
              <el-input v-model="form.clientCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属渠道">
              <!-- channelCode 后端取登录上下文填充，UpdateDTO 无此字段，只读展示 -->
              <el-tree-select
                v-model="form.channelCode"
                :data="channelTree"
                :props="{ label: 'fullName', value: 'channelCode', children: 'children' }"
                check-strictly
                clearable
                disabled
                placeholder="选择渠道"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="fullName">
              <el-input v-model="form.fullName" placeholder="客户姓名" maxlength="50" />
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
            <el-form-item label="性别">
              <el-select v-model="form.gender" placeholder="性别" style="width: 100%">
                <el-option v-for="o in GENDER_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生日">
              <el-date-picker
                v-model="form.birthday"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择生日"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号">
              <el-input v-model="form.idCard" placeholder="身份证号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学历">
              <el-select v-model="form.education" placeholder="学历" clearable style="width: 100%">
                <el-option v-for="o in EDUCATION_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
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
            <el-form-item label="客户等级">
              <el-select v-model="form.clientLevel" style="width: 100%">
                <el-option v-for="o in CLIENT_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否 VIP">
              <el-select v-model="form.isVip" style="width: 100%">
                <el-option v-for="o in VIP_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in CLIENT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
