<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCourse } from '@/api/course'
import { listCourseLecturers } from '@/api/course-sub'
import type { CourseInfo, CourseLecturer } from '@/types/course'
import { COURSE_TYPE_OPTIONS, COURSE_STATUS_OPTIONS, courseStatusTagType } from '@/types/course'
import BasicTab from './BasicTab.vue'
import RecordLearnTab from './RecordLearnTab.vue'

/**
 * 课程详情页（多 tab）。路由 CourseDetail，params: courseCode。
 * tab：基本信息（含讲师） / 学习记录。
 */
const route = useRoute()
const router = useRouter()
const courseCode = computed(() => route.params.courseCode as string)

const detailLoading = ref(false)
const info = ref<CourseInfo | null>(null)
const lecturers = ref<CourseLecturer[]>([])

const tabs = [
  { name: 'basic', label: '基本信息' },
  { name: 'learn', label: '学习记录' }
] as const
const activeTab = ref<'basic' | 'learn'>('basic')

const lecturerName = computed(() => {
  if (!info.value?.lecturerCode) return '-'
  return lecturers.value.find((l) => l.lecturerCode === info.value?.lecturerCode)?.lecturerName || info.value.lecturerCode
})

async function loadDetail() {
  if (!courseCode.value) return
  detailLoading.value = true
  try {
    info.value = await getCourse(courseCode.value)
  } catch {
    info.value = null
  } finally {
    detailLoading.value = false
  }
}

async function loadLecturers() {
  try {
    lecturers.value = await listCourseLecturers()
  } catch {
    lecturers.value = []
  }
}

function courseTypeLabel(t?: number) {
  return COURSE_TYPE_OPTIONS.find((o) => o.value === t)?.label ?? '--'
}
function courseStatusLabel(s?: number) {
  return COURSE_STATUS_OPTIONS.find((o) => o.value === s)?.label ?? '--'
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadLecturers()
  loadDetail()
})
</script>

<template>
  <div v-loading="detailLoading" class="page-container">
    <el-page-header @back="goBack">
      <template #content>
        <span class="header-title">{{ info?.courseName ?? '课程详情' }}</span>
        <el-tag v-if="info?.courseCode" size="small" style="margin-left: 12px">{{ info.courseCode }}</el-tag>
      </template>
    </el-page-header>

    <el-card v-if="info" shadow="never">
      <el-descriptions :column="4" border>
        <el-descriptions-item label="类型">
          <el-tag type="info">{{ courseTypeLabel(info.courseType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="courseStatusTagType(info.courseStatus)">
            {{ courseStatusLabel(info.courseStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="讲师">{{ lecturerName }}</el-descriptions-item>
        <el-descriptions-item label="售价">¥{{ info.salePrice ?? 0 }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-card v-else-if="!detailLoading" shadow="never">
      <el-empty :description="`未找到课程（courseCode=${courseCode}）`" />
    </el-card>

    <el-card v-if="info" shadow="never">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" lazy>
          <BasicTab
            v-if="t.name === 'basic'"
            :course-code="courseCode"
            :lecturers="lecturers"
            @updated="loadDetail"
          />
          <RecordLearnTab v-else-if="t.name === 'learn'" :course-code="courseCode" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.header-title {
  font-size: 16px;
  font-weight: 500;
}
</style>
