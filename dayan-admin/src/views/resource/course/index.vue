<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
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
import { listCourseLecturers } from '@/api/course-sub'
import type { CourseInfo, CourseInfoQuery, CourseLecturer } from '@/types/course'
import {
  CourseType,
  COURSE_TYPE_OPTIONS,
  CourseStatus,
  COURSE_STATUS_OPTIONS,
  CourseSource,
  courseStatusTagType
} from '@/types/course'
import FileUploader from '@/components/FileUploader/index.vue'

interface CourseChapter {
  title: string
  lessons?: { title: string; duration?: number }[]
}
import LecturerManageDrawer from './components/LecturerManageDrawer.vue'
import CourseBoardTab from './components/CourseBoardTab.vue'
import { useDictOptions } from '@/composables/useDict'

/** 课程分类选项（业务字典 course_category，管理入口：系统管理-业务字典） */
const { options: categoryOptions } = useDictOptions('course_category')

/**
 * 课程管理页（四大板块 tab，统一 course_info）。
 *
 * - 大雁课程 = course_source=1（平台自研）：标准 CRUD + 上/下架 + 讲师管理抽屉；
 * - 渠道课程 / 外部课程 / 雁鸣中国 = course_source 2/3/4，
 *   共用 CourseBoardTab 组件（轻量表单 + 正文），tab 懒加载时才发请求；
 * - 所有 el-tab-pane 必须 lazy（避免 4 tab 同挂载 + 未捕获错误整页崩）；
 * - 路由由后端菜单 menu_seed（component='resource/course/index'）+ router/dynamic.ts 自动解析，无需改路由。
 */

/** 板块 tab：dayan=大雁课程(course_source=1)，其余为 course_source 2/3/4 */
const activeTab = ref('dayan')

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<CourseInfo, CourseInfoQuery>(
    { page: pageCourses },
    {
      initialQuery: {
        courseName: '',
        courseType: undefined,
        courseStatus: undefined,
        lecturerCode: ''
      },
      // 大雁课程 tab 固定只看平台自研板块
      fixedParams: { courseSource: CourseSource.SELF }
    }
  )

const router = useRouter()

/** 讲师下拉选项 + 名称映射（后端 VO 不带 lecturerName，前端自行映射） */
const lecturerOptions = ref<CourseLecturer[]>([])

/** 讲师管理抽屉开关（v-if 挂载，打开才加载） */
const lecturerDrawerVisible = ref(false)
const lecturerNameMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  for (const l of lecturerOptions.value) {
    if (l.lecturerCode) map[l.lecturerCode] = l.lecturerName
  }
  return map
})

async function loadLecturers() {
  try {
    lecturerOptions.value = await listCourseLecturers({ status: 1 })
  } catch {
    lecturerOptions.value = []
  }
}

/** 跳转课程详情 */
function openDetail(row: CourseInfo) {
  if (!row.courseCode) return
  router.push({ name: 'CourseDetail', params: { courseCode: row.courseCode } })
}

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
    courseOutline: '',
    learningObjectives: '',
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

/** 课程大纲动态表单模型（提交时序列化进 form.courseOutline） */
interface OutlineChapterModel {
  title: string
  lessons: { title: string; duration?: number }[]
}
const outlineChapters = ref<OutlineChapterModel[]>([])

/** JSON → 动态表单（空/坏 JSON 容错为空数组） */
function parseOutline(raw?: string): OutlineChapterModel[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter((ch: CourseChapter) => ch && typeof ch.title === 'string' && Array.isArray(ch.lessons))
      .map((ch: CourseChapter) => ({
        title: ch.title,
        lessons: (ch.lessons ?? [])
          .filter((ls: { title: string; duration?: number }) => ls && typeof ls.title === 'string')
          .map((ls: { title: string; duration?: number }) => ({ title: ls.title, duration: ls.duration ?? undefined }))
      }))
  } catch {
    return []
  }
}

/** 动态表单 → JSON（过滤空标题项） */
function serializeOutline(): string {
  const chapters = outlineChapters.value
    .map((ch) => ({
      title: ch.title.trim(),
      lessons: ch.lessons
        .filter((ls) => ls.title.trim())
        .map((ls) => ({ title: ls.title.trim(), ...(ls.duration ? { duration: ls.duration } : {}) }))
    }))
    .filter((ch) => ch.title && ch.lessons.length)
  return chapters.length ? JSON.stringify(chapters) : ''
}

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
  outlineChapters.value = []
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
    courseOutline: row.courseOutline ?? '',
    learningObjectives: row.learningObjectives ?? '',
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
  outlineChapters.value = parseOutline(row.courseOutline)
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
  query.lecturerCode = ''
  handleSearch()
}

