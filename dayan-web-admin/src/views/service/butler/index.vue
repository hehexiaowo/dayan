<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageButlers,
  getButler,
  createButler,
  updateButler,
  deleteButler
} from '@/api/service'
import type { ButlerInfo, ButlerInfoQuery } from '@/types/service'
import { BUTLER_LEVEL_OPTIONS } from '@/types/service'
import { COMMON_STATUS_OPTIONS } from '@/types/common'
import { formatDateTime } from '@/utils/format'

/**
 * 管家信息管理页。
 *
 * 字段最少的标准 CRUD 实体（仅 8 个业务字段）。
 * - butlerCode 服务端生成：新增表单不含 butlerCode，编辑时只读。
 * - status：1启用 / 0禁用（共用 COMMON_STATUS_OPTIONS）。
 * - butlerLevel：1初级 / 2中级 / 3高级 / 4专家。
 */

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<ButlerInfo, ButlerInfoQuery>(
  { page: pageButlers },
  {
    initialQuery: {
      butlerCode: '',
      fullName: '',
      phone: '',
      organCode: '',
      butlerLevel: undefined,
      status: undefined
    }
  }
)

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ButlerInfo>({
  butlerCode: undefined,
  fullName: '',
  phone: '',
  avatar: '',
  organCode: '',
  butlerLevel: undefined,
  status: 1,
  remark: ''
})

const rules: FormRules<ButlerInfo> = {
  fullName: [{ required: true, message: '请输入管家姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

function resetForm() {
  Object.assign(form, {
    butlerCode: undefined,
    fullName: '',
    phone: '',
    avatar: '',
    organCode: '',
    butlerLevel: undefined,
    status: 1,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: ButlerInfo) {
  if (!row.butlerCode) return
  dialogType.value = 'edit'
  resetForm()
  try {
    const detail = await getButler(row.butlerCode)
    Object.assign(form, {
      butlerCode: detail.butlerCode,
      fullName: detail.fullName ?? '',
      phone: detail.phone ?? '',
      avatar: detail.avatar ?? '',
      organCode: detail.organCode ?? '',
      butlerLevel: detail.butlerLevel,
      status: detail.status ?? 1,
      remark: detail.remark ?? ''
    })
  } catch {
    Object.assign(form, {
      butlerCode: row.butlerCode,
      fullName: row.fullName ?? '',
      phone: row.phone ?? '',
      avatar: row.avatar ?? '',
      organCode: row.organCode ?? '',
      butlerLevel: row.butlerLevel,
      status: row.status ?? 1,
      remark: row.remark ?? ''
    })
  }
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
      await createButler(form)
      ElMessage.success('新增成功')
    } else if (form.butlerCode) {
      await updateButler(form.butlerCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: ButlerInfo) {
  if (!row.butlerCode) return
  await ElMessageBox.confirm(`确定删除管家「${row.fullName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteButler(row.butlerCode)
  ElMessage.success('删除成功')
  loadPage()
}

function handleReset() {
  query.butlerCode = ''
  query.fullName = ''
  query.phone = ''
  query.organCode = ''
  query.butlerLevel = undefined
  query.status = undefined
  handleSearch()
}

// ---------- 辅助渲染 ----------
function butlerLevelLabel(level?: number): string {
  const found = BUTLER_LEVEL_OPTIONS.find((o) => o.value === level)
  return found ? found.label : level != null ? String(level) : '--'
}

function statusLabel(status?: number): string {
  const found = COMMON_STATUS_OPTIONS.find((o) => o.value === status)
  return found ? found.label : status != null ? String(status) : '--'
}

/** 状态 el-tag type：1启用 success / 0禁用 info。 */
function statusTagType(status?: number): 'success' | 'info' {
  return status === 1 ? 'success' : 'info'
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="管家编码">
          <el-input v-model="query.butlerCode" placeholder="管家编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="管家姓名">
          <el-input v-model="query.fullName" placeholder="姓名关键字" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="手机号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="所属组织">
          <el-input v-model="query.organCode" placeholder="组织编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="管家等级">
          <el-select v-model="query.butlerLevel" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in BUTLER_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in COMMON_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>管家列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增管家</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="butlerCode">
        <el-table-column prop="butlerCode" label="管家编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="fullName" label="姓名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="130" show-overflow-tooltip />
        <el-table-column prop="avatar" label="头像" min-width="120" show-overflow-tooltip />
        <el-table-column prop="organCode" label="所属组织" min-width="130" show-overflow-tooltip />
        <el-table-column prop="butlerLevel" label="等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ butlerLevelLabel(row.butlerLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
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
      :title="dialogType === 'create' ? '新增管家' : '编辑管家'"
      width="620px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col v-if="dialogType === 'edit'" :span="24">
            <el-form-item label="管家编码">
              <el-input v-model="form.butlerCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管家姓名" prop="fullName">
              <el-input v-model="form.fullName" placeholder="管家姓名" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="手机号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="头像URL">
              <el-input v-model="form.avatar" placeholder="头像图片地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属组织">
              <el-input v-model="form.organCode" placeholder="组织编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管家等级">
              <el-select v-model="form.butlerLevel" placeholder="管家等级" clearable style="width: 100%">
                <el-option v-for="o in BUTLER_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" placeholder="状态" style="width: 100%">
                <el-option v-for="o in COMMON_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="内部备注（可选）" />
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
