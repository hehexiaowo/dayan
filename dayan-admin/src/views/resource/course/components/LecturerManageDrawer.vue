<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageCourseLecturers,
  createCourseLecturer,
  updateCourseLecturer,
  deleteCourseLecturer
} from '@/api/course-sub'
import type { CourseLecturer, CourseLecturerQuery } from '@/types/course'
import FileUploader from '@/components/FileUploader/index.vue'

/**
 * 讲师管理抽屉——课程管理页内嵌入口（无独立菜单）。
 * 全局讲师资源 CRUD；增删改成功后 emit('changed')，父页据此刷新讲师下拉。
 * 主键 id（自增），update 用 path id，lecturerCode 系统生成。
 * 父页用 v-if 挂载本组件：首次打开才发列表请求，关闭即销毁。
 */
const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'changed'): void
}>()

const GENDER_OPTIONS = [
  { label: '男', value: 1 },
  { label: '女', value: 2 },
  { label: '未知', value: 0 }
] as const

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<CourseLecturer, CourseLecturerQuery, number>(
    {
      page: pageCourseLecturers,
      create: createCourseLecturer,
      update: (id, data) => updateCourseLecturer(id, data),
      remove: deleteCourseLecturer
    },
    {
      initialQuery: { lecturerName: '', organization: '', status: undefined },
      idKey: 'id'
    }
  )

const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<CourseLecturer>({
  lecturerName: '',
  gender: 0,
  avatar: '',
  title: '',
  organization: '',
  specialty: '',
  introduction: '',
  certifications: '',
  phone: '',
  email: '',
  isCertified: 0,
  sortOrder: 0,
  status: 1
})

const rules: FormRules<CourseLecturer> = {
  lecturerName: [{ required: true, message: '请输入讲师姓名', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    lecturerCode: undefined,
    lecturerName: '',
    gender: 0,
    avatar: '',
    title: '',
    organization: '',
    specialty: '',
    introduction: '',
    certifications: '',
    phone: '',
    email: '',
    isCertified: 0,
    sortOrder: 0,
    status: 1
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: CourseLecturer) {
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
      await createCourseLecturer(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateCourseLecturer(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
    emit('changed')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: CourseLecturer) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除讲师「${row.lecturerName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteCourseLecturer(row.id)
  ElMessage.success('删除成功')
  loadPage()
  emit('changed')
}

function handleReset() {
  query.lecturerName = ''
  query.organization = ''
  query.status = undefined
  handleSearch()
}

function genderText(g?: number) {
  return GENDER_OPTIONS.find((o) => o.value === g)?.label ?? '未知'
}

loadPage()
</script>

<template>
  <el-drawer
    :model-value="props.modelValue"
    title="讲师管理"
    size="72%"
    append-to-body
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <div class="drawer-body">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="姓名">
          <el-input v-model="query.lecturerName" placeholder="讲师姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="机构">
          <el-input v-model="query.organization" placeholder="所属机构" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <span class="table-title">讲师列表</span>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增讲师</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column label="头像" width="70" align="center">
          <template #default="{ row }">
            <el-avatar v-if="row.avatar" :src="row.avatar" :size="36" />
            <el-avatar v-else :size="36">{{ (row.lecturerName || '?').charAt(0) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="lecturerName" label="姓名" min-width="100" />
        <el-table-column prop="lecturerCode" label="编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="title" label="头衔" min-width="120" show-overflow-tooltip />
        <el-table-column prop="organization" label="机构" min-width="140" show-overflow-tooltip />
        <el-table-column prop="gender" label="性别" width="70" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="courseCount" label="课程数" width="90" align="center" />
        <el-table-column prop="ratingAvg" label="评分" width="80" align="center" />
        <el-table-column prop="isCertified" label="认证" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isCertified === 1 ? 'success' : 'info'">{{ row.isCertified === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
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
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 新增 / 编辑讲师（叠在抽屉之上） -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增讲师' : '编辑讲师'"
      width="680px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="讲师编码">
              <el-input :model-value="dialogType === 'create' ? '保存时自动生成' : form.lecturerCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="lecturerName">
              <el-input v-model="form.lecturerName" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="头衔">
              <el-input v-model="form.title" maxlength="100" placeholder="如 高级讲师" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属机构">
              <el-input v-model="form.organization" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" style="width: 100%">
                <el-option v-for="o in GENDER_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="认证">
              <el-switch :model-value="form.isCertified === 1" @change="(v: boolean) => (form.isCertified = v ? 1 : 0)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="头像">
              <FileUploader v-model="form.avatar" type="image" module="course" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="擅长领域">
              <el-input v-model="form.specialty" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="简介">
              <el-input v-model="form.introduction" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="资质证书">
              <el-input
                v-model="form.certifications"
                type="textarea"
                :rows="3"
                placeholder='JSON 数组格式，如 ["高级保险规划师","CPB 认证私人银行家"]'
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<style scoped lang="scss">
.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.table-title {
  font-weight: 600;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
}
</style>
