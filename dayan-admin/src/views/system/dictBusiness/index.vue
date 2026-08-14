<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageDictBusiness,
  createDictBusiness,
  updateDictBusiness,
  deleteDictBusiness
} from '@/api/dictBusiness'
import type { SystemDictBusiness, SystemDictBusinessQuery } from '@/types/dict'

/**
 * 业务字典管理页（独立菜单）。
 * 按 domain（业务域）+ dictType 组织，供各业务域维护专属字典项。
 * 主键 id（自增），update/delete 用 path id。
 */
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<SystemDictBusiness, SystemDictBusinessQuery, number>(
    {
      page: pageDictBusiness,
      create: createDictBusiness,
      update: (id, data) => updateDictBusiness(id, data),
      remove: deleteDictBusiness
    },
    {
      initialQuery: { dictType: '', domain: '' },
      idKey: 'id'
    }
  )

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

function defaultForm(): SystemDictBusiness {
  return {
    dictType: '',
    dictCode: '',
    dictName: '',
    dictValue: '',
    parentCode: '',
    domain: '',
    sortOrder: 0,
    status: 1,
    remark: ''
  }
}

const form = reactive<SystemDictBusiness>(defaultForm())

const rules: FormRules<SystemDictBusiness> = {
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }],
  dictCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }],
  domain: [{ required: true, message: '请输入业务域', trigger: 'blur' }]
}

function openCreate() {
  dialogMode.value = 'create'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEdit(row: SystemDictBusiness) {
  dialogMode.value = 'edit'
  Object.assign(form, defaultForm(), row)
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
    if (dialogMode.value === 'create') {
      await createDictBusiness(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateDictBusiness(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function onDelete(row: SystemDictBusiness) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除业务字典项「${row.dictName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteDictBusiness(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

function handleReset() {
  query.dictType = ''
  query.domain = ''
  handleSearch()
}

loadPage()
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="字典类型">
          <el-input v-model="query.dictType" placeholder="如 park_status" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="业务域">
          <el-input v-model="query.domain" placeholder="如 park/scene/order" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>业务字典</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="domain" label="业务域" width="110" />
        <el-table-column prop="dictType" label="字典类型" min-width="140" show-overflow-tooltip />
        <el-table-column prop="dictCode" label="编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="dictName" label="名称" min-width="130" />
        <el-table-column prop="dictValue" label="值" min-width="110" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增业务字典项' : '编辑业务字典项'"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="业务域" prop="domain">
              <el-input v-model="form.domain" placeholder="如 park" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字典类型" prop="dictType">
              <el-input v-model="form.dictType" placeholder="如 park_status" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字典编码" prop="dictCode">
              <el-input v-model="form.dictCode" :disabled="dialogMode === 'edit'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字典名称" prop="dictName">
              <el-input v-model="form.dictName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字典值" prop="dictValue">
              <el-input v-model="form.dictValue" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
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
