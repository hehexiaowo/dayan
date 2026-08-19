<script setup lang="ts">
/**
 * 工具配置页。
 *
 * - 标准 CRUD（搜索 + 表格 + 分页 + 新增/编辑弹窗）；
 * - 主键 toolCode 由服务端生成（TL 前缀），新增表单不含该字段；
 * - 类型四类固定：pension/gap/aiartist/aichat；
 * - aichat 类型（你问我答）实例 = 一个人物：表单直接编辑人物属性
 *   （人设/开场白/头像/推荐问题/知识库），提交时组装为 config_json；
 * - aiartist 类型（AI 创作）实例 = 一个创作分类：表单编辑
 *   （目的/人设/图标/流水线配置 pipeline），提交时组装为 config_json；
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

/** aiartist 创作目的选项（与后端 AiPurpose 一致） */
const AIARTIST_PURPOSE_OPTIONS = [
  { label: '主题创作（内容转写）', value: 'science' },
  { label: '机构介绍（机构总结）', value: 'park' },
  { label: '保险计划（计划书重组）', value: 'product' }
] as const

/** aiartist 流水线配置（结构化表单，提交时组装为 config_json.pipeline；与后端 ToolAiartistPipelineConfig 一致） */
interface PipelineForm {
  temps: Record<'digest' | 'strategy' | 'titles' | 'outline' | 'body' | 'audit' | 'polish' | 'revise', number>
  bannedPhrases: string
  materialMax: number
  titleCountLimit: number
  scoreMin: number
  scoreMax: number
  polishKeepRatio: number
  imagePollTimeoutMs: number
  imageRetryAfterFailures: number
  imageFallbackPrompt: string
  coverSizeDefault: string
  coverSizeXhs: string
  nodeSize: string
  titleLimits: Record<'1' | '2' | '3' | '4', number>
  lengthWindows: Record<'1' | '2' | '3' | '4', { min: number; max: number }>
  imageCountHints: Record<'1' | '3' | '4', string>
  formInstructions: Record<'1' | '2' | '3' | '4', string>
  styleInstructions: Record<'professional' | 'warm' | 'authoritative' | 'colloquial', string>
  audienceInstructions: Record<'children' | 'elder' | 'general', string>
  /** 全局系统提示词（pipeline.system；留空用内置默认） */
  system: string
  /** 目的规则（pipeline.purposeRules：science/park/product；留空用内置默认） */
  purposeRules: Record<string, string>
  /** 平台规则（pipeline.platformRules：mp/xhs/moment/script；留空用内置默认） */
  platformRules: Record<string, string>
  prompts: Record<string, string>
}

/** 流水线默认值（与后端 ToolAiartistPipelineConfig 一致，迁移 87 种子） */
const PIPELINE_DEFAULT: PipelineForm = {
  temps: { digest: 0.2, strategy: 0.7, titles: 0.7, outline: 0.5, body: 0.6, audit: 0.2, polish: 0.5, revise: 0.3 },
  bannedPhrases: '保证收益\n稳赚\n包赚\n最高级\n国家级\n顶级\n100%\n百分百\n绝对\n秒杀\n史上',
  materialMax: 8000,
  titleCountLimit: 5,
  scoreMin: 70,
  scoreMax: 99,
  polishKeepRatio: 0.95,
  imagePollTimeoutMs: 90000,
  imageRetryAfterFailures: 2,
  imageFallbackPrompt:
    'Warm lifestyle photograph, elderly care concept related to: {promptZh}, single subject, shallow depth of field',
  coverSizeDefault: '1024*1024',
  coverSizeXhs: '1080*1440',
  nodeSize: '1280*720',
  titleLimits: { '1': 30, '2': 20, '3': 15, '4': 20 },
  lengthWindows: {
    '1': { min: 800, max: 2500 },
    '2': { min: 30, max: 400 },
    '3': { min: 400, max: 2500 },
    '4': { min: 350, max: 1500 }
  },
  imageCountHints: {
    '1': 'coverImage 1 张（1024*1024）+ 正文节点配图 3-4 张（1280*720）',
    '3': '仅规划 coverImage 1 张（1024*1024），所有 nodes 的 imageInsertion 必须为 null',
    '4': 'coverImage 1 张（1080*1440）+ 节点配位合计 2-4 张（1280*720）'
  },
  formInstructions: {
    '1': '微信公众号精品图文（1200-1500 字，HTML 片段 <h2>/<p>，标题 ≤30 字）',
    '2': '朋友圈文案（≤200 字纯文本 + 1-2 emoji + 1 个 #话题标签，标题=首句钩子 ≤20 字）',
    '3': '短视频口播脚本（60-90 秒，【画面】【口播】【字幕】分镜，标题 ≤15 字）',
    '4': '小红书笔记（600-800 字，Emoji 列表 + #标签段，标题 ≤20 字）'
  },
  styleInstructions: {
    professional: '专业科普风格：用词严谨、逻辑清晰、多用数据与术语，面向对养老品质有要求的家庭决策者',
    warm: '温情软文风格：以长辈/家庭的真实生活场景切入，情感细腻、语气温暖，引发共鸣',
    authoritative: '权威报告风格：结论先行、分点论述、数据化表达，塑造平台专业可信形象',
    colloquial: '口语化风格：短句、亲切、像朋友聊天'
  },
  audienceInstructions: {
    children: '为父母养老做决策的子女（30-50 岁）：理性、数据与家庭责任视角，专业可信赖',
    elder: '老人本人（55-75 岁）：直白温暖、短句、从老人自身利益出发，避免术语',
    general: '40-70 岁客户及其子女：通俗易懂'
  },
  system: '',
  purposeRules: { science: '', park: '', product: '' },
  platformRules: { mp: '', xhs: '', moment: '', script: '' },
  prompts: { digest: '', strategy: '', 'titles-regen': '', outline: '', body: '', audit: '', polish: '', revise: '' }
}

