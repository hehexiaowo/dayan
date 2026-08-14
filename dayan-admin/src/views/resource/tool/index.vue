<script setup lang="ts">
/**
 * 获客工具管理页。
 *
 * - 标准 CRUD（搜索 + 表格 + 分页 + 新增/编辑弹窗）；
 * - 主键 toolCode 由服务端生成（TL 前缀），新增表单不含该字段；
 * - visibleScope 逗号分隔多值，表单用多选（数组）与字符串互转；
 * - config 为 JSON 字符串，用 textarea 原样编辑，提交前做 JSON 合法性校验；
 * - 路由由后端菜单（component='resource/tool/index'）自动解析，无需改路由。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { pageTools, createTool, updateTool, deleteTool } from '@/api/tool'
import type { ToolInfo, ToolInfoQuery } from '@/types/tool'
import { TOOL_TYPE_OPTIONS, TOOL_END_OPTIONS, toolTypeLabel } from '@/types/tool'
import { COMMON_STATUS_OPTIONS } from '@/types/common'
import { formatDateTime } from '@/utils/format'

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<ToolInfo, ToolInfoQuery>(
    { page: pageTools },
    {
      initialQuery: {
        toolName: '',
        toolType: undefined,
        status: undefined
      }
    }
  )

loadPage()

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 表单类型：visibleScope 在表单内用数组承载（多选），提交时转逗号分隔字符串 */
type ToolForm = Omit<ToolInfo, 'visibleScope'> & { visibleScope: string[] }

const form = reactive<ToolForm>({
  toolCode: undefined,
  toolName: '',
  toolType: 1,
  toolDesc: '',
  icon: '',
  entryPath: '',
  config: '',
  visibleScope: ['agent'],
  sortOrder: 0,
  status: 1,
  remark: ''
})

const rules: FormRules<ToolForm> = {
  toolName: [{ required: true, message: '请输入工具名称', trigger: 'blur' }],
  entryPath: [{ required: true, message: '请输入入口路径', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    toolCode: undefined,
    toolName: '',
    toolType: 1,
    toolDesc: '',
    icon: '',
    entryPath: '',
    config: '',
    visibleScope: ['agent'],
    sortOrder: 0,
    status: 1,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ToolInfo) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, {
    toolCode: row.toolCode,
    toolName: row.toolName ?? '',
    toolType: row.toolType ?? 1,
    toolDesc: row.toolDesc ?? '',
    icon: row.icon ?? '',
    entryPath: row.entryPath ?? '',
    config: row.config ?? '',
    visibleScope: row.visibleScope ? row.visibleScope.split(',').filter(Boolean) : ['agent'],
    sortOrder: row.sortOrder ?? 0,
    status: row.status ?? 1,
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
  // config 非空时校验 JSON 合法性
  if (form.config) {
    try {
      JSON.parse(form.config)
    } catch {
      ElMessage.error('工具配置不是合法 JSON')
      return
    }
  }
  const payload: Partial<ToolInfo> = {
    toolName: form.toolName,
    toolType: form.toolType,
    toolDesc: form.toolDesc || undefined,
    icon: form.icon || undefined,
    entryPath: form.entryPath,
    config: form.config || undefined,
    visibleScope: form.visibleScope.length ? form.visibleScope.join(',') : undefined,
    sortOrder: form.sortOrder,
    status: form.status,
    remark: form.remark || undefined
  }
  submitLoading.value = true
  try {
    if (dialogType.value === 'create') {
      await createTool(payload)
      ElMessage.success('新增成功')
    } else if (form.toolCode) {
      await updateTool(form.toolCode, payload)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: ToolInfo) {
  if (!row.toolCode) return
  await ElMessageBox.confirm(`确定删除工具「${row.toolName}」吗？删除后端上立即不可见。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteTool(row.toolCode)
  ElMessage.success('删除成功')
  loadPage()
}

/** 可见端 → 中文标签（逗号分隔多值逐个映射） */
function visibleScopeLabel(scope?: string): string {
  if (!scope) return '--'
  return scope
    .split(',')
    .filter(Boolean)
    .map((s) => TOOL_END_OPTIONS.find((o) => o.value === s)?.label ?? s)
    .join('、')
}
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="工具名称">
          <el-input v-model="query.toolName" placeholder="工具名称关键字" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="工具类型">
          <el-select v-model="query.toolType" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="o in TOOL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="o in COMMON_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button v-permission="'tool:info:create'" type="primary" :icon="'Plus'" @click="openCreate">
            新增工具
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe row-key="toolCode">
        <el-table-column prop="toolCode" label="工具编码" width="110" />
        <el-table-column prop="toolName" label="工具名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">{{ toolTypeLabel(row.toolType) }}</template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="70" align="center">
          <template #default="{ row }">{{ row.icon || '--' }}</template>
        </el-table-column>
        <el-table-column prop="entryPath" label="入口路径" min-width="230" show-overflow-tooltip />
        <el-table-column label="可见端" width="130">
          <template #default="{ row }">{{ visibleScopeLabel(row.visibleScope) }}</template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'tool:info:update'" link type="primary" size="small" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-permission="'tool:info:delete'" link type="danger" size="small" @click="handleDeleteRow(row)">
              删除
            </el-button>
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
      :title="dialogType === 'create' ? '新增工具' : '编辑工具'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col v-if="dialogType === 'edit'" :span="24">
            <el-form-item label="工具编码">
              <el-input v-model="form.toolCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工具名称" prop="toolName">
              <el-input v-model="form.toolName" placeholder="如：退休养老金计算器" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工具类型">
              <el-select v-model="form.toolType" style="width: 100%">
                <el-option v-for="o in TOOL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="入口路径" prop="entryPath">
              <el-input v-model="form.entryPath" placeholder="如：/pages/acquisition/tools/pension-calculator" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标">
              <el-input v-model="form.icon" placeholder="文字或图标名（可选）" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可见端">
              <el-select v-model="form.visibleScope" multiple collapse-tags style="width: 100%">
                <el-option v-for="o in TOOL_END_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
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
                <el-option v-for="o in COMMON_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="工具简介">
              <el-input v-model="form.toolDesc" type="textarea" :rows="2" placeholder="端上卡片展示的一句话简介" maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="工具配置">
              <el-input
                v-model="form.config"
                type="textarea"
                :rows="2"
                placeholder='JSON，如 {"color":"orange"}（可选）'
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="内部备注（可选）" maxlength="500" />
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
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
