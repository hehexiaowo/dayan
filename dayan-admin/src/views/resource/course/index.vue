<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageCourses,
  createCourse,
  updateCourse,
  deleteCourse,
  publishCourse,
  offlineCourse
} from '@/api/course'
import type { CourseInfo, CourseInfoQuery } from '@/types/course'
import {
  CourseType,
  COURSE_TYPE_OPTIONS,
  CourseStatus,
  COURSE_STATUS_OPTIONS
} from '@/types/course'
import FileUploader from '@/components/FileUploader/index.vue'

/**
 * 课程管理页。
 *
 * - 标准 CRUD（搜索 + 表格 + 分页 + 新增/编辑弹窗）；
 * - 操作列按 courseStatus 动态显示上架 / 下架按钮（PUT publish / offline）。
 * - 路由由后端菜单 menu_seed（component='resource/course/index'）+ router/dynamic.ts 自动解析，无需改路由。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<CourseInfo, CourseInfoQuery>(
    { page: pageCourses },
    {
      initialQuery: {
        courseName: '',
        courseType: undefined,
        courseStatus: undefined
      }
    }
  )

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 表单默认值（新建用） */
function defaultForm(): CourseInfo {
  return {
    courseCode: undefined,
    courseName: '',
    courseType: CourseType.ONLINE_RECORDED,
    categoryCode: '',
    coverImage: '',
    videoUrl: '',
    courseDescription: '',
    targetAudience: '',
    lecturerCode: '',
    totalClass: 0,
    totalDuration: 0,
    validDays: 0,
    originalPrice: 0,
    salePrice: 0,
    maxStudents: 0,
    isFree: 0,
    isRecommend: 0,
    courseStartDate: '',
    courseEndDate: '',
    sortOrder: 0,
    remark: ''
  }
}

const form = reactive<CourseInfo>(defaultForm())

const rules: FormRules<CourseInfo> = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  courseType: [{ required: true, message: '请选择课程类型', trigger: 'change' }],
  originalPrice: [{ required: true, message: '请输入原价', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入售价', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, defaultForm())
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: CourseInfo) {
  dialogType.value = 'edit'
  resetForm()
  // 将行数据覆盖到表单（仅取表单需要的字段）
  Object.assign(form, {
    courseCode: row.courseCode,
    courseName: row.courseName,
    courseType: row.courseType,
    categoryCode: row.categoryCode ?? '',
    coverImage: row.coverImage ?? '',
    videoUrl: row.videoUrl ?? '',
    courseDescription: row.courseDescription ?? '',
    targetAudience: row.targetAudience ?? '',
    lecturerCode: row.lecturerCode ?? '',
    totalClass: row.totalClass ?? 0,
    totalDuration: row.totalDuration ?? 0,
    validDays: row.validDays ?? 0,
    originalPrice: row.originalPrice ?? 0,
    salePrice: row.salePrice ?? 0,
    maxStudents: row.maxStudents ?? 0,
    isFree: row.isFree ?? 0,
    isRecommend: row.isRecommend ?? 0,
    courseStartDate: row.courseStartDate ?? '',
    courseEndDate: row.courseEndDate ?? '',
    sortOrder: row.sortOrder ?? 0,
    remark: row.remark ?? ''
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
    if (dialogType.value === 'create') {
      await createCourse(form)
      ElMessage.success('新增成功')
    } else if (form.courseCode) {
      // courseCode 走 path，其余字段走 body
      await updateCourse(form.courseCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: CourseInfo) {
  if (!row.courseCode) return
  await ElMessageBox.confirm(`确定删除课程「${row.courseName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteCourse(row.courseCode)
  ElMessage.success('删除成功')
  loadPage()
}

async function handlePublish(row: CourseInfo) {
  if (!row.courseCode) return
  await ElMessageBox.confirm(`确定上架课程「${row.courseName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await publishCourse(row.courseCode)
  ElMessage.success('上架成功')
  loadPage()
}

async function handleOffline(row: CourseInfo) {
  if (!row.courseCode) return
  await ElMessageBox.confirm(`确定下架课程「${row.courseName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await offlineCourse(row.courseCode)
  ElMessage.success('下架成功')
  loadPage()
}

function handleReset() {
  query.courseName = ''
  query.courseType = undefined
  query.courseStatus = undefined
  handleSearch()
}

// 课程类型 label 映射
function courseTypeLabel(type?: number): string {
  if (type === undefined || type === null) return '-'
  return COURSE_TYPE_OPTIONS.find((o) => o.value === type)?.label ?? String(type)
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="课程名称">
          <el-input
            v-model="query.courseName"
            placeholder="课程名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="课程类型">
          <el-select v-model="query.courseType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in COURSE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.courseStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in COURSE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>课程列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增课程</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="courseCode">
        <el-table-column prop="courseCode" label="课程编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="课程类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag>{{ courseTypeLabel(row.courseType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lecturerCode" label="讲师编码" min-width="120" show-overflow-tooltip />
        <el-table-column label="售价" width="100" align="right">
          <template #default="{ row }"> ¥{{ row.salePrice ?? 0 }} </template>
        </el-table-column>
        <el-table-column label="学员数" width="100" align="center">
          <template #default="{ row }">
            {{ row.currentStudents ?? 0 }}/{{ row.maxStudents ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="salesCount" label="销量" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.courseStatus === CourseStatus.ONLINE ? 'success' : 'info'">
              {{ row.courseStatus === CourseStatus.ONLINE ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.courseStatus === CourseStatus.OFFLINE"
              link
              type="success"
              size="small"
              @click="handlePublish(row)"
              >上架</el-button
            >
            <el-button
              v-if="row.courseStatus === CourseStatus.ONLINE"
              link
              type="warning"
              size="small"
              @click="handleOffline(row)"
              >下架</el-button
            >
            <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          :current-page="query.current"
          :page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增课程' : '编辑课程'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" placeholder="课程名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程类型" prop="courseType">
              <el-select v-model="form.courseType" placeholder="课程类型" style="width: 100%">
                <el-option v-for="o in COURSE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类编码">
              <el-input v-model="form.categoryCode" placeholder="分类编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="讲师编码">
              <el-input v-model="form.lecturerCode" placeholder="讲师编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <FileUploader v-model="form.coverImage" type="image" module="course" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课程视频">
              <FileUploader v-model="form.videoUrl" type="video" module="course" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课程简介">
              <el-input
                v-model="form.courseDescription"
                type="textarea"
                :rows="2"
                placeholder="课程简介"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="目标人群">
              <el-input v-model="form.targetAudience" placeholder="目标人群" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总课时">
              <el-input-number v-model="form.totalClass" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总时长">
              <el-input-number v-model="form.totalDuration" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="有效天数">
              <el-input-number v-model="form.validDays" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原价" prop="originalPrice">
              <el-input-number
                v-model="form.originalPrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="售价" prop="salePrice">
              <el-input-number
                v-model="form.salePrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="人数上限">
              <el-input-number v-model="form.maxStudents" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始日期">
              <el-date-picker
                v-model="form.courseStartDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="开始日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期">
              <el-date-picker
                v-model="form.courseEndDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="结束日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否免费">
              <el-switch v-model="form.isFree" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否推荐">
              <el-switch v-model="form.isRecommend" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
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
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