/** 温度/标题字数/篇幅窗口字段展示配置 */
const TEMP_FIELDS = [
  { key: 'digest', label: '素材消化' },
  { key: 'strategy', label: '策略' },
  { key: 'titles', label: '标题' },
  { key: 'outline', label: '大纲' },
  { key: 'body', label: '正文' },
  { key: 'audit', label: '审计' },
  { key: 'polish', label: '润色' },
  { key: 'revise', label: '勘误' }
] as const

const TITLE_LIMIT_FIELDS = [
  { key: '1', label: '图文' },
  { key: '2', label: '朋友圈' },
  { key: '3', label: '视频脚本' },
  { key: '4', label: '小红书' }
] as const

const FORM_INSTRUCTION_FIELDS = TITLE_LIMIT_FIELDS

const STYLE_INSTRUCTION_FIELDS = [
  { key: 'professional', label: '专业科普' },
  { key: 'warm', label: '温情软文' },
  { key: 'authoritative', label: '权威报告' },
  { key: 'colloquial', label: '口语化' }
] as const

const AUDIENCE_INSTRUCTION_FIELDS = [
  { key: 'children', label: '子女决策者' },
  { key: 'elder', label: '老人本人' },
  { key: 'general', label: '通用人群' }
] as const

const IMAGE_COUNT_HINT_FIELDS = [
  { key: '1', label: '图文/朋友圈' },
  { key: '3', label: '视频脚本' },
  { key: '4', label: '小红书' }
] as const

const PROMPT_FIELDS = [
  { key: 'digest', label: '素材消化 digest' },
  { key: 'strategy', label: '策略 strategy' },
  { key: 'titles-regen', label: '标题重出 titles-regen' },
  { key: 'outline', label: '大纲 outline' },
  { key: 'body', label: '正文 body' },
  { key: 'audit', label: '审计 audit' },
  { key: 'polish', label: '润色 polish' },
  { key: 'revise', label: '勘误 revise' }
] as const

/** 平台规则字段（pipeline.platformRules） */
const PLATFORM_RULE_FIELDS = [
  { key: 'mp', label: '公众号图文' },
  { key: 'xhs', label: '小红书' },
  { key: 'moment', label: '朋友圈' },
  { key: 'script', label: '视频脚本' }
] as const

/** 深拷贝默认流水线（resetForm 用） */
function defaultPipeline(): PipelineForm {
  return JSON.parse(JSON.stringify(PIPELINE_DEFAULT))
}

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
  /** aiartist 创作目的（science/park/product） */
  purpose?: string
  /** aiartist 流水线配置（结构化表单，提交时组装为 config_json.pipeline） */
  pipeline?: PipelineForm
}

