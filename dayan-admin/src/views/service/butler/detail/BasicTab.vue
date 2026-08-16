<script setup lang="ts">
/**
 * 管家详情页 - 基本信息 tab。
 *
 * 只读展示 ButlerInfo 关键字段（el-descriptions），提供"编辑"按钮打开 el-dialog + el-form
 * 修改主表（提交 updateButler）。字段集与主列表页编辑表单一致。
 *
 * 枚举字段用 el-select + OPTIONS：
 * - butlerLevel：4 档（BUTLER_LEVEL_OPTIONS）。
 * - status：在职/离职（BUTLER_STATUS_OPTIONS，非通用 COMMON_STATUS_OPTIONS）。
 */
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getButler, updateButler } from '@/api/service'
import {
  BUTLER_LEVEL_OPTIONS,
  BUTLER_STATUS_OPTIONS,
  butlerLevelLabel,
  butlerLevelTagType,
  butlerStatusLabel,
  butlerStatusTagType
} from '@/types/service'
import type { ButlerInfo } from '@/types/service'
import { formatDateTime } from '@/utils/format'
import FileUploader from '@/components/FileUploader/index.vue'
import { formatFileUrl } from '@/utils/file'

const props = defineProps<{
  /** 管家编码（从详情页路由 prop 带入） */
  butlerCode: string
}>()

const loading = ref(false)
const butlerInfo = ref<ButlerInfo | null>(null)

async function loadDetail() {
  if (!props.butlerCode) return
  loading.value = true
  try {
    butlerInfo.value = await getButler(props.butlerCode)
  } catch {
    butlerInfo.value = null
  } finally {
    loading.value = false
  }
}

loadDetail()

// ---------- 编辑弹窗 ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ButlerInfo>({
  butlerCode: undefined,
  fullName: '',
  phone: '',
  avatar: '',
  organCode: '',
  butlerLevel: undefined,
  status: 1,
  remark: ''
})

const rules: FormRules<ButlerInfo> = {
  fullName: [{ required: true, message: '请输入管家姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

function resetForm() {
  Object.assign(form, {
    butlerCode: undefined,
    fullName: '',
    phone: '',
    avatar: '',
    organCode: '',
    butlerLevel: undefined,
    status: 1,
    remark: ''
  })
}

function openEdit() {
  if (!butlerInfo.value) return
  resetForm()
  Object.assign(form, {
    butlerCode: butlerInfo.value.butlerCode,
    fullName: butlerInfo.value.fullName ?? '',
    phone: butlerInfo.value.phone ?? '',
    avatar: butlerInfo.value.avatar ?? '',
    organCode: butlerInfo.value.organCode ?? '',
    butlerLevel: butlerInfo.value.butlerLevel,
    status: butlerInfo.value.status ?? 1,
    remark: butlerInfo.value.remark ?? ''
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
  if (!form.butlerCode) return
  submitLoading.value = true
  try {
    await updateButler(form.butlerCode, form)
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
    <template v-if="butlerInfo">
      <div class="basic-toolbar">
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑基本信息</el-button>
        </div>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="管家编码">{{ butlerInfo.butlerCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ butlerInfo.fullName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ butlerInfo.phone ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="所属组织">{{ butlerInfo.organCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="头像" :span="2">
          <el-image
            v-if="butlerInfo.avatar"
            :src="formatFileUrl(butlerInfo.avatar)"
            :preview-src-list="[formatFileUrl(butlerInfo.avatar)]"
            fit="cover"
            style="width: 60px; height: 60px; border-radius: 50%"
          />
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="管家等级">
          <el-tag size="small" :type="butlerLevelTagType(butlerInfo.butlerLevel)">
            {{ butlerLevelLabel(butlerInfo.butlerLevel) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="butlerStatusTagType(butlerInfo.status)" effect="light">
            {{ butlerStatusLabel(butlerInfo.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(butlerInfo.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(butlerInfo.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ butlerInfo.remark ?? '--' }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <el-empty v-else-if="!loading" description="未加载到管家信息" />

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑管家基本信息"
      width="620px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="管家编码">
              <el-input v-model="form.butlerCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管家姓名" prop="fullName">
              <el-input v-model="form.fullName" placeholder="管家姓名" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="手机号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="头像">
              <FileUploader v-model="form.avatar" type="image" module="service" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属组织">
              <el-input v-model="form.organCode" placeholder="组织编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管家等级">
              <el-select v-model="form.butlerLevel" placeholder="管家等级" clearable style="width: 100%">
                <el-option v-for="o in BUTLER_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" placeholder="状态" style="width: 100%">
                <el-option v-for="o in BUTLER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
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
