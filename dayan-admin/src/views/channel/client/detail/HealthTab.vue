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
 * - 枚举字段（bloodType/mobilityLevel/cognitiveLevel/mentalStatus/sleepQuality）后端无 @Schema 文档，
 *   暂用 el-input-number 兜底 + TODO 注释。
 * - height/weight/bloodSugar/healthScore 是 BigDecimal/数值，用 el-input-number（精度按需）。
 */
import { reactive, ref } from 'vue'
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

// ---------- 辅助渲染 ----------
function formatDateTime(s?: string): string {
  if (!s) return '--'
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

function numText(v?: number): string {
  return v != null ? String(v) : '--'
}

defineExpose({ loadProfile })
</script>

<template>
  <div v-loading="loading">
    <template v-if="profile">
      <div class="toolbar">
        <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑健康档案</el-button>
        <el-button type="danger" :icon="'Delete'" plain @click="handleDelete">删除档案</el-button>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="身高(cm)">{{ numText(profile.height) }}</el-descriptions-item>
        <el-descriptions-item label="体重(kg)">{{ numText(profile.weight) }}</el-descriptions-item>
        <!-- TODO: bloodType 枚举值待后端补 @Schema 文档后改为 select -->
        <el-descriptions-item label="血型">{{ numText(profile.bloodType) }}</el-descriptions-item>
        <el-descriptions-item label="血压">{{ profile.bloodPressure || '--' }}</el-descriptions-item>
        <el-descriptions-item label="血糖">{{ numText(profile.bloodSugar) }}</el-descriptions-item>
        <el-descriptions-item label="心率">{{ numText(profile.heartRate) }}</el-descriptions-item>
        <!-- TODO: mobilityLevel 枚举值待后端补 @Schema 文档后改为 select -->
        <el-descriptions-item label="行动能力">{{ numText(profile.mobilityLevel) }}</el-descriptions-item>
        <!-- TODO: cognitiveLevel 枚举值待后端补 @Schema 文档后改为 select -->
        <el-descriptions-item label="认知等级">{{ numText(profile.cognitiveLevel) }}</el-descriptions-item>
        <!-- TODO: mentalStatus 枚举值待后端补 @Schema 文档后改为 select -->
        <el-descriptions-item label="精神状态">{{ numText(profile.mentalStatus) }}</el-descriptions-item>
        <!-- TODO: sleepQuality 枚举值待后端补 @Schema 文档后改为 select -->
        <el-descriptions-item label="睡眠质量">{{ numText(profile.sleepQuality) }}</el-descriptions-item>
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
            <!-- TODO: bloodType 枚举值待后端补 @Schema 文档后改为 select -->
            <el-form-item label="血型">
              <el-input-number v-model="form.bloodType" :min="0" controls-position="right" style="width: 100%" />
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
            <!-- TODO: mobilityLevel 枚举值待后端补 @Schema 文档后改为 select -->
            <el-form-item label="行动能力">
              <el-input-number v-model="form.mobilityLevel" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <!-- TODO: cognitiveLevel 枚举值待后端补 @Schema 文档后改为 select -->
            <el-form-item label="认知等级">
              <el-input-number v-model="form.cognitiveLevel" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <!-- TODO: mentalStatus 枚举值待后端补 @Schema 文档后改为 select -->
            <el-form-item label="精神状态">
              <el-input-number v-model="form.mentalStatus" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <!-- TODO: sleepQuality 枚举值待后端补 @Schema 文档后改为 select -->
            <el-form-item label="睡眠质量">
              <el-input-number v-model="form.sleepQuality" :min="0" controls-position="right" style="width: 100%" />
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
              <el-input v-model="form.chronicDiseases" type="textarea" :rows="2" placeholder="JSON 字符串，如 [&quot;高血压&quot;]" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="过敏史">
              <el-input v-model="form.allergyHistory" type="textarea" :rows="2" placeholder="JSON 字符串" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="手术史">
              <el-input v-model="form.surgeryHistory" type="textarea" :rows="2" placeholder="JSON 字符串" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="家族病史">
              <el-input v-model="form.familyHistory" type="textarea" :rows="2" placeholder="JSON 字符串" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="当前用药">
              <el-input v-model="form.medicationInfo" type="textarea" :rows="2" placeholder="JSON 字符串" />
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
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}
</style>