/** 知识库选项（aichat 人物绑定用，全量拉取） */
const repoOptions = ref<{ id: number; repoName: string }[]>([])

/** aiartist 流水线折叠面板默认展开的分组 */
const pipelineCollapse = ref(['temps', 'limits', 'images'])

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
      purpose: cfg.purpose ?? 'science',
      icon: cfg.icon ?? '',
      iconColor: cfg.iconColor ?? 'blue',
      systemPrompt: cfg.systemPrompt ?? '',
      repoIds: Array.isArray(cfg.repoIds) ? cfg.repoIds : [],
      pipeline: toPipelineForm(cfg.pipeline)
    }
  } catch {
    return {}
  }
}

/** 流水线配置解析：config 值打底默认值（缺失字段回落内置默认） */
function toPipelineForm(pipeline: Record<string, unknown> | undefined): PipelineForm {
  const d = PIPELINE_DEFAULT
  const temps = { ...d.temps, ...((pipeline?.temps ?? {}) as Record<string, number>) } as PipelineForm['temps']
  const tl = { ...d.titleLimits, ...((pipeline?.titleLimits ?? {}) as Record<string, number>) } as PipelineForm['titleLimits']
  const lw = (pipeline?.lengthWindows ?? {}) as Record<string, number[]>
  const fi = { ...d.formInstructions, ...((pipeline?.formInstructions ?? {}) as Record<string, string>) } as PipelineForm['formInstructions']
  const si = { ...d.styleInstructions, ...((pipeline?.styleInstructions ?? {}) as Record<string, string>) } as PipelineForm['styleInstructions']
  const ai = { ...d.audienceInstructions, ...((pipeline?.audienceInstructions ?? {}) as Record<string, string>) } as PipelineForm['audienceInstructions']
  const ih = { ...d.imageCountHints, ...((pipeline?.imageCountHints ?? {}) as Record<string, string>) } as PipelineForm['imageCountHints']
  const pr = { ...d.prompts, ...((pipeline?.prompts ?? {}) as Record<string, string>) }
  const pur = { ...d.purposeRules, ...((pipeline?.purposeRules ?? {}) as Record<string, string>) }
  const plf = { ...d.platformRules, ...((pipeline?.platformRules ?? {}) as Record<string, string>) }
  const sr = Array.isArray(pipeline?.scoreRange) ? (pipeline.scoreRange as number[]) : []
  return {
    temps,
    bannedPhrases: Array.isArray(pipeline?.bannedPhrases)
      ? (pipeline.bannedPhrases as string[]).join('\n')
      : d.bannedPhrases,
    materialMax: (pipeline?.materialMax as number) ?? d.materialMax,
    titleCountLimit: (pipeline?.titleCountLimit as number) ?? d.titleCountLimit,
    scoreMin: sr[0] ?? d.scoreMin,
    scoreMax: sr[1] ?? d.scoreMax,
    polishKeepRatio: (pipeline?.polishKeepRatio as number) ?? d.polishKeepRatio,
    imagePollTimeoutMs: (pipeline?.imagePollTimeoutMs as number) ?? d.imagePollTimeoutMs,
    imageRetryAfterFailures: (pipeline?.imageRetryAfterFailures as number) ?? d.imageRetryAfterFailures,
    imageFallbackPrompt: (pipeline?.imageFallbackPrompt as string) ?? d.imageFallbackPrompt,
    coverSizeDefault: (pipeline?.coverSizeDefault as string) ?? d.coverSizeDefault,
    coverSizeXhs: (pipeline?.coverSizeXhs as string) ?? d.coverSizeXhs,
    nodeSize: (pipeline?.nodeSize as string) ?? d.nodeSize,
    titleLimits: tl,
    lengthWindows: {
      '1': { min: lw['1']?.[0] ?? d.lengthWindows['1'].min, max: lw['1']?.[1] ?? d.lengthWindows['1'].max },
      '2': { min: lw['2']?.[0] ?? d.lengthWindows['2'].min, max: lw['2']?.[1] ?? d.lengthWindows['2'].max },
      '3': { min: lw['3']?.[0] ?? d.lengthWindows['3'].min, max: lw['3']?.[1] ?? d.lengthWindows['3'].max },
      '4': { min: lw['4']?.[0] ?? d.lengthWindows['4'].min, max: lw['4']?.[1] ?? d.lengthWindows['4'].max }
    },
    imageCountHints: ih,
    formInstructions: fi,
    styleInstructions: si,
    audienceInstructions: ai,
    system: (pipeline?.system as string) ?? d.system,
    purposeRules: pur,
    platformRules: plf,
    prompts: pr
  }
}

