<script setup lang="ts">
/**
 * AI 问答人物配置子页（工具管理 Tab）。
 *
 * - 标准 CRUD（搜索 + 表格 + 分页 + 新增/编辑弹窗）；
 * - 主键为自增 id，新增表单不含 id；
 * - 推荐问题为「可动态增删的 string[]」；
 * - 绑定知识库为多选下拉（复用已有分页接口拉 id+repoName，label=repoName value=id）。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { pageQaConfigs, createQaConfig, updateQaConfig, deleteQaConfig, fetchRepoOptions } from '@/api/toolQa'
import type { ToolAiQaConfig, ToolAiQaConfigQuery } from '@/types/toolQa'
import { QA_ICON_COLOR_OPTIONS, iconColorTagType, qaStatusLabel, qaStatusTagType } from '@/types/toolQa'
import { COMMON_STATUS_OPTIONS } from '@/types/common'
import { formatDateTime } from '@/utils/format'

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<ToolAiQaConfig, ToolAiQaConfigQuery>(
    { page: pageQaConfigs },
    {
      idKey: 'id',
      initialQuery: {
        personaName: '',
        status: undefined
      }
    }
  )

loadPage()

/** 重置筛选并重新查询 */
function handleReset() {
  Object.assign(query, { personaName: '', status: undefined })
  handleSearch()
}

// ---------- 知识库绑定选项 ----------
const repoOptions = ref<{ id: number; repoName: string }[]>([])
const repoById = computed(() => new Map(repoOptions.value.map((r) => [r.id, r])))
const repoNameOf = (id: number) => repoById.value.get(id)?.repoName ?? `#${id}`

async function loadRepoOptions() {
  try {
    repoOptions.value = await fetchRepoOptions()
  } catch {
    // 失败时保持为空 select，错误提示由全局拦截器处理
    repoOptions.value = []
  }
}

onMounted(loadRepoOptions)

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
/** 编辑中的记录 id（新增时为 undefined） */
const editingId = ref<number>()

type QaForm = {
  configCode?: string
  personaName: string
  icon: string
  iconColor: string
  systemPrompt: string
  welcomeMsg: string
  recommendQuestions: string[]
  repoIds: number[]
  sortOrder: number
  status: number
  remark: string
}

const form = reactive<QaForm>({
  configCode: undefined,
  personaName: '',
  icon: '',
  iconColor: 'blue',
  systemPrompt: '',
  welcomeMsg: '',
  recommendQuestions: [''],
  repoIds: [],
  sortOrder: 0,
  status: 1,
  remark: ''
})

