<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { getCourse, updateCourse } from '@/api/course'
import type { CourseInfo, CourseLecturer } from '@/types/course'
import { COURSE_TYPE_OPTIONS } from '@/types/course'
import FileUploader from '@/components/FileUploader/index.vue'
import { useDictOptions } from '@/composables/useDict'

/** 课程分类选项（业务字典 course_category） */
const { options: categoryOptions } = useDictOptions('course_category')

const props = defineProps<{ courseCode: string; lecturers: CourseLecturer[] }>()
const emit = defineEmits<{ (e: 'updated'): void }>()

const loading = ref(false)
const detail = ref<CourseInfo | null>(null)

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getCourse(props.courseCode)
  } finally {
    loading.value = false
  }
}

const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({} as CourseInfo)

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
    await updateCourse(props.courseCode, form)
    ElMessage.success('修改成功')
    dialogVisible.value = false
    await loadDetail()
    emit('updated')
  } finally {
    submitLoading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div v-loading="loading">
    <div style="margin-bottom: 12px">
      <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑基本信息</el-button>
    </div>

    <el-descriptions v-if="detail" :column="2" border>
      <el-descriptions-item label="课程名称" :span="2">{{ detail.courseName }}</el-descriptions-item>
      <el-descriptions-item label="课程类型">
        {{ COURSE_TYPE_OPTIONS.find((o) => o.value === detail!.courseType)?.label ?? '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="讲师">
        {{ props.lecturers.find((l) => l.lecturerCode === detail!.lecturerCode)?.lecturerName || detail!.lecturerCode || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="原价">¥{{ detail.originalPrice ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="售价">¥{{ detail.salePrice ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="总课时">{{ detail.totalClass ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="总时长(分)">{{ detail.totalDuration ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="有效天数">{{ detail.validDays ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="起止日期">{{ detail.courseStartDate || '?' }} ~ {{ detail.courseEndDate || '?' }}</el-descriptions-item>
      <el-descriptions-item label="学员数">{{ detail.currentStudents ?? 0 }} / {{ detail.maxStudents ?? '不限' }}</el-descriptions-item>
      <el-descriptions-item label="目标人群" :span="2">{{ detail.targetAudience || '-' }}</el-descriptions-item>
      <el-descriptions-item label="课程简介" :span="2">{{ detail.courseDescription || '-' }}</el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-dialog v-model="dialogVisible" title="编辑基本信息" width="760px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName" :rules="[{ required: true, message: '请输入课程名称' }]">
              <el-input v-model="form.courseName" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程类型">
              <el-select v-model="form.courseType" style="width: 100%">
                <el-option v-for="o in COURSE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="讲师">
              <el-select v-model="form.lecturerCode" clearable filterable placeholder="选择讲师" style="width: 100%">
                <el-option
                  v-for="l in props.lecturers"
                  :key="l.lecturerCode"
                  :label="l.lecturerName"
                  :value="l.lecturerCode!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程分类">
              <el-select v-model="form.categoryCode" placeholder="选择分类" clearable filterable style="width: 100%">
                <el-option
                  v-for="o in categoryOptions"
                  :key="o.dictCode"
                  :label="o.dictName"
                  :value="o.dictCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="售价">
              <el-input-number v-model="form.salePrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总课时">
              <el-input-number v-model="form.totalClass" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时长(分)">
              <el-input-number v-model="form.totalDuration" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="有效天数">
              <el-input-number v-model="form.validDays" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始日期">
              <el-date-picker v-model="form.courseStartDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期">
              <el-date-picker v-model="form.courseEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <FileUploader v-model="form.coverImage" type="image" module="course" register-asset asset-ref-type1="course" :asset-ref-code="props.courseCode" asset-ref-type2="course" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课程简介">
              <el-input v-model="form.courseDescription" type="textarea" :rows="2" />
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