/** 结构化表单 → config_json.pipeline 对象（禁语拆行、评分组回数组、空提示词剔除） */
function toPipelineObject(f: PipelineForm): Record<string, unknown> {
  const prompts = Object.fromEntries(Object.entries(f.prompts).filter(([, v]) => v.trim()))
  const purposeRules = Object.fromEntries(Object.entries(f.purposeRules).filter(([, v]) => v.trim()))
  const platformRules = Object.fromEntries(Object.entries(f.platformRules).filter(([, v]) => v.trim()))
  return {
    temps: f.temps,
    bannedPhrases: f.bannedPhrases.split('\n').map((s) => s.trim()).filter(Boolean),
    materialMax: f.materialMax,
    titleCountLimit: f.titleCountLimit,
    scoreRange: [f.scoreMin, f.scoreMax],
    polishKeepRatio: f.polishKeepRatio,
    imagePollTimeoutMs: f.imagePollTimeoutMs,
    imageRetryAfterFailures: f.imageRetryAfterFailures,
    imageFallbackPrompt: f.imageFallbackPrompt,
    coverSizeDefault: f.coverSizeDefault,
    coverSizeXhs: f.coverSizeXhs,
    nodeSize: f.nodeSize,
    titleLimits: f.titleLimits,
    lengthWindows: {
      '1': [f.lengthWindows['1'].min, f.lengthWindows['1'].max],
      '2': [f.lengthWindows['2'].min, f.lengthWindows['2'].max],
      '3': [f.lengthWindows['3'].min, f.lengthWindows['3'].max],
      '4': [f.lengthWindows['4'].min, f.lengthWindows['4'].max]
    },
    imageCountHints: f.imageCountHints,
    formInstructions: f.formInstructions,
    styleInstructions: f.styleInstructions,
    audienceInstructions: f.audienceInstructions,
    ...(f.system.trim() ? { system: f.system } : {}),
    ...(Object.keys(purposeRules).length ? { purposeRules } : {}),
    ...(Object.keys(platformRules).length ? { platformRules } : {}),
    prompts
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
  purpose: 'science',
  pipeline: defaultPipeline()
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
    purpose: 'science',
    pipeline: defaultPipeline()
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

/** 组装 aiartist 的 config_json：基础字段 + 知识库绑定 + 流水线配置（结构化表单组装） */
function buildAiartistConfigJson(): string {
  const cfg: Record<string, unknown> = {
    purpose: form.purpose || 'science',
    systemPrompt: form.systemPrompt
  }
  if (form.icon) cfg.icon = form.icon
  if (form.iconColor && form.iconColor !== 'blue') cfg.iconColor = form.iconColor
  if (form.repoIds.length) cfg.repoIds = form.repoIds
  cfg.pipeline = toPipelineObject(form.pipeline ?? defaultPipeline())
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
    // 目的与人设必填；流水线配置由结构化字段组装（无需 JSON 校验）
    if (!form.systemPrompt?.trim()) {
      ElMessage.error('AI 创作类型必须填写人设描述')
      return
    }
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
                <el-input v-model="form.icon" placeholder="如：主（不填取分类名首字）" maxlength="20" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="人设描述" required>
                <el-input
                  v-model="form.systemPrompt"
                  type="textarea"
                  :rows="3"
                  placeholder="创作分类的角色设定，注入流水线各阶段提示词（必填）"
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
              <el-form-item label="绑定知识库">
                <el-select v-model="form.repoIds" multiple filterable collapse-tags style="width: 100%">
                  <el-option v-for="r in repoOptions" :key="r.id" :label="r.repoName" :value="r.id" />
                </el-select>
                <div class="form-tip">正文生成前自动检索补充：以策略+大纲为 query 检索绑定仓库，结果作为事实供给与审计基准（留空则不检索）。</div>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="流水线配置">
                <el-collapse v-model="pipelineCollapse" class="pipeline-collapse">
                  <!-- ① 温度 -->
                  <el-collapse-item title="① 各阶段温度（LLM 随机性，越高越有创造性）" name="temps">
                    <el-row :gutter="16">
                      <el-col v-for="t in TEMP_FIELDS" :key="t.key" :span="12">
                        <el-form-item :label="t.label + ' 温度'">
                          <el-input-number
                            v-model="form.pipeline!.temps[t.key]"
                            :min="0"
                            :max="1.5"
                            :step="0.05"
                            :precision="2"
                            controls-position="right"
                            style="width: 100%"
                          />
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </el-collapse-item>
                  <!-- ② 合规与生成限制 -->
                  <el-collapse-item title="② 合规与生成限制" name="limits">
                    <el-form-item label="合规禁语">
                      <el-input
                        v-model="form.pipeline!.bannedPhrases"
                        type="textarea"
                        :rows="3"
                        placeholder="每行一条，正文自检命中会提示人工复核"
                      />
                    </el-form-item>
                    <el-row :gutter="16">
                      <el-col :span="12">
                        <el-form-item label="素材上限">
                          <el-input-number v-model="form.pipeline!.materialMax" :min="1000" :max="20000" :step="500" controls-position="right" style="width: 100%" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="标题条数上限">
                          <el-input-number v-model="form.pipeline!.titleCountLimit" :min="1" :max="10" controls-position="right" style="width: 100%" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="评分下限">
                          <el-input-number v-model="form.pipeline!.scoreMin" :min="0" :max="99" controls-position="right" style="width: 100%" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="评分上限">
                          <el-input-number v-model="form.pipeline!.scoreMax" :min="1" :max="100" controls-position="right" style="width: 100%" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="润色保留阈值">
                          <el-input-number v-model="form.pipeline!.polishKeepRatio" :min="0.5" :max="1" :step="0.05" :precision="2" controls-position="right" style="width: 100%" />
                          <div class="form-tip">润色版篇幅低于原文该比例时保留审计版（防删减）</div>
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </el-collapse-item>
                  <!-- ③ 配图 -->
                  <el-collapse-item title="③ 配图设置" name="images">
                    <el-row :gutter="16">
                      <el-col :span="12">
                        <el-form-item label="任务轮询超时">
                          <el-input-number v-model="form.pipeline!.imagePollTimeoutMs" :min="10000" :max="300000" :step="5000" controls-position="right" style="width: 100%" />
                          <div class="form-tip">单张配图等待上限（毫秒）</div>
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="连续失败降级">
                          <el-input-number v-model="form.pipeline!.imageRetryAfterFailures" :min="1" :max="5" controls-position="right" style="width: 100%" />
                          <div class="form-tip">连续失败 N 张后剩余位降级为出图描述</div>
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="封面默认尺寸">
                          <el-input v-model="form.pipeline!.coverSizeDefault" placeholder="如 1024*1024" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="小红书封面尺寸">
                          <el-input v-model="form.pipeline!.coverSizeXhs" placeholder="如 1080*1440" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="正文插图尺寸">
                          <el-input v-model="form.pipeline!.nodeSize" placeholder="如 1280*720" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="24">
                        <el-form-item label="降级出图模板">
                          <el-input
                            v-model="form.pipeline!.imageFallbackPrompt"
                            type="textarea"
                            :rows="2"
                            placeholder="{promptZh} 会被替换为中文场景描述"
                          />
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </el-collapse-item>
                  <!-- ④ 文案 -->
                  <el-collapse-item title="④ 形态 / 风格 / 受众文案（注入提示词）" name="copy">
                    <div class="pipeline-group-title">标题字数上限（按形态）</div>
                    <el-row :gutter="16">
                      <el-col v-for="f in TITLE_LIMIT_FIELDS" :key="'tl' + f.key" :span="12">
                        <el-form-item :label="f.label + ' 字数'">
                          <el-input-number v-model="form.pipeline!.titleLimits[f.key as '1']" :min="5" :max="60" controls-position="right" style="width: 100%" />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <div class="pipeline-group-title">正文篇幅窗口（字，超出提示重新生成）</div>
                    <el-row :gutter="16">
                      <el-col v-for="f in TITLE_LIMIT_FIELDS" :key="'lw' + f.key" :span="12">
                        <el-form-item :label="f.label + ' 窗口'">
                          <div class="range-inputs">
                            <el-input-number v-model="form.pipeline!.lengthWindows[f.key as '1'].min" :min="0" :max="20000" controls-position="right" style="width: 46%" />
                            <span class="range-sep">~</span>
                            <el-input-number v-model="form.pipeline!.lengthWindows[f.key as '1'].max" :min="0" :max="30000" controls-position="right" style="width: 46%" />
                          </div>
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <div class="pipeline-group-title">形态写作指令</div>
                    <el-form-item v-for="f in FORM_INSTRUCTION_FIELDS" :key="'fi' + f.key" :label="f.label">
                      <el-input v-model="form.pipeline!.formInstructions[f.key as '1']" type="textarea" :rows="2" />
                    </el-form-item>
                    <div class="pipeline-group-title">风格指令</div>
                    <el-form-item v-for="f in STYLE_INSTRUCTION_FIELDS" :key="'si' + f.key" :label="f.label">
                      <el-input v-model="form.pipeline!.styleInstructions[f.key as 'professional']" type="textarea" :rows="2" />
                    </el-form-item>
                    <div class="pipeline-group-title">受众指令</div>
                    <el-form-item v-for="f in AUDIENCE_INSTRUCTION_FIELDS" :key="'ai' + f.key" :label="f.label">
                      <el-input v-model="form.pipeline!.audienceInstructions[f.key as 'children']" type="textarea" :rows="2" />
                    </el-form-item>
                    <div class="pipeline-group-title">配图规划提示（outline 阶段注入）</div>
                    <el-form-item v-for="f in IMAGE_COUNT_HINT_FIELDS" :key="'ih' + f.key" :label="f.label">
                      <el-input v-model="form.pipeline!.imageCountHints[f.key as '1']" type="textarea" :rows="2" />
                    </el-form-item>
                  </el-collapse-item>
                  <!-- ⑤ 提示词覆盖 -->
                  <el-collapse-item title="⑤ 阶段提示词覆盖（留空使用内置默认提示词）" name="prompts">
                    <el-form-item v-for="f in PROMPT_FIELDS" :key="'pf' + f.key" :label="f.label">
                      <el-input v-model="form.pipeline!.prompts[f.key]" type="textarea" :rows="3" placeholder="留空使用内置默认模板" />
                    </el-form-item>
                  </el-collapse-item>
                  <!-- ⑥ 全局与规则提示词 -->
                  <el-collapse-item title="⑥ 全局与规则提示词（system / 目的 / 平台；留空使用内置默认）" name="rules">
                    <el-form-item label="全局系统提示词">
                      <el-input v-model="form.pipeline!.system" type="textarea" :rows="6" placeholder="所有阶段 LLM 调用的系统层提示（数据忠诚/合规红线/表达铁律）；留空使用内置默认" />
                    </el-form-item>
                    <div class="pipeline-group-title">目的规则（注入各阶段）</div>
                    <el-form-item v-for="p in AIARTIST_PURPOSE_OPTIONS" :key="'pur' + p.value" :label="p.label">
                      <el-input v-model="form.pipeline!.purposeRules[p.value]" type="textarea" :rows="4" placeholder="留空使用内置默认" />
                    </el-form-item>
                    <div class="pipeline-group-title">平台规则（注入正文/润色阶段）</div>
                    <el-form-item v-for="pl in PLATFORM_RULE_FIELDS" :key="'plf' + pl.key" :label="pl.label">
                      <el-input v-model="form.pipeline!.platformRules[pl.key]" type="textarea" :rows="4" placeholder="留空使用内置默认" />
                    </el-form-item>
                  </el-collapse-item>
                </el-collapse>
                <div class="form-tip">各阶段温度、合规禁语、素材上限、篇幅窗口、配图规格等流水线参数，可在分类级独立调整。</div>
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
/* 流水线配置折叠表单 */
.pipeline-collapse {
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;

  :deep(.el-collapse-item__header) {
    font-size: 13px;
    font-weight: 600;
  }
}
.pipeline-group-title {
  font-size: 12px;
  color: #8a919f;
  margin: 8px 0 4px;
  font-weight: 600;
}
.range-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.range-sep {
  color: #8a919f;
  font-size: 12px;
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