// 课程类型 label 映射
function courseTypeLabel(type?: number): string {
  if (type === undefined || type === null) return '-'
  return COURSE_TYPE_OPTIONS.find((o) => o.value === type)?.label ?? String(type)
}

// 初始化加载
onMounted(() => {
  loadLecturers()
  loadPage()
})
</script>

<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" class="board-tabs">
      <!-- ===== 大雁课程（course_info 平台自研课程） ===== -->
      <el-tab-pane label="大雁课程" name="dayan" lazy>
        <div class="tab-body">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input
          v-model="query.courseName"
          placeholder="课程名称"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.courseType" placeholder="课程类型" clearable style="width: 140px">
          <el-option v-for="o in COURSE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.courseStatus" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="o in COURSE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.lecturerCode" placeholder="讲师" clearable filterable style="width: 180px">
          <el-option
            v-for="l in lecturerOptions"
            :key="l.lecturerCode"
            :label="l.lecturerName"
            :value="l.lecturerCode!"
          />
        </el-select>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">课程列表</span>
          <div>
            <el-button :icon="'UserFilled'" @click="lecturerDrawerVisible = true">讲师管理</el-button>
            <el-button type="primary" :icon="'Plus'" @click="openCreate">新增课程</el-button>
          </div>
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
        <el-table-column label="讲师" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ lecturerNameMap[row.lecturerCode] || row.lecturerCode || '-' }}</template>
        </el-table-column>
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
            <el-tag :type="courseStatusTagType(row.courseStatus)">
              {{ COURSE_STATUS_OPTIONS.find((o) => o.value === row.courseStatus)?.label ?? '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.courseStatus !== CourseStatus.ONLINE && row.courseStatus !== CourseStatus.FINISHED"
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
        </div>
      </el-tab-pane>

      <!-- ===== course_source 三板块（共用组件，懒加载） ===== -->
      <el-tab-pane label="渠道课程" name="channel" lazy>
        <CourseBoardTab :source="CourseSource.CHANNEL" />
      </el-tab-pane>
      <el-tab-pane label="外部课程" name="external" lazy>
        <CourseBoardTab :source="CourseSource.EXTERNAL" />
      </el-tab-pane>
      <el-tab-pane label="雁鸣中国" name="yanming" lazy>
        <CourseBoardTab :source="CourseSource.YANMING" />
      </el-tab-pane>
    </el-tabs>

    <!-- 新增 / 编辑课程弹窗（大雁课程） -->
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
            <el-form-item label="讲师">
              <el-select v-model="form.lecturerCode" placeholder="选择讲师" clearable filterable style="width: 100%">
                <el-option
                  v-for="l in lecturerOptions"
                  :key="l.lecturerCode"
                  :label="`${l.lecturerName}${l.title ? '（' + l.title + '）' : ''}`"
                  :value="l.lecturerCode!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <FileUploader v-model="form.coverImage" type="image" module="course" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="宣传视频">
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
            <el-form-item label="课程大纲">
              <div class="outline-editor">
                <div v-for="(ch, ci) in outlineChapters" :key="ci" class="outline-chapter">
                  <div class="outline-chapter-head">
                    <el-input v-model="ch.title" placeholder="章节标题（如：第一章 需求挖掘）" maxlength="50" style="flex: 1" />
                    <el-button link type="danger" @click="outlineChapters.splice(ci, 1)">删章节</el-button>
                  </div>
                  <div v-for="(ls, li) in ch.lessons" :key="li" class="outline-lesson">
                    <el-input v-model="ls.title" placeholder="课次标题" maxlength="100" style="flex: 1" />
                    <el-input-number v-model="ls.duration" :min="0" :max="600" placeholder="分钟" controls-position="right" style="width: 110px" />
                    <el-button link type="danger" @click="ch.lessons.splice(li, 1)">删</el-button>
                  </div>
                  <el-button link type="primary" size="small" @click="ch.lessons.push({ title: '', duration: undefined })">+ 添加课次</el-button>
                </div>
                <el-button link type="primary" @click="outlineChapters.push({ title: '', lessons: [{ title: '', duration: undefined }] })">+ 添加章节</el-button>
              </div>
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

    <!-- 讲师管理抽屉（全局讲师资源内联 CRUD） -->
    <LecturerManageDrawer v-if="lecturerDrawerVisible" v-model="lecturerDrawerVisible" @changed="loadLecturers" />
  </div>
</template>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.board-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }
  :deep(.el-tabs__content) {
    padding-top: 16px;
  }
}
.tab-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

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

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}
</style>
