<script setup lang="ts">
/**
 * 服务会话详情页 - 基本信息 tab。
 *
 * 只读展示 ServiceSession 关键字段（el-descriptions），提供"编辑"按钮打开 el-dialog + el-form
 * 修改主表（提交 updateSession）。
 *
 * 状态机字段（sessionStatus/acceptTime/completeTime/butlerCode 等）由列表页流转操作控制，
 * 这里编辑表单只放普通描述性字段，对齐列表页编辑表单字段集：
 * serviceTitle / serviceDescription / priority / remark。
 */
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getSession, updateSession } from '@/api/service'
import {
  SESSION_STATUS_OPTIONS,
  SERVICE_TYPE_OPTIONS
} from '@/types/service'
import type { ServiceSession } from '@/types/service'

const props = defineProps<{
  /** 会话编码（从详情页路由 prop 带入） */
  sessionCode: string
}>()

const loading = ref(false)
const sessionInfo = ref<ServiceSession | null>(null)

async function loadDetail() {
  if (!props.sessionCode) return
  loading.value = true
  try {
    sessionInfo.value = await getSession(props.sessionCode)
  } catch {
    sessionInfo.value = null
  } finally {
    loading.value = false
  }
}

loadDetail()

// ---------- 辅助渲染 ----------
function serviceTypeLabel(v?: number): string {
  const found = SERVICE_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function sessionStatusLabel(v?: number): string {
  const found = SESSION_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function formatDateTime(s?: string): string {
  if (!s) return '--'
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

// ---------- 编辑弹窗（普通字段，对齐列表页编辑表单） ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ServiceSession>({
  sessionCode: undefined,
  serviceTitle: '',
  serviceDescription: '',
  priority: undefined,
  remark: ''
})

const rules: FormRules<ServiceSession> = {
  serviceTitle: [{ required: true, message: '请输入服务标题', trigger: 'blur' }]
}

function openEdit() {
  if (!sessionInfo.value) return
  Object.assign(form, {
    sessionCode: sessionInfo.value.sessionCode,
    serviceTitle: sessionInfo.value.serviceTitle ?? '',
    serviceDescription: sessionInfo.value.serviceDescription ?? '',
    priority: sessionInfo.value.priority,
    remark: sessionInfo.value.remark ?? ''
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
  if (!form.sessionCode) return
  submitLoading.value = true
  try {
    await updateSession(form.sessionCode, {
      serviceTitle: form.serviceTitle,
      serviceDescription: form.serviceDescription,
      priority: form.priority,
      remark: form.remark
    })
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
    <template v-if="sessionInfo">
      <div class="basic-toolbar">
        <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑基本信息</el-button>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="会话编码">{{ sessionInfo.sessionCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="服务类型">{{ serviceTypeLabel(sessionInfo.serviceType) }}</el-descriptions-item>
        <el-descriptions-item label="会话状态">
          <el-tag size="small">{{ sessionStatusLabel(sessionInfo.sessionStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="服务标题" :span="2">{{ sessionInfo.serviceTitle || '--' }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ sessionInfo.priority ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="权益编码">{{ sessionInfo.equityCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="客户编码">{{ sessionInfo.clientCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="服务管家">
          {{ sessionInfo.butlerFullName || sessionInfo.butlerCode || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="养老机构">
          {{ sessionInfo.parkFullName || sessionInfo.parkCode || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="代理人">{{ sessionInfo.agentCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ sessionInfo.channelCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="来源类型">{{ sessionInfo.sourceType ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="来源编码">{{ sessionInfo.sourceCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="子状态">{{ sessionInfo.subStatus || '--' }}</el-descriptions-item>
        <el-descriptions-item label="服务描述" :span="3">{{ sessionInfo.serviceDescription || '--' }}</el-descriptions-item>
        <el-descriptions-item label="受理时间">{{ formatDateTime(sessionInfo.acceptTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDateTime(sessionInfo.completeTime) }}</el-descriptions-item>
        <el-descriptions-item label="关闭时间">{{ formatDateTime(sessionInfo.closeTime) }}</el-descriptions-item>
        <el-descriptions-item label="总服务时长(小时)">{{ sessionInfo.totalDuration ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="接触次数">{{ sessionInfo.touchCount ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="综合评分">{{ sessionInfo.overallRating ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="关闭原因" :span="3">{{ sessionInfo.closeReason || '--' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ sessionInfo.remark || '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(sessionInfo.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(sessionInfo.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <el-empty v-else-if="!loading" description="未加载到会话信息" />

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑会话基本信息"
      width="620px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="会话编码">
          <el-input v-model="form.sessionCode" disabled />
        </el-form-item>
        <el-form-item label="服务标题" prop="serviceTitle">
          <el-input v-model="form.serviceTitle" placeholder="服务标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="服务描述">
          <el-input v-model="form.serviceDescription" type="textarea" :rows="3" placeholder="服务描述" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" :max="999" controls-position="right" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="内部备注（可选）" />
        </el-form-item>
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
