<script setup lang="ts">
/**
 * 工具配置页。
 *
 * - 标准 CRUD（搜索 + 表格 + 分页 + 新增/编辑弹窗）；
 * - 主键 toolCode 由服务端生成（TL 前缀），新增表单不含该字段；
 * - 类型四类固定：pension/gap/aiartist/aichat；
 * - aichat 类型（你问我答）实例 = 一个人物：表单直接编辑人物属性
 *   （人设/开场白/头像/推荐问题/知识库），提交时组装为 config_json；
 * - aiartist 类型（AI 创作·文章转写）实例：表单编辑
 *   （目的/图标/人设），提交时组装为 config_json；
 * - 其余类型 configJson 为 JSON 字符串，用 textarea 原样编辑，提交前校验 JSON 合法性；
 * - 图标/颜色/页面路径等展示细节由端上按 tool_type 固定映射，此处不配置；
 * - 路由由后端菜单（component='resource/tool/index'）自动解析，无需改路由。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { pageTools, createTool, updateTool, deleteTool } from '@/api/tool'
import { pageKnowledgeRepos } from '@/api/knowledge'
import type { ToolInfo, ToolInfoQuery } from '@/types/tool'
import { TOOL_TYPE_OPTIONS, toolTypeLabel } from '@/types/tool'
import { COMMON_STATUS_OPTIONS } from '@/types/common'
import { formatDateTime } from '@/utils/format'

/** 工具图标颜色选项（aichat 人物 / aiartist 分类共用） */
const ICON_COLOR_OPTIONS = [
  { label: '蓝色', value: 'blue' },
  { label: '绿色', value: 'green' },
  { label: '橙色', value: 'orange' },
  { label: '红色', value: 'red' },
  { label: '灰色', value: 'gray' }
] as const

/** aiartist 创作目的选项（文章转写） */
const AIARTIST_PURPOSE_OPTIONS = [
  { label: '文章转写', value: 'rewrite' }
] as const


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

/** 重置筛选并重新查询 */
function handleReset() {
  Object.assign(query, { toolName: '', toolType: undefined, status: undefined })
  handleSearch()
}

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** aichat 类型的结构化人物属性（提交时组装进 configJson） */
type ToolForm = ToolInfo & {
  icon?: string
  iconColor?: string
  welcomeMsg?: string
  recommendQuestions: string[]
  repoIds: number[]
  systemPrompt?: string
  /** aiartist 创作目的（rewrite=文章转写） */
  purpose?: string
  /** aiartist 相关性标签（后台配置，供内容总结步骤选择） */
  relevanceTags: string[]
}

/** aiartist 相关性标签快捷选项（可自由输入新增） */
const RELEVANCE_TAG_OPTIONS = ['养老保险', '社区养老'] as const

/** 知识库选项（aichat 人物绑定用，全量拉取） */
const repoOptions = ref<{ id: number; repoName: string }[]>([])

function fetchRepoOptions() {
  let current = 1
  const list: { id: number; repoName: string }[] = []
  async function load() {
    try {
      // eslint-disable-next-line no-constant-condition
      while (true) {
        const res = await pageKnowledgeRepos({ current, size: 100 })
        list.push(...res.records.map((r) => ({ id: r.id!, repoName: r.repoName })))
        if (res.records.length === 0 || current * 100 >= res.total) break
        current++
      }
      repoOptions.value = list
    } catch {
      // 拉取失败静默，绑库下拉为空不影响其他配置
    }
  }
  void load()
}

onMounted(fetchRepoOptions)

/** 从 config_json 解析 aichat 人物属性（非法/缺失按空处理） */
function parsePersonaConfig(configJson?: string) {
  if (!configJson) return {}
  try {
    const cfg = JSON.parse(configJson)
    return {
      icon: cfg.icon ?? '',
      iconColor: cfg.iconColor ?? 'blue',
      welcomeMsg: cfg.welcomeMsg ?? '',
      recommendQuestions: Array.isArray(cfg.recommendQuestions) ? cfg.recommendQuestions : [],
      repoIds: Array.isArray(cfg.repoIds) ? cfg.repoIds : [],
      systemPrompt: cfg.systemPrompt ?? ''
    }
  } catch {
    return {}
  }
}

/** 从 config_json 解析 aiartist 分类属性（非法/缺失按空处理） */
function parseAiartistConfig(configJson?: string) {
  if (!configJson) return {}
  try {
    const cfg = JSON.parse(configJson)
    return {
      purpose: cfg.purpose ?? 'rewrite',
      icon: cfg.icon ?? '',
      iconColor: cfg.iconColor ?? 'blue',
      repoIds: Array.isArray(cfg.repoIds) ? cfg.repoIds : [],
      relevanceTags: Array.isArray(cfg.relevanceTags) ? cfg.relevanceTags : []
    }
  } catch {
    return {}
  }
}

