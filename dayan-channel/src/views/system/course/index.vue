<script setup lang="ts">
/**
 * Channel 端课程配置落地页（系统管理 → 课程配置）。
 *
 * 渠道可配置哪些课程在 agent 端可见：
 * - 可选课程 = 平台课程（channel_code 为 NULL）+ 本渠道课程
 * - 已配置课程存 channel_config_course 表
 * - agent 端课程列表 = 平台课程 + 本渠道课程 + 已配置课程（并集）
 */
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage, type TableInstance } from 'element-plus'
import { getAvailableCourses, getConfiguredCourseCodes, saveCourseVisibility } from '@/api/courseConfig'
import type { CourseOption } from '@/types/courseConfig'

const loading = ref(false)
const saving = ref(false)
const courses = ref<CourseOption[]>([])
const configuredCodes = ref<string[]>([])
const selectedCodes = ref<string[]>([])
const tableRef = ref<TableInstance>()

async function loadData() {
  loading.value = true
  try {
    const [c, codes] = await Promise.all([getAvailableCourses(), getConfiguredCourseCodes()])
    courses.value = c
    configuredCodes.value = codes
    selectedCodes.value = [...codes]
    // 数据加载完成后，回填已配置课程的勾选状态
    await nextTick()
    if (tableRef.value) {
      tableRef.value.clearSelection()
      for (const row of c) {
        if (codes.includes(row.courseCode)) {
          tableRef.value.toggleRowSelection(row, true)
        }
      }
    }
  } catch {
    ElMessage.error('加载课程配置失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

function handleSelectionChange(rows: CourseOption[]) {
  selectedCodes.value = rows.map((r) => r.courseCode)
}

async function save() {
  saving.value = true
  try {
    await saveCourseVisibility(selectedCodes.value)
    configuredCodes.value = [...selectedCodes.value]
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function courseTypeLabel(type?: number) {
  const map: Record<number, string> = { 1: '线上录播', 2: '线上直播', 3: '线下课程', 4: '混合课程' }
  return type ? map[type] || '未知' : '-'
}

function courseSourceLabel(source?: number) {
  const map: Record<number, string> = { 1: '平台自研', 2: '渠道课程', 3: '外部课程', 4: '雁鸣资讯' }
  return source ? map[source] || '未知' : '-'
}
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>课程配置</span>
          <span class="card-tip">配置哪些课程在 agent 端可见（平台课程 + 渠道课程 + 已配置课程）</span>
        </div>
      </template>

      <div class="toolbar">
        <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
        <span class="selected-count">已选择 {{ selectedCodes.length }} 门课程</span>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="courses"
        stripe
        row-key="courseCode"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="courseName" label="课程名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="课程类型" width="100">
          <template #default="{ row }">{{ courseTypeLabel(row.courseType) }}</template>
        </el-table-column>
        <el-table-column label="板块来源" width="100">
          <template #default="{ row }">{{ courseSourceLabel(row.courseSource) }}</template>
        </el-table-column>
        <el-table-column prop="lecturerName" label="讲师" width="120" />
        <el-table-column prop="courseDescription" label="简介" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.card-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.selected-count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
