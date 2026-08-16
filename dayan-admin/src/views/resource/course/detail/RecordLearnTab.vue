<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageCourseRecordLearn,
  createCourseRecordLearn,
  updateCourseRecordLearn,
  deleteCourseRecordLearn
} from '@/api/course-sub'
import type { CourseRecordLearn, CourseRecordLearnQuery } from '@/types/course'
import { LearnStatus, LEARN_STATUS_OPTIONS } from '@/types/course'

/**
 * 学习记录 tab（按 courseCode 分组的 CRUD）。
 */
const props = defineProps<{ courseCode: string }>()

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<CourseRecordLearn, CourseRecordLearnQuery, number>(
  {
    page: pageCourseRecordLearn,
    create: createCourseRecordLearn,
    update: (id, data) => updateCourseRecordLearn(id, data),
    remove: deleteCourseRecordLearn
  },
  {
    initialQuery: { status: undefined },
    idKey: 'id',
    fixedParams: { courseCode: props.courseCode }
  }
)

const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<CourseRecordLearn>({
  courseCode: props.courseCode,
  learnerName: '',
  learnerPhone: '',
  clientCode: '',
  agentCode: '',
  totalLesson: 0,
  currentLesson: 0,
  learnProgress: 0,
  totalLearnTime: 0,
  isCompleted: 0,
  status: LearnStatus.LEARNING
})

const rules: FormRules<CourseRecordLearn> = {
  learnerName: [{ required: true, message: '请输入学员姓名', trigger: 'blur' }],
  totalLesson: [{ required: true, message: '请输入总课时', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    courseCode: props.courseCode,
    learnerName: '',
    learnerPhone: '',
    clientCode: '',
    agentCode: '',
    totalLesson: 0,
    currentLesson: 0,
    learnProgress: 0,
    totalLearnTime: 0,
    isCompleted: 0,
    status: LearnStatus.LEARNING
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: CourseRecordLearn) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, row)
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
      await createCourseRecordLearn(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateCourseRecordLearn(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: CourseRecordLearn) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除学员「${row.learnerName}」的学习记录吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteCourseRecordLearn(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

function statusLabel(s?: number) {
  return LEARN_STATUS_OPTIONS.find((o) => o.value === s)?.label ?? '-'
}
function statusTagType(s?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (s) {
    case LearnStatus.COMPLETED:
      return 'success'
    case LearnStatus.LEARNING:
      return 'warning'
    case LearnStatus.REFUNDED:
      return 'danger'
    default:
      return 'info'
  }
}

loadPage()
</script>

<template>
  <div>
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="学习状态" clearable style="width: 160px" @change="handleSearch">
        <el-option v-for="o in LEARN_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增学习记录</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="learnerName" label="学员" min-width="100" />
      <el-table-column prop="learnerPhone" label="手机号" min-width="120" />
      <el-table-column label="进度" width="160">
        <template #default="{ row }">
          <el-progress :percentage="row.learnProgress ?? 0" :stroke-width="12" />
          <span style="font-size: 12px; color: #999">{{ row.currentLesson ?? 0 }}/{{ row.totalLesson ?? 0 }} 课</span>
        </template>
      </el-table-column>
      <el-table-column prop="totalLearnTime" label="学习时长(分)" width="120" align="right" />
      <el-table-column prop="rating" label="评分" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="enrollTime" label="报名时间" min-width="160" show-overflow-tooltip />
      <el-table-column prop="lastLearnTime" label="最近学习" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: flex-end; margin-top: 12px">
      <el-pagination
        :current-page="query.current"
        :page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增学习记录' : '编辑学习记录'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-alert
          v-if="dialogType === 'create'"
          type="info"
          :closable="false"
          title="学习进度、完成状态、评分由系统自动计算，保存后自动更新"
          style="margin-bottom: 12px"
        />
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学员姓名" prop="learnerName">
              <el-input v-model="form.learnerName" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.learnerPhone" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户编码">
              <el-input v-model="form.clientCode" :disabled="dialogType === 'edit'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="代理人编码">
              <el-input v-model="form.agentCode" :disabled="dialogType === 'edit'" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总课时" prop="totalLesson">
              <el-input-number v-model="form.totalLesson" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="当前课">
              <el-input-number v-model="form.currentLesson" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="进度(%)" v-if="dialogType === 'edit'">
              <el-input-number v-model="form.learnProgress" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学习时长(分)">
              <el-input-number v-model="form.totalLearnTime" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in LEARN_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否完成" v-if="dialogType === 'edit'">
              <el-switch :model-value="form.isCompleted === 1" @change="(v: boolean) => (form.isCompleted = v ? 1 : 0)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评分" v-if="dialogType === 'edit'">
              <el-input-number v-model="form.rating" :min="1" :max="5" controls-position="right" style="width: 100%" />
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
</style>