const form = reactive<ToolForm>({
  toolCode: undefined,
  toolName: '',
  toolType: 'pension',
  toolDesc: '',
  configJson: '',
  status: 1,
  remark: '',
  icon: '',
  iconColor: 'blue',
  welcomeMsg: '',
  recommendQuestions: [],
  repoIds: [],
  systemPrompt: '',
  purpose: 'rewrite',
  relevanceTags: ['养老保险', '社区养老']
})

const rules: FormRules<ToolForm> = {
  toolName: [{ required: true, message: '请输入工具名称', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    toolCode: undefined,
    toolName: '',
    toolType: 'pension',
    toolDesc: '',
    configJson: '',
    status: 1,
    remark: '',
    icon: '',
    iconColor: 'blue',
    welcomeMsg: '',
    recommendQuestions: [],
    repoIds: [],
    systemPrompt: '',
    purpose: 'rewrite',
    relevanceTags: ['养老保险', '社区养老']
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
    toolType: row.toolType ?? 'pension',
    toolDesc: row.toolDesc ?? '',
    configJson: row.configJson ?? '',
    status: row.status ?? 1,
    remark: row.remark ?? '',
    ...(row.toolType === 'aichat' ? parsePersonaConfig(row.configJson) : {}),
    ...(row.toolType === 'aiartist' ? parseAiartistConfig(row.configJson) : {})
  })
  dialogVisible.value = true
}

/** 推荐问题增删 */
function addQuestion() {
  form.recommendQuestions.push('')
}
function removeQuestion(index: number) {
  form.recommendQuestions.splice(index, 1)
}

/** 组装 aichat 的 config_json（空值剔除） */
function buildPersonaConfigJson(): string {
  const cfg: Record<string, unknown> = {
    systemPrompt: form.systemPrompt
  }
  if (form.icon) cfg.icon = form.icon
  if (form.iconColor && form.iconColor !== 'blue') cfg.iconColor = form.iconColor
  if (form.welcomeMsg) cfg.welcomeMsg = form.welcomeMsg
  const questions = form.recommendQuestions.map((q) => q.trim()).filter(Boolean)
  if (questions.length) cfg.recommendQuestions = questions
  if (form.repoIds.length) cfg.repoIds = form.repoIds
  return JSON.stringify(cfg)
}

