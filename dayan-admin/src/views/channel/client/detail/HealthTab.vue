<script setup lang="ts">
/**
 * 客户详情页 - 健康档案 tab。
 *
 * 数据模式：单条（一客户一档案）。GET /health-profiles/{clientCode} 返回单条或 null；
 * 保存（新增/编辑都）走 POST /health-profiles（upsert：存在则更新，不存在则新增）。
 * 删除走 DELETE /health-profiles/{clientCode}。
 *
 * 关键约束：
 * - 后端无 update 端点，编辑用 POST（saveOrUpdate 语义）。
 * - lastAssessmentTime 由后端自动设为 now()，表单只读展示，不编辑。
 * - 枚举字段（bloodType/mobilityLevel/cognitiveLevel/mentalStatus/sleepQuality）按 DDL
 *   09_client.sql 注释用 el-select（选项见下方本地常量）。
 * - height/weight/bloodSugar/healthScore 是 BigDecimal/数值，用 el-input-number（精度按需）。
 */
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getHealthProfile, saveHealthProfile, deleteHealthProfile } from '@/api/client-sub'
import type { ClientHealthProfile } from '@/types/client'

const props = defineProps<{
  /** 客户编码（路由参数） */
  clientCode: string
}>()

// ---------- 加载单条档案 ----------
const loading = ref(false)
const profile = ref<ClientHealthProfile | null>(null)

async function loadProfile() {
  if (!props.clientCode) return
  loading.value = true
  try {
    profile.value = await getHealthProfile(props.clientCode)
  } catch {
    profile.value = null
  } finally {
    loading.value = false
  }
}

loadProfile()

// ---------- 编辑弹窗（upsert） ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)

const form = reactive<ClientHealthProfile>({
  id: undefined,
  clientCode: '',
  height: undefined,
  weight: undefined,
  bloodType: undefined,
  bloodPressure: '',
  bloodSugar: undefined,
  heartRate: undefined,
  chronicDiseases: '',
  allergyHistory: '',
  surgeryHistory: '',
  familyHistory: '',
  medicationInfo: '',
  mobilityLevel: undefined,
  cognitiveLevel: undefined,
  mentalStatus: undefined,
  dietPreference: '',
  sleepQuality: undefined,
  emergencyContactName: '',
  emergencyContactPhone: '',
  emergencyContactRelation: '',
  healthScore: undefined,
  remark: ''
})

/**
 * JSON 字符串数组 <-> 编辑态数组适配（避免终端用户手写 JSON）。
 * 后端存储为 JSON 数组字符串，编辑时以标签列表呈现，回车/逗号即可新增。
 */
function jsonListAdapter(model: Record<string, unknown>, key: string) {
  return computed<string[]>({
    get: () => {
      const raw = model[key]
      if (!raw) return []
      try {
        const parsed = JSON.parse(String(raw))
        return Array.isArray(parsed) ? parsed.map(String) : [String(raw)]
      } catch {
        // 历史脏数据可能是普通字符串，按单值处理
        return [String(raw)]
      }
    },
    set: (arr: string[]) => {
      model[key] = arr.length ? JSON.stringify(arr) : ''
    }
  })
}

const chronicDiseasesList = jsonListAdapter(form, 'chronicDiseases')
const allergyHistoryList = jsonListAdapter(form, 'allergyHistory')
const surgeryHistoryList = jsonListAdapter(form, 'surgeryHistory')
const familyHistoryList = jsonListAdapter(form, 'familyHistory')
const medicationInfoList = jsonListAdapter(form, 'medicationInfo')

function resetForm() {
  Object.assign(form, {
    id: undefined,
    clientCode: '',
    height: undefined,
    weight: undefined,
    bloodType: undefined,
    bloodPressure: '',
    bloodSugar: undefined,
    heartRate: undefined,
    chronicDiseases: '',
    allergyHistory: '',
    surgeryHistory: '',
    familyHistory: '',
    medicationInfo: '',
    mobilityLevel: undefined,
    cognitiveLevel: undefined,
    mentalStatus: undefined,
    dietPreference: '',
    sleepQuality: undefined,
    emergencyContactName: '',
    emergencyContactPhone: '',
    emergencyContactRelation: '',
    healthScore: undefined,
    remark: ''
  })
}

