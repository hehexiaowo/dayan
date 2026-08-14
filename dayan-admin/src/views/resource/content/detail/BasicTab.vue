<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { getContent, updateContent } from '@/api/content'
import { listContentCategories } from '@/api/content-sub'
import type { ContentInfo, ContentCategory } from '@/types/content'
import { ContentType, CONTENT_TYPE_OPTIONS, SOURCE_TYPE_OPTIONS } from '@/types/content'
import FileUploader from '@/components/FileUploader/index.vue'

const props = defineProps<{ contentCode: string }>()
const emit = defineEmits<{ (e: 'updated'): void }>()

const loading = ref(false)
const detail = ref<ContentInfo | null>(null)
const categoryOptions = ref<ContentCategory[]>([])

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getContent(props.contentCode)
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categoryOptions.value = await listContentCategories()
  } catch {
    categoryOptions.value = []
  }
}

// ---------- 编辑弹窗 ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<ContentInfo>({})

function openEdit() {
  if (!detail.value) return
  Object.assign(form, detail.value)
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
    await updateContent(props.contentCode, form)
    ElMessage.success('修改成功')
    dialogVisible.value = false
    await loadDetail()
    emit('updated')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadCategories()
  loadDetail()
})
</script>

<template>
  <div v-loading="loading">
    <div style="margin-bottom: 12px">
      <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑基本信息</el-button>
    </div>

    <el-descriptions v-if="detail" :column="2" border>
      <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
      <el-descriptions-item label="副标题" :span="2">{{ detail.subtitle || '-' }}</el-descriptions-item>
      <el-descriptions-item label="内容类型">
        {{ CONTENT_TYPE_OPTIONS.find((o) => o.value === detail!.contentType)?.label ?? '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="分类">
        {{ categoryOptions.find((c) => c.categoryCode === detail!.categoryCode)?.categoryName || detail!.categoryCode || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="作者">{{ detail.authorName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="来源类型">
        {{ SOURCE_TYPE_OPTIONS.find((o) => o.value === detail!.sourceType)?.label ?? '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="标签">{{ detail.tags || '-' }}</el-descriptions-item>
      <el-descriptions-item label="置顶/推荐">
        {{ detail.isTop === 1 ? '置顶' : '否' }} / {{ detail.isRecommend === 1 ? '推荐' : '否' }}
      </el-descriptions-item>
      <el-descriptions-item label="摘要" :span="2">{{ detail.summary || '-' }}</el-descriptions-item>
      <el-descriptions-item label="来源链接" :span="2">{{ detail.sourceUrl || '-' }}</el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
    </el-descriptions>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" title="编辑基本信息" width="760px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="标题" prop="title" :rules="[{ required: true, message: '请输入标题' }]">
              <el-input v-model="form.title" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="内容类型">
              <el-select v-model="form.contentType" style="width: 100%">
                <el-option v-for="o in CONTENT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.categoryCode" clearable filterable placeholder="选择分类" style="width: 100%">
                <el-option
                  v-for="c in categoryOptions"
                  :key="c.categoryCode"
                  :label="c.categoryName"
                  :value="c.categoryCode!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者">
              <el-input v-model="form.authorName" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源类型">
              <el-select v-model="form.sourceType" style="width: 100%">
                <el-option v-for="o in SOURCE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="置顶">
              <el-switch :model-value="form.isTop === 1" @change="(v: boolean) => (form.isTop = v ? 1 : 0)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="推荐">
              <el-switch :model-value="form.isRecommend === 1" @change="(v: boolean) => (form.isRecommend = v ? 1 : 0)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标签">
              <el-input v-model="form.tags" placeholder="多个标签用逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <FileUploader v-model="form.coverImage" type="image" module="content" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="摘要">
              <el-input v-model="form.summary" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="正文">
              <el-input v-model="form.contentBody" type="textarea" :rows="6" placeholder="支持 HTML" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
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
