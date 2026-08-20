<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { pageCourses, createCourse, updateCourse, deleteCourse } from '@/api/course'
import { COURSE_SOURCE_OPTIONS, COURSE_STATUS_OPTIONS, type CourseInfo, type CourseInfoQuery } from '@/types/course'
import FileUploader from '@/components/FileUploader/index.vue'
import RichEditor from '@/components/RichEditor/index.vue'

/**
 * 课程板块 tab（渠道课程 / 外部课程 / 雁鸣中国共用）。
 *
 * - 学习中心四板块统一收口 course_info 后，非平台自研板块由本组件承载，
 *   轻量表单（基础信息 + 正文），课程形态字段（讲师/价格/大纲）不展示；
 * - 由课程管理页按 courseSource 实例化，tab 懒加载时才发请求；
 * - 板块来源可在表单中调整（内容跨板块搬家），查询范围固定本板块；
 * - 主键 courseCode 为字符串，update 用 path courseCode。
 */
const props = defineProps<{
  /** 板块来源：2=渠道课程 3=外部课程 4=雁鸣中国 */
  source: number
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<CourseInfo, CourseInfoQuery, string>(
    {
      page: (q) => pageCourses({ ...q, courseSource: props.source }),
      create: (data) => createCourse(data),
      update: (id, data) => updateCourse(id, data),
      remove: deleteCourse
    },
    {
      initialQuery: { courseName: '', courseStatus: undefined },
      idKey: 'courseCode'
    }
  )

const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 课程状态：0=草稿 1=待上架 2=已上架 3=已下架 4=已结课（新建默认待上架，可在表单调整） */
const CourseStatusValue = { DRAFT: 0, PENDING: 1, ONLINE: 2, OFFLINE: 3, FINISHED: 4 }

const form = reactive<CourseInfo>({
  courseName: '',
  courseSource: props.source,
  coverImage: '',
  courseDescription: '',
  courseBody: '',
  author: '',
  durationText: '',
  badge: '',
  publishTime: '',
  sortOrder: 0,
  courseStatus: CourseStatusValue.PENDING
})

const rules: FormRules<CourseInfo> = {
  courseName: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  courseSource: [{ required: true, message: '请选择板块来源', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    courseCode: undefined,
    courseName: '',
    courseSource: props.source,
    coverImage: '',
    courseDescription: '',
    courseBody: '',
    author: '',
    durationText: '',
    badge: '',
    publishTime: '',
    sortOrder: 0,
    courseStatus: CourseStatusValue.PENDING
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: CourseInfo) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, {
    courseCode: row.courseCode,
    courseName: row.courseName,
    courseSource: row.courseSource ?? props.source,
    coverImage: row.coverImage ?? '',
    courseDescription: row.courseDescription ?? '',
    courseBody: row.courseBody ?? '',
    author: row.author ?? '',
    durationText: row.durationText ?? '',
    badge: row.badge ?? '',
    publishTime: (row.publishTime || '').replace('T', ' ').slice(0, 19),
    sortOrder: row.sortOrder ?? 0,
    courseStatus: row.courseStatus ?? CourseStatusValue.PENDING
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
    // publishTime 为空时置 undefined（后端默认当前时间），避免空串反序列化 400
    if (!form.publishTime) form.publishTime = undefined
    if (dialogType.value === 'create') {
      await createCourse(form)
      ElMessage.success('新增成功')
    } else if (form.courseCode) {
      await updateCourse(form.courseCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: CourseInfo) {
  if (!row.courseCode || !row.courseName) return
  await ElMessageBox.confirm(`确定删除「${row.courseName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteCourse(row.courseCode)
  ElMessage.success('删除成功')
  loadPage()
}

function handleReset() {
  query.courseName = ''
  query.courseStatus = undefined
  handleSearch()
}

function sourceLabel(s?: number) {
  return COURSE_SOURCE_OPTIONS.find((o) => o.value === s)?.label ?? '-'
}

loadPage()
</script>

<template>
  <div class="tab-body">
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input
          v-model="query.courseName"
          placeholder="标题"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.courseStatus" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="o in COURSE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">{{ sourceLabel(props.source) }}列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增内容</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="courseCode">
        <el-table-column prop="courseCode" label="编码" min-width="140" show-overflow-tooltip />
        <el-table-column label="标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.badge" size="small" class="badge-tag">{{ row.badge }}</el-tag>
            <span>{{ row.courseName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" min-width="120" show-overflow-tooltip />
        <el-table-column prop="viewCount" label="浏览量" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.courseStatus === 2 ? 'success' : 'info'">
              {{ COURSE_STATUS_OPTIONS.find((o) => o.value === row.courseStatus)?.label ?? '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160" align="center">
          <template #default="{ row }">{{ (row.publishTime || '').replace('T', ' ').slice(0, 16) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
      :title="dialogType === 'create' ? '新增内容' : '编辑内容'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="内容编码">
              <el-input :model-value="dialogType === 'create' ? '保存时自动生成' : form.courseCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="板块来源" prop="courseSource">
              <el-select v-model="form.courseSource" style="width: 100%">
                <el-option v-for="o in COURSE_SOURCE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标题" prop="courseName">
              <el-input v-model="form.courseName" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <FileUploader v-model="form.coverImage" type="image" module="course" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="摘要">
              <el-input v-model="form.courseDescription" type="textarea" :rows="2" maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者">
              <el-input v-model="form.author" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时长">
              <el-input v-model="form.durationText" maxlength="20" placeholder="如 28:30 / 约 15 分钟" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="角标">
              <el-input v-model="form.badge" maxlength="20" placeholder="热 / 新 / 要闻" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="发布时间">
              <el-date-picker
                v-model="form.publishTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="发布时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.courseStatus" style="width: 100%">
                <el-option v-for="o in COURSE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 220px" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="正文">
              <RichEditor v-model="form.courseBody" :height="300" placeholder="正文内容（支持富文本）" module="course" />
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
.badge-tag {
  margin-right: 6px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}
</style>