function openEdit() {
  resetForm()
  if (profile.value) {
    // 编辑：回显已有档案
    Object.assign(form, profile.value)
  } else {
    // 新建：仅带 clientCode
    form.clientCode = props.clientCode
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  // 确保 clientCode 从上下文带入（编辑时也不可改）
  form.clientCode = props.clientCode
  submitLoading.value = true
  try {
    await saveHealthProfile(form)
    ElMessage.success(profile.value ? '保存成功' : '创建成功')
    dialogVisible.value = false
    await loadProfile()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete() {
  await ElMessageBox.confirm('确定删除该客户的健康档案吗？此操作不可恢复。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteHealthProfile(props.clientCode)
  ElMessage.success('删除成功')
  await loadProfile()
}

// ---------- 枚举选项（按 DDL 09_client.sql 注释） ----------
/** 血型：1=A, 2=B, 3=AB, 4=O */
const BLOOD_TYPE_OPTIONS = [
  { label: 'A', value: 1 },
  { label: 'B', value: 2 },
  { label: 'AB', value: 3 },
  { label: 'O', value: 4 }
] as const

/** 行动能力：1=完全自理, 2=部分自理, 3=需要协助, 4=完全依赖 */
const MOBILITY_LEVEL_OPTIONS = [
  { label: '完全自理', value: 1 },
  { label: '部分自理', value: 2 },
  { label: '需要协助', value: 3 },
  { label: '完全依赖', value: 4 }
] as const

/** 认知能力：1=正常, 2=轻度障碍, 3=中度障碍, 4=重度障碍 */
const COGNITIVE_LEVEL_OPTIONS = [
  { label: '正常', value: 1 },
  { label: '轻度障碍', value: 2 },
  { label: '中度障碍', value: 3 },
  { label: '重度障碍', value: 4 }
] as const

/** 心理状态：1=良好, 2=一般, 3=需关注 */
const MENTAL_STATUS_OPTIONS = [
  { label: '良好', value: 1 },
  { label: '一般', value: 2 },
  { label: '需关注', value: 3 }
] as const

/** 睡眠质量：1=良好, 2=一般, 3=较差 */
const SLEEP_QUALITY_OPTIONS = [
  { label: '良好', value: 1 },
  { label: '一般', value: 2 },
  { label: '较差', value: 3 }
] as const

// ---------- 辅助渲染 ----------
function formatDateTime(s?: string): string {
  if (!s) return '--'
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

function numText(v?: number): string {
  return v != null ? String(v) : '--'
}

/** 选项查找 label（兼容数字直传） */
function optionLabel(options: readonly { label: string; value: number }[], v?: number): string {
  const found = options.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

defineExpose({ loadProfile })
</script>

<template>
  <div v-loading="loading">
    <template v-if="profile">
      <div class="toolbar">
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑健康档案</el-button>
          <el-button type="danger" :icon="'Delete'" plain @click="handleDelete">删除档案</el-button>
        </div>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="身高(cm)">{{ numText(profile.height) }}</el-descriptions-item>
        <el-descriptions-item label="体重(kg)">{{ numText(profile.weight) }}</el-descriptions-item>
        <el-descriptions-item label="血型">{{ optionLabel(BLOOD_TYPE_OPTIONS, profile.bloodType) }}</el-descriptions-item>
        <el-descriptions-item label="血压">{{ profile.bloodPressure || '--' }}</el-descriptions-item>
        <el-descriptions-item label="血糖">{{ numText(profile.bloodSugar) }}</el-descriptions-item>
        <el-descriptions-item label="心率">{{ numText(profile.heartRate) }}</el-descriptions-item>
        <el-descriptions-item label="行动能力">{{ optionLabel(MOBILITY_LEVEL_OPTIONS, profile.mobilityLevel) }}</el-descriptions-item>
        <el-descriptions-item label="认知等级">{{ optionLabel(COGNITIVE_LEVEL_OPTIONS, profile.cognitiveLevel) }}</el-descriptions-item>
        <el-descriptions-item label="精神状态">{{ optionLabel(MENTAL_STATUS_OPTIONS, profile.mentalStatus) }}</el-descriptions-item>
        <el-descriptions-item label="睡眠质量">{{ optionLabel(SLEEP_QUALITY_OPTIONS, profile.sleepQuality) }}</el-descriptions-item>
        <el-descriptions-item label="健康评分">{{ numText(profile.healthScore) }}</el-descriptions-item>
        <el-descriptions-item label="饮食偏好" :span="3">{{ profile.dietPreference || '--' }}</el-descriptions-item>
        <el-descriptions-item label="慢性病" :span="3">{{ profile.chronicDiseases || '--' }}</el-descriptions-item>
        <el-descriptions-item label="过敏史" :span="3">{{ profile.allergyHistory || '--' }}</el-descriptions-item>
        <el-descriptions-item label="手术史" :span="3">{{ profile.surgeryHistory || '--' }}</el-descriptions-item>
        <el-descriptions-item label="家族病史" :span="3">{{ profile.familyHistory || '--' }}</el-descriptions-item>
        <el-descriptions-item label="当前用药" :span="3">{{ profile.medicationInfo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="紧急联系人">{{ profile.emergencyContactName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="紧急联系电话">{{ profile.emergencyContactPhone || '--' }}</el-descriptions-item>
        <el-descriptions-item label="紧急联系人关系">{{ profile.emergencyContactRelation || '--' }}</el-descriptions-item>
        <el-descriptions-item label="最近评估时间">{{ formatDateTime(profile.lastAssessmentTime) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(profile.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ profile.remark || '--' }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <template v-else>
      <el-empty description="该客户暂无健康档案">
        <el-button type="primary" :icon="'Plus'" @click="openEdit">创建健康档案</el-button>
      </el-empty>
    </template>

    <!-- 编辑/新建弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="profile ? '编辑健康档案' : '创建健康档案'"
      width="900px"
      :close-on-click-modal="false"
    >
      <el-alert
        v-if="!profile"
        type="info"
        :closable="false"
        title="保存为 saveOrUpdate：若已存在将更新，否则新增。"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-form :model="form" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="身高(cm)">
              <el-input-number v-model="form.height" :min="0" :precision="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="体重(kg)">
              <el-input-number v-model="form.weight" :min="0" :precision="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="血型">
              <el-select v-model="form.bloodType" placeholder="选择血型" clearable style="width: 100%">
                <el-option v-for="o in BLOOD_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="血压">
              <el-input v-model="form.bloodPressure" placeholder="如 120/80" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="血糖">
              <el-input-number v-model="form.bloodSugar" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="心率">
              <el-input-number v-model="form.heartRate" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="行动能力">
              <el-select v-model="form.mobilityLevel" placeholder="选择行动能力" clearable style="width: 100%">
                <el-option v-for="o in MOBILITY_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="认知等级">
              <el-select v-model="form.cognitiveLevel" placeholder="选择认知等级" clearable style="width: 100%">
                <el-option v-for="o in COGNITIVE_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="精神状态">
              <el-select v-model="form.mentalStatus" placeholder="选择精神状态" clearable style="width: 100%">
                <el-option v-for="o in MENTAL_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="睡眠质量">
              <el-select v-model="form.sleepQuality" placeholder="选择睡眠质量" clearable style="width: 100%">
                <el-option v-for="o in SLEEP_QUALITY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="健康评分">
              <el-input-number v-model="form.healthScore" :min="0" :max="100" :precision="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="饮食偏好">
              <el-input v-model="form.dietPreference" placeholder="饮食偏好" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="慢性病">
              <el-select
                v-model="chronicDiseasesList"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入后回车添加，如：高血压"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="过敏史">
              <el-select
                v-model="allergyHistoryList"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入后回车添加"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="手术史">
              <el-select
                v-model="surgeryHistoryList"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入后回车添加"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="家族病史">
              <el-select
                v-model="familyHistoryList"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入后回车添加"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="当前用药">
              <el-select
                v-model="medicationInfoList"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入后回车添加，如：阿司匹林"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="紧急联系人">
              <el-input v-model="form.emergencyContactName" placeholder="姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="紧急电话">
              <el-input v-model="form.emergencyContactPhone" placeholder="电话" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人关系">
              <el-input v-model="form.emergencyContactRelation" placeholder="如 子女" maxlength="50" />
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
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
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
}
.toolbar .toolbar-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}
</style>