const rules: FormRules<QaForm> = {
  personaName: [{ required: true, message: '请输入人物名称', trigger: 'blur' }],
  systemPrompt: [{ required: true, message: '请输入人设描述', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    configCode: undefined,
    personaName: '',
    icon: '',
    iconColor: 'blue',
    systemPrompt: '',
    welcomeMsg: '',
    recommendQuestions: [''],
    repoIds: [],
    sortOrder: 0,
    status: 1,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  editingId.value = undefined
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ToolAiQaConfig) {
  dialogType.value = 'edit'
  editingId.value = row.id
  resetForm()
  const questions = row.recommendQuestions?.length ? row.recommendQuestions : ['']
  Object.assign(form, {
    configCode: row.configCode,
    personaName: row.personaName ?? '',
    icon: row.icon ?? '',
    iconColor: row.iconColor ?? 'blue',
    systemPrompt: row.systemPrompt ?? '',
    welcomeMsg: row.welcomeMsg ?? '',
    recommendQuestions: [...questions],
    repoIds: row.repoIds ? [...row.repoIds.map(Number)] : [],
    sortOrder: row.sortOrder ?? 0,
    status: row.status ?? 1,
    remark: row.remark ?? ''
  })
  dialogVisible.value = true
}

/** 推荐问题：追加一条 */
function addQuestion() {
  form.recommendQuestions.push('')
}

/** 推荐问题：删除指定下标的条目 */
function removeQuestion(index: number) {
  form.recommendQuestions.splice(index, 1)
}

/** 推荐问题为空条目时（全部清空）保证至少保留一个输入框 */
function normalizeQuestions(questions: string[]): string[] {
  const q = questions.map((s) => (s ?? '').trim()).filter(Boolean)
  return q
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const payload: Partial<ToolAiQaConfig> = {
    personaName: form.personaName,
    icon: form.icon || undefined,
    iconColor: form.iconColor || undefined,
    systemPrompt: form.systemPrompt,
    welcomeMsg: form.welcomeMsg || undefined,
    recommendQuestions: normalizeQuestions(form.recommendQuestions) || undefined,
    repoIds: form.repoIds.length ? form.repoIds : undefined,
    sortOrder: form.sortOrder,
    status: form.status,
    remark: form.remark || undefined
  }
  submitLoading.value = true
  try {
    if (dialogType.value === 'create') {
      await createQaConfig(payload)
      ElMessage.success('新增成功')
    } else if (editingId.value) {
      await updateQaConfig(editingId.value, payload)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: ToolAiQaConfig) {
  await ElMessageBox.confirm(`确定删除人物「${row.personaName}」吗？删除后不可恢复。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteQaConfig(row.id)
  ElMessage.success('删除成功')
  loadPage()
}
</script>

<template>
  <div class="qa-config">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input
          v-model="query.personaName"
          placeholder="人物名称"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
          <el-option v-for="o in COMMON_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span class="card-title">AI 问答人物</span>
          <el-button v-permission="'tool:qa:create'" type="primary" :icon="'Plus'" @click="openCreate">
            新增人物
          </el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="configCode" label="配置编码" width="120" />
        <el-table-column label="人物名称" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="persona-cell">
              <el-tag v-if="row.icon" :type="iconColorTagType(row.iconColor)" size="small" effect="dark" class="persona-icon">
                {{ row.icon }}
              </el-tag>
              <span>{{ row.personaName }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="绑定知识库" width="130" align="center">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.repoIds && row.repoIds.length"
              :content="row.repoIds.map(repoNameOf).join('、')"
              placement="top"
            >
              <span>{{ row.repoIds.length }} 个</span>
            </el-tooltip>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="qaStatusTagType(row.status)" size="small">{{ qaStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'tool:qa:update'" link type="primary" size="small" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-permission="'tool:qa:delete'" link type="danger" size="small" @click="handleDeleteRow(row)">
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
      :title="dialogType === 'create' ? '新增人物' : '编辑人物'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col v-if="dialogType === 'edit'" :span="24">
            <el-form-item label="配置编码">
              <el-input v-model="form.configCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人物名称" prop="personaName">
              <el-input v-model="form.personaName" placeholder="如：养老规划顾问" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number
                v-model="form.sortOrder"
                :min="0"
                :max="9999"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="头像图标">
              <el-input v-model="form.icon" placeholder="文字或图标名（可选）" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标色">
              <el-select v-model="form.iconColor" style="width: 100%">
                <el-option v-for="o in QA_ICON_COLOR_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="人设描述" prop="systemPrompt">
              <el-input
                v-model="form.systemPrompt"
                type="textarea"
                :rows="3"
                placeholder="注入 AI 的 system prompt，描述人物角色、语气与回答边界"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="开场白">
              <el-input v-model="form.welcomeMsg" type="textarea" :rows="2" placeholder="欢迎语/开场白（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="推荐问题">
              <div class="question-editor">
                <div v-for="(_, index) in form.recommendQuestions" :key="index" class="question-row">
                  <el-input
                    v-model="form.recommendQuestions[index]"
                    placeholder="如：怎么帮爸妈规划养老金？"
                    maxlength="100"
                  />
                  <el-button
                    type="danger"
                    link
                    :disabled="form.recommendQuestions.length <= 1"
                    @click="removeQuestion(index)"
                  >
                    删除
                  </el-button>
                </div>
                <el-button type="primary" link :icon="'Plus'" @click="addQuestion">添加一条</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="绑定知识库">
              <el-select v-model="form.repoIds" multiple collapse-tags clearable style="width: 100%">
                <el-option v-for="r in repoOptions" :key="r.id" :label="r.repoName" :value="r.id" />
              </el-select>
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
.qa-config {
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
.persona-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;

  .persona-icon {
    min-width: 24px;
    text-align: center;
  }
}
.question-editor {
  width: 100%;

  .question-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }
}
</style>
