<script setup lang="ts">
/**
 * 服务会话详情页 - 服务评价 tab（1:1 一会话一评价）。
 *
 * 与其他分页子表不同，评价是 1:1 关系（业务约束），前端采用「有则编辑无则新增」模式：
 * 进入 tab 时调用 list?sessionCode=xxx，返回数组至多 1 条：
 * - 若有：展示只读卡片 + 编辑/删除按钮，编辑时 PUT /{id}。
 * - 若无：展示空状态 + 新增按钮，新增时 POST。
 *
 * 后端 create 校验已存在则抛业务异常（不依赖前端拦截）。
 *
 * 主键 id 雪花 Long（无业务 code），路径参数用 id。
 * 4 维评分均 1-5，用 el-rate。
 */
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  listServiceEvaluations,
  createServiceEvaluation,
  updateServiceEvaluation,
  deleteServiceEvaluation
} from '@/api/service-sub'
import {
  EVALUATION_STATUS_OPTIONS,
  EVALUATION_IS_ANONYMOUS_OPTIONS
} from '@/types/service'
import type { ServiceEvaluation } from '@/types/service'
import FileUploader from '@/components/FileUploader/index.vue'

const props = defineProps<{
  sessionCode: string
}>()

const loading = ref(false)
const evaluation = ref<ServiceEvaluation | null>(null)

async function loadEvaluation() {
  loading.value = true
  try {
    const list = await listServiceEvaluations({ sessionCode: props.sessionCode })
    evaluation.value = list && list.length > 0 ? list[0] : null
  } catch {
    evaluation.value = null
  } finally {
    loading.value = false
  }
}

loadEvaluation()

// ---------- 辅助渲染 ----------
function statusLabel(v?: number): string {
  const found = EVALUATION_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function statusTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}

function anonymousLabel(v?: number): string {
  const found = EVALUATION_IS_ANONYMOUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function formatDateTime(s?: string): string {
  if (!s) return '--'
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ServiceEvaluation>({
  id: undefined,
  sessionCode: '',
  attitudeRating: 0,
  professionalRating: 0,
  responsivenessRating: 0,
  satisfactionRating: 0,
  content: '',
  imageUrls: '',
  isAnonymous: 0,
  replyContent: '',
  status: 1
})

const rules: FormRules<ServiceEvaluation> = {
  content: [{ required: true, message: '请输入评价内容', trigger: 'blur' }]
}

/** imageUrls：后端是 string（JSON 数组），FileUploader 多图用 string[] */
const imageUrlsModel = computed<string[]>({
  get() {
    const raw = form.imageUrls
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
    form.imageUrls = val.length > 0 ? JSON.stringify(val) : ''
  }
})

function resetForm() {
  Object.assign(form, {
    id: undefined,
    sessionCode: '',
    attitudeRating: 0,
    professionalRating: 0,
    responsivenessRating: 0,
    satisfactionRating: 0,
    content: '',
    imageUrls: '',
    isAnonymous: 0,
    replyContent: '',
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.sessionCode = props.sessionCode
  dialogVisible.value = true
}

function openEdit() {
  if (!evaluation.value) return
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: evaluation.value.id,
    sessionCode: props.sessionCode,
    attitudeRating: evaluation.value.attitudeRating ?? 0,
    professionalRating: evaluation.value.professionalRating ?? 0,
    responsivenessRating: evaluation.value.responsivenessRating ?? 0,
    satisfactionRating: evaluation.value.satisfactionRating ?? 0,
    content: evaluation.value.content ?? '',
    imageUrls: evaluation.value.imageUrls ?? '',
    isAnonymous: evaluation.value.isAnonymous ?? 0,
    replyContent: evaluation.value.replyContent ?? '',
    status: evaluation.value.status ?? 1
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
  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createServiceEvaluation(form)
      ElMessage.success('评价已提交')
    } else if (form.id) {
      await updateServiceEvaluation(form.id, {
        attitudeRating: form.attitudeRating,
        professionalRating: form.professionalRating,
        responsivenessRating: form.responsivenessRating,
        satisfactionRating: form.satisfactionRating,
        content: form.content,
        imageUrls: form.imageUrls,
        isAnonymous: form.isAnonymous,
        replyContent: form.replyContent,
        status: form.status
      })
      ElMessage.success('评价已更新')
    }
    dialogVisible.value = false
    await loadEvaluation()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete() {
  if (!evaluation.value?.id) return
  await ElMessageBox.confirm('确定删除该会话的评价吗？删除后可重新提交。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteServiceEvaluation(evaluation.value.id)
  ElMessage.success('删除成功')
  await loadEvaluation()
}

defineExpose({ loadEvaluation })
</script>

<template>
  <div v-loading="loading">
    <!-- 已有评价：只读卡片 + 编辑/删除 -->
    <template v-if="evaluation">
      <div class="eval-toolbar">
        <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑评价</el-button>
        <el-button type="danger" :icon="'Delete'" @click="handleDelete">删除评价</el-button>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="评价ID">{{ evaluation.id ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(evaluation.status)" size="small">
            {{ statusLabel(evaluation.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="是否匿名">{{ anonymousLabel(evaluation.isAnonymous) }}</el-descriptions-item>
        <el-descriptions-item label="服务态度">
          <el-rate :model-value="evaluation.attitudeRating ?? 0" disabled />
        </el-descriptions-item>
        <el-descriptions-item label="专业度">
          <el-rate :model-value="evaluation.professionalRating ?? 0" disabled />
        </el-descriptions-item>
        <el-descriptions-item label="响应速度">
          <el-rate :model-value="evaluation.responsivenessRating ?? 0" disabled />
        </el-descriptions-item>
        <el-descriptions-item label="满意度">
          <el-rate :model-value="evaluation.satisfactionRating ?? 0" disabled />
        </el-descriptions-item>
        <el-descriptions-item label="评价图片（JSON）" :span="2">
          {{ evaluation.imageUrls || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="评价内容" :span="3">{{ evaluation.content || '--' }}</el-descriptions-item>
        <el-descriptions-item label="回复内容" :span="2">{{ evaluation.replyContent || '--' }}</el-descriptions-item>
        <el-descriptions-item label="回复时间">{{ formatDateTime(evaluation.replyTime) }}</el-descriptions-item>
        <el-descriptions-item label="回复人编码">{{ evaluation.replyByCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="3">{{ formatDateTime(evaluation.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </template>

    <!-- 无评价：空状态 + 新增 -->
    <template v-else-if="!loading">
      <el-empty description="该会话暂未评价">
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增评价</el-button>
      </el-empty>
    </template>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增评价' : '编辑评价'"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="服务态度">
          <el-rate v-model="form.attitudeRating" />
        </el-form-item>
        <el-form-item label="专业度">
          <el-rate v-model="form.professionalRating" />
        </el-form-item>
        <el-form-item label="响应速度">
          <el-rate v-model="form.responsivenessRating" />
        </el-form-item>
        <el-form-item label="满意度">
          <el-rate v-model="form.satisfactionRating" />
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="评价内容" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="评价图片">
          <FileUploader v-model="imageUrlsModel" type="image" multiple module="service" />
        </el-form-item>
        <el-form-item label="是否匿名">
          <el-select v-model="form.isAnonymous" style="width: 160px">
            <el-option v-for="o in EVALUATION_IS_ANONYMOUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="运营回复">
          <el-input v-model="form.replyContent" type="textarea" :rows="3" placeholder="运营回复（可选）" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 160px">
            <el-option v-for="o in EVALUATION_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
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
.eval-toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}
</style>