/** 组装 aiartist 的 config_json：基础字段（空值剔除） */
function buildAiartistConfigJson(): string {
  const cfg: Record<string, unknown> = {
    purpose: form.purpose || 'rewrite'
  }
  if (form.icon) cfg.icon = form.icon
  if (form.iconColor && form.iconColor !== 'blue') cfg.iconColor = form.iconColor
  if (form.systemPrompt?.trim()) cfg.systemPrompt = form.systemPrompt
  if (form.repoIds.length) cfg.repoIds = form.repoIds
  if (form.relevanceTags.length) cfg.relevanceTags = form.relevanceTags
  return JSON.stringify(cfg)
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (form.toolType === 'aichat') {
    // 人设必填；configJson 由结构化字段组装
    if (!form.systemPrompt?.trim()) {
      ElMessage.error('你问我答类型必须填写人设描述')
      return
    }
  } else if (form.toolType === 'aiartist') {
    // 目的下拉有默认值，无需额外校验；configJson 由结构化字段组装
  } else if (form.configJson) {
    try {
      JSON.parse(form.configJson)
    } catch {
      ElMessage.error('工具配置不是合法 JSON')
      return
    }
  }
  const payload: Partial<ToolInfo> = {
    toolName: form.toolName,
    toolType: form.toolType,
    toolDesc: form.toolDesc || undefined,
    configJson:
      form.toolType === 'aichat'
        ? buildPersonaConfigJson()
        : form.toolType === 'aiartist'
          ? buildAiartistConfigJson()
          : form.configJson || undefined,
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
</script>

<template>
  <div class="page-container">
    <!-- 工具列表：搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input
          v-model="query.toolName"
          placeholder="工具名称"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.toolType" placeholder="工具类型" clearable style="width: 130px">
          <el-option v-for="o in TOOL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
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
          <span class="card-title">工具配置</span>
          <el-button v-permission="'tool:info:create'" type="primary" :icon="'Plus'" @click="openCreate">新增工具</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe row-key="toolCode">
        <el-table-column prop="toolCode" label="工具编码" width="120" />
        <el-table-column prop="toolName" label="工具名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="工具类型" width="130" align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ toolTypeLabel(row.toolType) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="175" align="center">
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
              <el-input
                v-model="form.toolName"
                :placeholder="
                  form.toolType === 'aichat'
                    ? '即人物名称，如：养老规划师'
                    : form.toolType === 'aiartist'
                      ? '如：AI 创作（主题创作）'
                      : '如：AI 创作（公众号）'
                "
                maxlength="100"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工具类型">
              <el-select v-model="form.toolType" style="width: 100%">
                <el-option v-for="o in TOOL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
            <el-form-item label="工具简介">
              <el-input v-model="form.toolDesc" type="textarea" :rows="2" placeholder="端上卡片展示的一句话简介" maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.toolType === 'aichat'" :span="24">
            <el-form-item label="人设描述" required>
              <el-input
                v-model="form.systemPrompt"
                type="textarea"
                :rows="3"
                placeholder="问答人物的角色设定，如：你是资深养老规划师，擅长为客户测算养老金缺口……"
              />
            </el-form-item>
          </el-col>
          <el-col v-if="form.toolType === 'aichat'" :span="24">
            <el-form-item label="开场白">
              <el-input v-model="form.welcomeMsg" type="textarea" :rows="2" placeholder="端上人物卡片展示的欢迎语（可选）" maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.toolType === 'aichat'" :span="12">
            <el-form-item label="头像文字">
              <el-input v-model="form.icon" placeholder="如：养（不填取人物名首字）" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.toolType === 'aichat'" :span="12">
            <el-form-item label="头像颜色">
              <el-select v-model="form.iconColor" style="width: 100%">
                <el-option v-for="o in ICON_COLOR_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="form.toolType === 'aichat'" :span="24">
            <el-form-item label="推荐问题">
              <div class="qa-questions">
                <div v-for="(_, i) in form.recommendQuestions" :key="i" class="qa-question-row">
                  <el-input v-model="form.recommendQuestions[i]" placeholder="推荐问题（可选，端上可点击直接提问）" maxlength="100" />
                  <el-button link type="danger" @click="removeQuestion(i)">删除</el-button>
                </div>
                <el-button link type="primary" @click="addQuestion">+ 添加推荐问题</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col v-if="form.toolType === 'aichat'" :span="24">
            <el-form-item label="绑定知识库">
              <el-select v-model="form.repoIds" multiple filterable collapse-tags style="width: 100%">
                <el-option v-for="r in repoOptions" :key="r.id" :label="r.repoName" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <template v-else-if="form.toolType === 'aiartist'">
            <el-col :span="12">
              <el-form-item label="创作目的" required>
                <el-select v-model="form.purpose" style="width: 100%">
                  <el-option v-for="o in AIARTIST_PURPOSE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="分类图标">
                <el-input v-model="form.icon" placeholder="如：转（不填取分类名首字）" maxlength="20" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="人设描述">
                <el-input
                  v-model="form.systemPrompt"
                  type="textarea"
                  :rows="3"
                  placeholder="创作分类的角色设定（可选）"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="图标颜色">
                <el-select v-model="form.iconColor" style="width: 100%">
                  <el-option v-for="o in ICON_COLOR_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="相关性标签">
                <el-select
                  v-model="form.relevanceTags"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                  :reserve-keyword="false"
                  style="width: 100%"
                >
                  <el-option v-for="t in RELEVANCE_TAG_OPTIONS" :key="t" :label="t" :value="t" />
                </el-select>
                <div class="form-tip">内容总结步骤展示给用户选择的相关性标签（可回车输入新增）。</div>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="绑定知识库">
                <el-select v-model="form.repoIds" multiple filterable collapse-tags style="width: 100%">
                  <el-option v-for="r in repoOptions" :key="r.id" :label="r.repoName" :value="r.id" />
                </el-select>
                <div class="form-tip">绑定知识库作为创作素材补充（留空则不检索）。</div>
              </el-form-item>
            </el-col>
          </template>
          <el-col v-else :span="24">
            <el-form-item label="工具配置">
              <el-input
                v-model="form.configJson"
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
.qa-questions {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.qa-question-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.form-tip {
  font-size: 12px;
  color: #8a919f;
  line-height: 1.6;
  margin-top: 4px;
}
/* 表格单元格统一单行：溢出省略，不换行 */
:deep(.el-table .cell) {
  white-space: nowrap;
}
</style>
