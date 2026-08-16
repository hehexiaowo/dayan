<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
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

/** 学习目标按分号/换行拆行展示 */
const objectiveLines = computed(() =>
  (detail.value?.learningObjectives || '')
    .split(/[；;\n]/)
    .map((s) => s.trim())
    .filter(Boolean)
)

/** 课程大纲展示模型（JSON 容错解析） */
interface OutlineChapterModel {
  title: string
  lessons: { title: string; duration?: number }[]
}

const outlineChapters = computed<OutlineChapterModel[]>(() => {
  const raw = detail.value?.courseOutline
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.filter((ch: any) => ch && typeof ch.title === 'string' && Array.isArray(ch.lessons))
  } catch {
    return []
  }
})

const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({} as CourseInfo)

/** 编辑弹窗的大纲动态表单模型（提交时序列化进 form.courseOutline） */
const editOutlineChapters = ref<OutlineChapterModel[]>([])

/** JSON → 动态表单（空/坏 JSON 容错为空数组） */
function parseOutline(raw?: string): OutlineChapterModel[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter((ch: any) => ch && typeof ch.title === 'string' && Array.isArray(ch.lessons))
      .map((ch: any) => ({
        title: ch.title,
        lessons: ch.lessons
          .filter((ls: any) => ls && typeof ls.title === 'string')
          .map((ls: any) => ({ title: ls.title, duration: ls.duration ?? undefined }))
      }))
  } catch {
    return []
  }
}

/** 动态表单 → JSON（过滤空标题项） */
function serializeOutline(): string {
  const chapters = editOutlineChapters.value
    .map((ch) => ({
      title: ch.title.trim(),
      lessons: ch.lessons
        .filter((ls) => ls.title.trim())
        .map((ls) => ({ title: ls.title.trim(), ...(ls.duration ? { duration: ls.duration } : {}) }))
    }))
    .filter((ch) => ch.title && ch.lessons.length)
  return chapters.length ? JSON.stringify(chapters) : ''
}

function openEdit() {
  if (!detail.value) return
  Object.assign(form, detail.value)
  editOutlineChapters.value = parseOutline(detail.value.courseOutline)
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  form.courseOutline = serializeOutline()
  form.learningObjectives = form.learningObjectives?.trim() || ''

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
      <el-descriptions-item label="学习目标" :span="2">
        <template v-if="objectiveLines.length">
          <div v-for="(line, i) in objectiveLines" :key="i">{{ i + 1 }}. {{ line }}</div>
        </template>
        <template v-else>-</template>
      </el-descriptions-item>
      <el-descriptions-item label="宣传视频" :span="2">
        <video v-if="detail.videoUrl" :src="detail.videoUrl" controls style="max-width: 360px; max-height: 200px" />
        <template v-else>-</template>
      </el-descriptions-item>
      <el-descriptions-item label="课程大纲" :span="2">
        <template v-if="outlineChapters.length">
          <div v-for="(ch, ci) in outlineChapters" :key="ci" style="margin-bottom: 4px">
            <b>{{ ch.title }}</b>
            <div v-for="(ls, li) in ch.lessons" :key="li" style="padding-left: 16px">
              {{ li + 1 }}. {{ ls.title }}<template v-if="ls.duration">（{{ ls.duration }} 分钟）</template>
            </div>
          </div>
        </template>
        <template v-else>-</template>
      </el-descriptions-item>
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
            <el-form-item label="目标人群">
              <el-input v-model="form.targetAudience" maxlength="200" placeholder="如：入行 1-3 年的保险代理人" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="学习目标">
              <el-input
                v-model="form.learningObjectives"
                type="textarea"
                :rows="3"
                maxlength="500"
                placeholder="每行一个目标，如：&#10;独立完成年金险需求分析&#10;掌握 5 类高频异议处理"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="宣传视频">
              <FileUploader
                v-model="form.videoUrl"
                type="video"
                module="course"
                register-asset
                asset-ref-type1="course"
                :asset-ref-code="props.courseCode"
                asset-ref-type2="course"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课程大纲">
              <div class="outline-editor">
                <div v-for="(ch, ci) in editOutlineChapters" :key="ci" class="outline-chapter">
                  <div class="outline-chapter-head">
                    <el-input v-model="ch.title" placeholder="章节标题（如：第一章 需求挖掘）" maxlength="50" style="flex: 1" />
                    <el-button link type="danger" @click="editOutlineChapters.splice(ci, 1)">删章节</el-button>
                  </div>
                  <div v-for="(ls, li) in ch.lessons" :key="li" class="outline-lesson">
                    <el-input v-model="ls.title" placeholder="课次标题" maxlength="100" style="flex: 1" />
                    <el-input-number v-model="ls.duration" :min="0" :max="600" placeholder="分钟" controls-position="right" style="width: 110px" />
                    <el-button link type="danger" @click="ch.lessons.splice(li, 1)">删</el-button>
                  </div>
                  <el-button link type="primary" size="small" @click="ch.lessons.push({ title: '', duration: undefined })">+ 添加课次</el-button>
                </div>
                <el-button link type="primary" @click="editOutlineChapters.push({ title: '', lessons: [{ title: '', duration: undefined }] })">+ 添加章节</el-button>
              </div>
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

<style scoped lang="scss">
.outline-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.outline-chapter {
  padding: 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.outline-chapter-head,
.outline-lesson {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
