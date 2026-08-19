<script setup lang="ts">
/**
 * Channel 端知识仓库落地页（系统管理 → 知识仓库）。
 *
 * 树形继承视图：
 * - 左侧渠道树：本渠道 + 全部后代渠道，节点显示「渠道简称 + 库状态」（自有/继承自XX/未配置）；
 * - 右侧面板按选中节点渲染：
 *   - 本渠道节点：有独立库 → 完整管理（编辑/同步/删除 + 文档管理 + 问答）；
 *     无独立库 → 空态 + 创建弹窗（有继承库时可对其问答）；
 *   - 后代渠道节点：只读（摘要 + 问答 tab），隐藏文档管理/编辑/同步/删除；
 *   - 无库节点：空态提示。
 *
 * 管理操作仅本渠道（后端租户拦截兜底）；chat/retrieve 后端按
 * 「当前渠道 ∪ 祖先 ∪ 后代」可见性校验，继承库可问答。
 */
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import {
  getKnowledgeRepoTree,
  createKnowledgeRepo,
  updateKnowledgeRepo,
  deleteKnowledgeRepo,
  syncKnowledgeRepo,
  listKnowledgeDocs,
  uploadKnowledgeDoc,
  getKnowledgeDocParseStatus,
  importKnowledgeDocs,
  getKnowledgeImportStatus,
  initKnowledgeRepo,
  getKnowledgeRepoBuildStatus,
  deleteKnowledgeDoc,
  chatKnowledgeRepo,
  retrieveKnowledgeRepo,
  listKnowledgeCategories,
  updateKnowledgeDocTags
} from '@/api/knowledge'
import type {
  KnowledgeRepoTreeNode,
  KnowledgeDoc,
  KnowledgeChatResult,
  KnowledgeIndexConfig,
  KnowledgeCategory
} from '@/types/knowledge'
import {
  knowledgeRepoStatusLabel,
  knowledgeRepoStatusTagType,
  indexStatusLabel,
  indexStatusTagType,
  parseStatusLabel,
  KNOWLEDGE_PARSER_OPTIONS
} from '@/types/knowledge'
import { getChannelInfoCurrent } from '@/api/channel-sub'
import { formatDateTime } from '@/utils/format'
import ChunkDialog from '@/components/ChunkDialog/index.vue'
import KnowledgeCategoryDialog from '@/components/KnowledgeCategoryDialog/index.vue'

// ==================== 树形加载 ====================
const loading = ref(false)
const treeData = ref<KnowledgeRepoTreeNode[]>([])
const selectedNode = ref<KnowledgeRepoTreeNode | null>(null)
/** 当前登录渠道编码（树根节点即本渠道） */
const ownChannelCode = ref('')

async function loadTree() {
  loading.value = true
  try {
    const tree = await getKnowledgeRepoTree()
    treeData.value = tree ?? []
    ownChannelCode.value = tree?.[0]?.channelCode ?? ''
    // 默认选中本渠道节点
    selectedNode.value = tree?.[0] ?? null
    syncRetrieverForm()
  } catch {
    treeData.value = []
    selectedNode.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTree()
  loadCategories()
})

/** 是否本渠道节点（仅本渠道可管理） */
const isOwn = computed(() => !!selectedNode.value && selectedNode.value.channelCode === ownChannelCode.value)
/** 选中节点的实际可用仓库 */
const activeRepo = computed(() => selectedNode.value?.effectiveRepo ?? null)
/** 选中节点独立配置的仓库 */
const ownRepo = computed(() => selectedNode.value?.repo ?? null)

// ==================== 类目管理弹窗 ====================
const categoryDialogVisible = ref(false)

// ==================== 创建仓库弹窗 ====================
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = ref<{
  repoName: string
  description: string
  indexConfig: KnowledgeIndexConfig
}>({ repoName: '', description: '', indexConfig: emptyIndexConfig() })

/** 索引配置默认值（懒建库模式：建库后不可修改） */
function emptyIndexConfig(): KnowledgeIndexConfig {
  return {
    chunkMode: undefined,
    separator: '',
    chunkSize: 500,
    overlapSize: 100,
    embeddingModel: 'text-embedding-v3',
    rerankModel: 'qwen3-rerank',
    rerankMode: 'qa',
    rerankMinScore: 0.01,
    enableRewrite: true,
    denseTopK: 4,
    sparseTopK: 4
  }
}

const createRules: FormRules = {
  repoName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }]
}

async function openCreate() {
  createForm.value = { repoName: '', description: '', indexConfig: emptyIndexConfig() }
  // 默认名：本渠道简称 + 知识库（取简称失败时保持空名称由用户手填）
  try {
    const current = await getChannelInfoCurrent()
    createForm.value.repoName = `${current.shortName || current.fullName}知识库`
  } catch {
    // 渠道信息获取失败时保持空名称
  }
  createVisible.value = true
}

async function handleCreate() {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  createLoading.value = true
  try {
    await createKnowledgeRepo({
      repoName: createForm.value.repoName.trim(),
      description: createForm.value.description.trim() || undefined,
      indexConfig: { ...createForm.value.indexConfig }
    })
    ElMessage.success('创建成功，上传首个文档后将自动在百炼建库')
    createVisible.value = false
    loadTree()
  } finally {
    createLoading.value = false
  }
}

// ==================== 仓库操作（仅本渠道）====================
const saving = ref(false)
const editVisible = ref(false)
const editForm = ref({ repoName: '', description: '' })

function startEdit() {
  if (!ownRepo.value?.id) return
  editForm.value = {
    repoName: ownRepo.value.repoName ?? '',
    description: ownRepo.value.description ?? ''
  }
  editVisible.value = true
}

async function handleSaveEdit() {
  if (!ownRepo.value?.id) return
  saving.value = true
  try {
    await updateKnowledgeRepo(ownRepo.value.id, {
      repoName: editForm.value.repoName.trim(),
      description: editForm.value.description.trim()
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadTree()
  } finally {
    saving.value = false
  }
}

async function handleSync() {
  if (!ownRepo.value?.id) return
  try {
    await syncKnowledgeRepo(ownRepo.value.id)
    ElMessage.success('同步成功')
  } catch {
    // 同步失败会置状态为异常，刷新列表展示
  }
  loadTree()
}

async function handleDelete() {
  if (!ownRepo.value?.id) return
  await ElMessageBox.confirm(
    '删除仓库将同时删除百炼云端的知识库索引及全部文档，且不可恢复。确定删除？',
    '危险操作',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deleteKnowledgeRepo(ownRepo.value.id)
  ElMessage.success('删除成功')
  loadTree()
}

// ==================== 检索参数（已建库可编辑，对齐 admin 详情页）====================
/** 检索参数行内编辑表单（已建库可改，默认与新建弹窗一致） */
const retrieverForm = ref<KnowledgeIndexConfig>({
  denseTopK: 4,
  sparseTopK: 4,
  rerankMinScore: 0.01
})
const retrieverSaving = ref(false)

/** 检索参数表单同步：本渠道独立库已建库时从 indexConfig 预填（默认与新建弹窗一致） */
function syncRetrieverForm() {
  const cfg = ownRepo.value?.indexConfig
  retrieverForm.value = {
    denseTopK: cfg?.denseTopK ?? 4,
    sparseTopK: cfg?.sparseTopK ?? 4,
    rerankMinScore: cfg?.rerankMinScore ?? 0.01
  }
}

/** 保存检索参数：提交完整 indexConfig（原值 + 修改的三个字段），已建库后端仅允许这三项变更 */
async function handleSaveRetriever() {
  const repoId = ownRepo.value?.id
  if (!repoId) return
  retrieverSaving.value = true
  try {
    await updateKnowledgeRepo(repoId, {
      indexConfig: {
        ...(ownRepo.value?.indexConfig ?? {}),
        denseTopK: retrieverForm.value.denseTopK,
        sparseTopK: retrieverForm.value.sparseTopK,
        rerankMinScore: retrieverForm.value.rerankMinScore
      }
    })
    ElMessage.success('检索参数已保存')
    loadTree()
  } finally {
    retrieverSaving.value = false
  }
}

// ==================== 文档管理（仅本渠道独立库）====================
const docs = ref<KnowledgeDoc[]>([])
const docsLoading = ref(false)
/** 仓库是否已在百炼建库（懒建库模式下首个文档解析后自动建库） */
const indexed = computed(() => !!ownRepo.value?.indexId)

/** 上传处理中任务（解析/建库/导入轮询） */
interface UploadTask {
  fileId: string
  fileName: string
  status: 'parsing' | 'creating' | 'importing' | 'done' | 'failed'
  message?: string
}

const tasks = ref<UploadTask[]>([])
const timers: ReturnType<typeof setTimeout>[] = []

onBeforeUnmount(() => timers.forEach(clearTimeout))

async function loadDocs() {
  if (!ownRepo.value?.id) return
  docsLoading.value = true
  try {
    docs.value = await listKnowledgeDocs(ownRepo.value.id, { pageNumber: 1, pageSize: 100 })
  } catch {
    docs.value = []
  } finally {
    docsLoading.value = false
  }
}

/** 切换选中节点后：本渠道且建库则加载文档列表 */
function onSelectNode(node: KnowledgeRepoTreeNode) {
  selectedNode.value = node
  resetChat()
  syncRetrieverForm()
  if (isOwn.value && ownRepo.value?.id) {
    loadDocs()
  } else {
    docs.value = []
    tasks.value = []
  }
}

// ---------- 上传设置（选文件 → 设置对话框 → 确认后逐个上传） ----------
/** 待上传文件（设置对话框确认后逐个上传） */
const pendingFiles = ref<File[]>([])
const uploadDialogVisible = ref(false)
const uploadSetting = reactive({
  categoryId: '' as string, // 空 = 默认类目 default
  parser: 'DASHSCOPE_DOCMIND',
  tags: [] as string[]
})
const categories = ref<KnowledgeCategory[]>([])
/** 类目名映射（展示用） */
const categoryNameMap = ref(new Map<string, string>())

interface CategoryTreeNode extends KnowledgeCategory {
  children: CategoryTreeNode[]
}

/** 平铺 → 树（parentCategoryId 挂接；顶层含百炼 default 类目） */
function buildCategoryTree(flat: KnowledgeCategory[]): CategoryTreeNode[] {
  const map = new Map<string, CategoryTreeNode>()
  flat.forEach((c) => map.set(c.categoryId, { ...c, children: [] }))
  const roots: CategoryTreeNode[] = []
  map.forEach((node) => {
    if (node.parentCategoryId && map.has(node.parentCategoryId)) {
      map.get(node.parentCategoryId)!.children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

async function loadCategories() {
  try {
    const list = await listKnowledgeCategories()
    categories.value = list
    categoryNameMap.value = new Map(list.map((c) => [c.categoryId, c.categoryName]))
  } catch {
    categories.value = []
  }
}

/** 拖入/选择文件 → 打开上传设置 */
function handleSelectFile(options: UploadRequestOptions) {
  pendingFiles.value.push(options.file)
  uploadDialogVisible.value = true
  return Promise.resolve() // 阻止 el-upload 直接上传，由确认后统一走 handleUpload
}

/** 取消上传设置：关闭对话框并清空待传文件，避免下次打开混入旧文件 */
function closeUploadDialog() {
  uploadDialogVisible.value = false
  pendingFiles.value = []
}

/** 确认上传：按设置逐个上传 */
async function confirmUpload() {
  const repoId = ownRepo.value?.id
  if (!repoId) return
  // 先快照并清空，再关闭对话框（@close 也会清空 pendingFiles，二者互不干扰）
  const files = pendingFiles.value
  pendingFiles.value = []
  uploadDialogVisible.value = false
  for (const file of files) {
    await handleUpload(repoId, file, {
      categoryId: uploadSetting.categoryId || undefined,
      parser: uploadSetting.parser,
      tags: uploadSetting.tags
    })
  }
}

async function handleUpload(repoId: number, file: File, opts: { categoryId?: string; parser: string; tags: string[] }) {
  const fileName = file.name
  let fileId: string
  try {
    fileId = await uploadKnowledgeDoc(repoId, file, opts, true)
  } catch (e) {
    const msg = e instanceof Error && e.message ? e.message : '未知原因'
    ElMessage.error(`「${fileName}」上传失败：${msg}`)
    return
  }
  const task: UploadTask = { fileId, fileName, status: 'parsing' }
  tasks.value.push(task)
  ElMessage.success(`「${fileName}」上传成功，正在解析`)
  pollParse(task)
}

/** 轮询解析状态 → PARSE_SUCCESS 后自动建库（懒建库）或导入索引 */
function pollParse(task: UploadTask) {
  const repoId = ownRepo.value?.id
  if (!repoId) return
  timers.push(
    setTimeout(async () => {
      try {
        const info = await getKnowledgeDocParseStatus(repoId, task.fileId)
        if (info.parseStatus === 'PARSE_SUCCESS') {
          if (indexed.value) {
            task.status = 'importing'
            pollImport(task)
          } else {
            // 首个文档：自动在百炼建库（CreateIndex + SubmitIndexJob）
            task.status = 'creating'
            pollInitIndex(task)
          }
        } else if (info.parseStatus === 'PARSE_FAILED') {
          task.status = 'failed'
          task.message = '解析失败（文件格式不支持或损坏）'
        } else {
          pollParse(task)
        }
      } catch {
        task.status = 'failed'
        task.message = '查询解析状态失败'
      }
    }, 5000)
  )
}

/** 懒建库：首个文档解析成功后创建百炼知识库并轮询构建状态 */
function pollInitIndex(task: UploadTask) {
  const repoId = ownRepo.value?.id
  if (!repoId) return
  timers.push(
    setTimeout(async () => {
      try {
        const jobId = await initKnowledgeRepo(repoId, [task.fileId])
        pollBuildJob(task, jobId)
      } catch {
        task.status = 'failed'
        task.message = '初始化建库失败'
      }
    }, 0)
  )
}

function pollBuildJob(task: UploadTask, jobId: string) {
  const repoId = ownRepo.value?.id
  if (!repoId) return
  timers.push(
    setTimeout(async () => {
      try {
        const status = await getKnowledgeRepoBuildStatus(repoId)
        if (status === 'FINISH' || status === 'COMPLETED') {
          task.status = 'done'
          ElMessage.success(`「${task.fileName}」已入库，知识库创建完成`)
          loadTree()
          loadDocs()
        } else if (status === 'FAILED') {
          task.status = 'failed'
          task.message = '索引构建失败（可在百炼控制台查看原因）'
        } else {
          pollBuildJob(task, jobId)
        }
      } catch {
        task.status = 'failed'
        task.message = '查询建库状态失败'
      }
    }, 5000)
  )
}

/** 轮询导入索引状态 → FINISH 后刷新列表 */
function pollImport(task: UploadTask) {
  const repoId = ownRepo.value?.id
  if (!repoId) return
  timers.push(
    setTimeout(async () => {
      try {
        const jobId = await importKnowledgeDocs(repoId, [task.fileId])
        pollImportJob(task, jobId)
      } catch {
        task.status = 'failed'
        task.message = '导入索引失败'
      }
    }, 0)
  )
}

function pollImportJob(task: UploadTask, jobId: string) {
  const repoId = ownRepo.value?.id
  if (!repoId) return
  timers.push(
    setTimeout(async () => {
      try {
        const status = await getKnowledgeImportStatus(repoId, jobId)
        if (status === 'FINISH' || status === 'COMPLETED') {
          task.status = 'done'
          ElMessage.success(`「${task.fileName}」已入库`)
          loadDocs()
        } else if (status === 'FAILED') {
          task.status = 'failed'
          task.message = '入库失败（可在百炼控制台查看原因）'
        } else {
          pollImportJob(task, jobId)
        }
      } catch {
        task.status = 'failed'
        task.message = '查询入库状态失败'
      }
    }, 5000)
  )
}

async function handleDeleteDoc(row: KnowledgeDoc) {
  if (!ownRepo.value?.id) return
  await ElMessageBox.confirm(`确定从知识库永久删除「${row.fileName}」？删除后不可恢复。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteKnowledgeDoc(ownRepo.value.id, row.fileId)
  ElMessage.success('删除成功')
  loadDocs()
}

// ---------- 切片管理 ----------
const chunkDialogRef = ref<InstanceType<typeof ChunkDialog>>()

function openChunks(row: KnowledgeDoc) {
  chunkDialogRef.value?.open(row.fileId, row.fileName)
}

// ---------- 文件详情 / 编辑标签 ----------
const detailVisible = ref(false)
const detailRow = ref<KnowledgeDoc | null>(null)
const editTagsVisible = ref(false)
const editingFile = ref<KnowledgeDoc | null>(null)
const editTags = ref<string[]>([])

function openDetail(row: KnowledgeDoc) {
  detailRow.value = row
  detailVisible.value = true
}

function openEditTags(row: KnowledgeDoc) {
  editingFile.value = row
  editTags.value = [...(row.tags || [])]
  editTagsVisible.value = true
}

async function confirmEditTags() {
  const repoId = ownRepo.value?.id
  if (!editingFile.value || !repoId) return
  const tags = editTags.value.slice(0, 10)
  try {
    await updateKnowledgeDocTags(repoId, editingFile.value.fileId, tags)
    editingFile.value.tags = tags
    editTagsVisible.value = false
    ElMessage.success('标签已更新')
  } catch {
    // 失败时保持对话框打开、不回写，便于用户重试
    ElMessage.error('标签更新失败，请重试')
  }
}

/** 解析器选项名（unknown 原样展示） */
function parserLabel(v?: string): string {
  return KNOWLEDGE_PARSER_OPTIONS.find((o) => o.value === v)?.label || v || '--'
}

function formatSize(bytes?: number): string {
  if (!bytes) return '--'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

function formatTime(ms?: number): string {
  if (!ms) return '--'
  const d = new Date(ms)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function taskStatusText(t: UploadTask): string {
  if (t.status === 'parsing') return '解析中…'
  if (t.status === 'creating') return '云端建库中…'
  if (t.status === 'importing') return '入库中…'
  if (t.status === 'done') return '已完成'
  return t.message || '失败'
}

// ==================== 问答 / 检索（本渠道 + 继承 + 后代可见库）====================
const question = ref('')
const asking = ref(false)
const chatResult = ref<KnowledgeChatResult | null>(null)

const retrieveQuery = ref('')
const retrieving = ref(false)
const hits = ref<KnowledgeChatResult['citations']>([])

function resetChat() {
  question.value = ''
  chatResult.value = null
  retrieveQuery.value = ''
  hits.value = []
}

async function handleAsk() {
  if (!activeRepo.value?.id || !question.value.trim()) return
  asking.value = true
  try {
    chatResult.value = await chatKnowledgeRepo(activeRepo.value.id, { question: question.value.trim(), topK: 4 })
  } catch {
    chatResult.value = null
  } finally {
    asking.value = false
  }
}

async function handleRetrieve() {
  if (!activeRepo.value?.id || !retrieveQuery.value.trim()) return
  retrieving.value = true
  try {
    hits.value = await retrieveKnowledgeRepo(activeRepo.value.id, { query: retrieveQuery.value.trim(), topK: 5 })
    if (!hits.value.length) ElMessage.info('未检索到相关内容')
  } catch {
    hits.value = []
  } finally {
    retrieving.value = false
  }
}

/** 树节点状态标签：自有/继承自XX/未配置 */
function nodeStatusText(node: KnowledgeRepoTreeNode): string {
  if (node.repo) return '自有'
  if (node.inheritedFrom) return `继承自${node.inheritedFromName || node.inheritedFrom}`
  return '未配置'
}

function nodeStatusTagType(node: KnowledgeRepoTreeNode): 'success' | 'warning' | 'info' {
  if (node.repo) return 'success'
  if (node.inheritedFrom) return 'warning'
  return 'info'
}

/** 节点 label（渠道简称 + 状态） */
function nodeLabel(node: KnowledgeRepoTreeNode): string {
  return node.shortName || node.fullName || node.channelCode
}
</script>

<template>
  <div v-loading="loading" class="knowledge-page">
    <el-card shadow="never" class="tree-card">
      <div class="page-head">
        <span class="card-title">渠道知识库</span>
        <span class="page-tip">本渠道可管理自己的知识库；子渠道继承的知识库只可使用，不可管理</span>
        <div class="tree-toolbar">
          <el-button
            size="small"
            :icon="'FolderOpened'"
            v-permission="'channel:knowledge:view'"
            @click="categoryDialogVisible = true"
          >
            类目管理
          </el-button>
        </div>
      </div>
      <el-tree
        :data="treeData"
        node-key="channelCode"
        :props="{ label: 'fullName', children: 'children' }"
        default-expand-all
        highlight-current
        :current-node-key="ownChannelCode"
        @node-click="onSelectNode"
      >
        <template #default="{ data }">
          <span class="tree-node">
            <span class="tree-label">{{ nodeLabel(data) }}</span>
            <el-tag size="small" :type="nodeStatusTagType(data)" class="tree-tag">
              {{ nodeStatusText(data) }}
            </el-tag>
          </span>
        </template>
      </el-tree>
    </el-card>

    <div class="detail-area">
      <!-- 未选中 -->
      <el-card v-if="!selectedNode && !loading" shadow="never" class="empty-card">
        <el-empty description="请在左侧选择渠道" :image-size="80" />
      </el-card>

      <!-- 选中：无可用库 -->
      <el-card v-else-if="selectedNode && !activeRepo" shadow="never" class="empty-card">
        <el-empty :description="`${nodeLabel(selectedNode)} 暂无可用知识库`" :image-size="80">
          <template v-if="isOwn">
            <div class="empty-tip">
              创建后即可上传产品手册、服务说明等资料；上传首个文档并解析成功后，将自动在百炼云端建库，随后可进行 AI 问答。
            </div>
            <el-button type="primary" v-permission="'channel:knowledge:create'" @click="openCreate">
              创建知识仓库
            </el-button>
          </template>
        </el-empty>
      </el-card>

      <!-- 选中：有可用库 -->
      <template v-else-if="selectedNode && activeRepo">
        <!-- 摘要区 -->
        <el-card shadow="never" class="summary-card">
          <div class="summary-header">
            <div class="summary-title">
              <span class="repo-name">{{ activeRepo.repoName }}</span>
              <el-tag size="small" :type="knowledgeRepoStatusTagType(activeRepo.status)">
                {{ knowledgeRepoStatusLabel(activeRepo.status) }}
              </el-tag>
              <el-tag v-if="selectedNode.inheritedFrom" size="small" type="warning" class="inherit-tag">
                继承自{{ selectedNode.inheritedFromName || selectedNode.inheritedFrom }}
              </el-tag>
              <span class="repo-code">{{ activeRepo.repoCode }}</span>
            </div>
            <div class="summary-actions">
              <template v-if="isOwn">
                <el-button size="small" v-permission="'channel:knowledge:update'" @click="startEdit">编辑</el-button>
                <el-button size="small" type="success" v-permission="'channel:knowledge:sync'" @click="handleSync">
                  同步
                </el-button>
                <el-button size="small" type="danger" v-permission="'channel:knowledge:delete'" @click="handleDelete">
                  删除
                </el-button>
              </template>
              <el-tag v-else size="small" type="info">只读（子渠道知识库）</el-tag>
            </div>
          </div>
          <div class="summary-meta">
            <span>文档数：{{ activeRepo.docCount ?? 0 }}</span>
            <span v-if="activeRepo.indexId" :title="activeRepo.indexId">索引：{{ activeRepo.indexId }}</span>
            <span v-else>索引：未建库</span>
            <span>最近同步：{{ activeRepo.lastSyncAt ? formatDateTime(activeRepo.lastSyncAt) : '--' }}</span>
            <span>创建时间：{{ activeRepo.createdAt ? formatDateTime(activeRepo.createdAt) : '--' }}</span>
          </div>

          <!-- 检索参数（已建库可编辑，对齐 admin 详情页；保存提交完整 indexConfig） -->
          <div v-if="isOwn && ownRepo?.indexId" class="retriever-block">
            <div class="retriever-title">检索参数（保存后同步百炼）</div>
            <el-form inline label-width="150px">
              <el-form-item label="稠密召回 TopK（dense）">
                <el-input-number v-model="retrieverForm.denseTopK" :min="1" :max="100" controls-position="right" />
              </el-form-item>
              <el-form-item label="稀疏召回 TopK（sparse）">
                <el-input-number v-model="retrieverForm.sparseTopK" :min="1" :max="100" controls-position="right" />
              </el-form-item>
              <el-form-item label="相似度阈值">
                <el-input-number
                  v-model="retrieverForm.rerankMinScore"
                  :min="0.01"
                  :max="1"
                  :step="0.01"
                  controls-position="right"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="retrieverSaving" @click="handleSaveRetriever">
                  保存检索参数
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-card>

        <!-- tabs：本渠道且独立库 → 文档管理 + 问答；否则仅问答（只读使用） -->
        <el-card shadow="never" class="content-card">
          <el-tabs v-if="isOwn && ownRepo?.id" :model-value="'docs'" @tab-change="(n: string) => n === 'docs' && loadDocs()">
            <!-- 文档管理 -->
            <el-tab-pane label="文档管理" name="docs">
              <div class="doc-upload">
                <el-upload
                  :show-file-list="false"
                  :http-request="handleSelectFile"
                  :multiple="true"
                  :accept="'.pdf,.doc,.docx,.md,.txt,.xls,.xlsx,.ppt,.pptx'"
                  v-permission="'channel:knowledge:doc:upload'"
                  drag
                >
                  <div class="upload-inner">
                    <el-icon size="36" color="#909399"><UploadFilled /></el-icon>
                    <div class="upload-text">拖拽文件到此处，或<em>点击上传</em></div>
                    <div class="upload-tip">支持 PDF / Word / Markdown / TXT / Excel / PPT，单文件 ≤ 100MB；上传后自动解析并入索引</div>
                  </div>
                </el-upload>
              </div>

              <!-- 上传设置（选文件后先确认类目/解析器/标签再上传） -->
              <el-dialog v-model="uploadDialogVisible" title="上传设置" width="480px" :close-on-click-modal="false" @close="closeUploadDialog">
                <el-form label-width="80px">
                  <el-form-item label="文件">
                    <span class="upload-file-list">
                      <el-tag v-for="(f, i) in pendingFiles" :key="i" size="small" closable @close="pendingFiles.splice(i, 1)">
                        {{ f.name }}
                      </el-tag>
                    </span>
                  </el-form-item>
                  <el-form-item label="所属类目">
                    <el-tree-select
                      v-model="uploadSetting.categoryId"
                      :data="buildCategoryTree(categories)"
                      node-key="categoryId"
                      :props="{ label: 'categoryName', children: 'children' }"
                      check-strictly
                      clearable
                      placeholder="默认类目"
                      style="width: 100%"
                    />
                  </el-form-item>
                  <el-form-item label="解析器">
                    <el-select v-model="uploadSetting.parser" style="width: 100%">
                      <el-option v-for="o in KNOWLEDGE_PARSER_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="标签">
                    <el-select
                      v-model="uploadSetting.tags"
                      multiple
                      filterable
                      allow-create
                      default-first-option
                      placeholder="输入后回车创建，最多 10 个"
                      style="width: 100%"
                    >
                      <el-option v-for="t in uploadSetting.tags" :key="t" :label="t" :value="t" />
                    </el-select>
                  </el-form-item>
                </el-form>
                <template #footer>
                  <el-button @click="closeUploadDialog">取消</el-button>
                  <el-button type="primary" :disabled="!pendingFiles.length" @click="confirmUpload">
                    上传 {{ pendingFiles.length ? `（${pendingFiles.length} 个文件）` : '' }}
                  </el-button>
                </template>
              </el-dialog>

              <!-- 处理中任务 -->
              <el-card v-if="tasks.length" shadow="never" class="task-card">
                <template #header>处理中</template>
                <div v-for="t in tasks" :key="t.fileId" class="task-item">
                  <span class="task-name">{{ t.fileName }}</span>
                  <el-tag size="small" :type="t.status === 'failed' ? 'danger' : t.status === 'done' ? 'success' : 'warning'">
                    {{ taskStatusText(t) }}
                  </el-tag>
                </div>
              </el-card>

              <el-table v-loading="docsLoading" :data="docs" border stripe row-key="fileId" class="doc-table">
                <el-table-column prop="fileName" label="文件名" min-width="260" show-overflow-tooltip />
                <el-table-column label="大小" width="110" align="center">
                  <template #default="{ row }">{{ formatSize(row.sizeInBytes) }}</template>
                </el-table-column>
                <el-table-column label="入库状态" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="indexStatusTagType(row.indexStatus)">
                      {{ indexStatusLabel(row.indexStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="标签" min-width="140">
                  <template #default="{ row }">
                    <template v-if="row.tags?.length">
                      <el-tag v-for="t in row.tags" :key="t" size="small" style="margin-right: 4px">{{ t }}</el-tag>
                    </template>
                    <span v-else class="no-tags">--</span>
                  </template>
                </el-table-column>
                <el-table-column label="更新时间" width="170" align="center">
                  <template #default="{ row }">{{ formatTime(row.gmtModified) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="220" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="info" size="small" @click="openDetail(row)">详情</el-button>
                    <el-button link type="warning" size="small" @click="openEditTags(row)">标签</el-button>
                    <el-button link type="primary" size="small" @click="openChunks(row)">切片</el-button>
                    <el-button link type="danger" size="small" v-permission="'channel:knowledge:doc:delete'" @click="handleDeleteDoc(row)">
                      删除
                    </el-button>
                  </template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无文档，上传后自动解析入库" :image-size="80" />
                </template>
              </el-table>

              <!-- 文件详情 -->
              <el-dialog v-model="detailVisible" title="文件详情" width="480px">
                <el-descriptions v-if="detailRow" :column="1" border>
                  <el-descriptions-item label="文件名">{{ detailRow.fileName }}</el-descriptions-item>
                  <el-descriptions-item label="所属类目">
                    {{ detailRow.categoryId ? categoryNameMap.get(detailRow.categoryId) || detailRow.categoryId : '默认类目' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="解析器">{{ parserLabel(detailRow.parser) }}</el-descriptions-item>
                  <el-descriptions-item label="解析状态">{{ parseStatusLabel(detailRow.parseStatus) }}</el-descriptions-item>
                  <el-descriptions-item label="文件大小">{{ formatSize(detailRow.sizeInBytes) }}</el-descriptions-item>
                  <el-descriptions-item label="文件 ID">{{ detailRow.fileId }}</el-descriptions-item>
                </el-descriptions>
                <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
              </el-dialog>

              <!-- 编辑标签 -->
              <el-dialog v-model="editTagsVisible" title="编辑标签" width="440px">
                <el-select v-model="editTags" multiple filterable allow-create default-first-option style="width: 100%" placeholder="输入后回车创建，最多 10 个">
                  <el-option v-for="t in editTags" :key="t" :label="t" :value="t" />
                </el-select>
                <template #footer>
                  <el-button @click="editTagsVisible = false">取消</el-button>
                  <el-button type="primary" @click="confirmEditTags">保存</el-button>
                </template>
              </el-dialog>
            </el-tab-pane>

            <!-- 问答测试 -->
            <el-tab-pane label="问答测试" name="chat">
              <el-card shadow="never">
                <template #header>知识库问答（RAG）</template>
                <div class="ask-row">
                  <el-input
                    v-model="question"
                    placeholder="输入问题，如：大雁养老的终身养老权益包含哪些服务？"
                    clearable
                    @keyup.enter="handleAsk"
                  />
                  <el-button type="primary" v-permission="'channel:knowledge:chat'" :loading="asking" @click="handleAsk">
                    提问
                  </el-button>
                </div>
                <div v-if="chatResult" class="answer-box">
                  <div class="answer-text">{{ chatResult.answer }}</div>
                  <el-collapse v-if="chatResult.citations.length" class="cite-collapse">
                    <el-collapse-item :title="`引用片段（${chatResult.citations.length} 条）`" name="cites">
                      <div v-for="(c, i) in chatResult.citations" :key="i" class="cite-item">
                        <div class="cite-head">
                          <span class="cite-index">[{{ i + 1 }}]</span>
                          <span v-if="c.score" class="cite-score">相关度 {{ (c.score * 100).toFixed(1) }}%</span>
                        </div>
                        <div class="cite-text">{{ c.text }}</div>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </el-card>

              <el-card shadow="never" style="margin-top: 16px">
                <template #header>仅检索（查看召回片段，不调模型）</template>
                <div class="ask-row">
                  <el-input v-model="retrieveQuery" placeholder="输入检索词" clearable @keyup.enter="handleRetrieve" />
                  <el-button :loading="retrieving" @click="handleRetrieve">检索</el-button>
                </div>
                <div v-if="hits.length" class="hits-box">
                  <div v-for="(h, i) in hits" :key="i" class="hit-item">
                    <div class="hit-head">
                      <span class="hit-index">命中 {{ i + 1 }}</span>
                      <span v-if="h.score" class="hit-score">相关度 {{ (h.score * 100).toFixed(1) }}%</span>
                    </div>
                    <div class="hit-text">{{ h.text }}</div>
                  </div>
                </div>
              </el-card>
            </el-tab-pane>
          </el-tabs>

          <!-- 非本渠道 / 继承库：仅问答（只读使用） -->
          <div v-else class="readonly-chat">
            <el-card shadow="never">
              <template #header>
                <span>知识库问答（RAG）</span>
                <el-tag v-if="selectedNode.inheritedFrom" size="small" type="warning" class="inherit-tag">
                  继承自{{ selectedNode.inheritedFromName || selectedNode.inheritedFrom }}
                </el-tag>
                <el-tag v-else size="small" type="info">只读使用</el-tag>
              </template>
              <div class="ask-row">
                <el-input
                  v-model="question"
                  placeholder="输入问题，如：大雁养老的终身养老权益包含哪些服务？"
                  clearable
                  @keyup.enter="handleAsk"
                />
                <el-button type="primary" v-permission="'channel:knowledge:chat'" :loading="asking" @click="handleAsk">
                  提问
                </el-button>
              </div>
              <div v-if="chatResult" class="answer-box">
                <div class="answer-text">{{ chatResult.answer }}</div>
                <el-collapse v-if="chatResult.citations.length" class="cite-collapse">
                  <el-collapse-item :title="`引用片段（${chatResult.citations.length} 条）`" name="cites">
                    <div v-for="(c, i) in chatResult.citations" :key="i" class="cite-item">
                      <div class="cite-head">
                        <span class="cite-index">[{{ i + 1 }}]</span>
                        <span v-if="c.score" class="cite-score">相关度 {{ (c.score * 100).toFixed(1) }}%</span>
                      </div>
                      <div class="cite-text">{{ c.text }}</div>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </el-card>

            <el-card shadow="never" style="margin-top: 16px">
              <template #header>仅检索（查看召回片段，不调模型）</template>
              <div class="ask-row">
                <el-input v-model="retrieveQuery" placeholder="输入检索词" clearable @keyup.enter="handleRetrieve" />
                <el-button :loading="retrieving" @click="handleRetrieve">检索</el-button>
              </div>
              <div v-if="hits.length" class="hits-box">
                <div v-for="(h, i) in hits" :key="i" class="hit-item">
                  <div class="hit-head">
                    <span class="hit-index">命中 {{ i + 1 }}</span>
                    <span v-if="h.score" class="hit-score">相关度 {{ (h.score * 100).toFixed(1) }}%</span>
                  </div>
                  <div class="hit-text">{{ h.text }}</div>
                </div>
              </div>
            </el-card>
          </div>
        </el-card>
      </template>
    </div>

    <!-- 切片管理弹窗 -->
    <ChunkDialog ref="chunkDialogRef" :repo-id="ownRepo?.id ?? 0" />

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑知识仓库" width="520px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="仓库名称">
          <el-input v-model="editForm.repoName" maxlength="100" />
        </el-form-item>
        <el-form-item label="仓库描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" maxlength="255" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 创建弹窗 -->
    <el-dialog v-model="createVisible" title="创建知识仓库" width="680px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
        <el-form-item label="仓库名称" prop="repoName">
          <el-input v-model="createForm.repoName" placeholder="如：本渠道知识库" maxlength="100" />
        </el-form-item>
        <el-form-item label="切分方式">
          <el-radio-group v-model="createForm.indexConfig.chunkMode">
            <el-radio :value="undefined">智能切分</el-radio>
            <el-radio value="regex">自定义切分</el-radio>
          </el-radio-group>
          <div class="form-tip">智能切分按语义自动切块；自定义按分隔符 + 切块长度切块（建库后不可修改）</div>
        </el-form-item>
        <template v-if="createForm.indexConfig.chunkMode === 'regex'">
          <el-form-item label="分隔符">
            <el-input v-model="createForm.indexConfig.separator" placeholder="正则表达式，如 (?<=。)" />
          </el-form-item>
          <el-form-item label="切块长度">
            <el-input-number v-model="createForm.indexConfig.chunkSize" :min="1" :max="6000" controls-position="right" />
          </el-form-item>
        </template>
        <el-form-item label="向量模型">
          <el-select v-model="createForm.indexConfig.embeddingModel" style="width: 220px">
            <el-option label="text-embedding-v3" value="text-embedding-v3" />
            <el-option label="text-embedding-v4" value="text-embedding-v4" />
          </el-select>
        </el-form-item>
        <el-form-item label="重排模型">
          <el-select v-model="createForm.indexConfig.rerankModel" style="width: 220px">
            <el-option label="qwen3-rerank（语义重排）" value="qwen3-rerank" />
            <el-option label="qwen3-rerank-hybrid（语义+文本匹配）" value="qwen3-rerank-hybrid" />
          </el-select>
        </el-form-item>
        <el-form-item label="重排模式">
          <el-select v-model="createForm.indexConfig.rerankMode" style="width: 220px">
            <el-option label="问答模式（qa）" value="qa" />
            <el-option label="相似模式（similar）" value="similar" />
            <el-option label="自定义（custom）" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="相似度阈值">
          <el-input-number v-model="createForm.indexConfig.rerankMinScore" :min="0.01" :max="1" :step="0.01" controls-position="right" />
        </el-form-item>
        <el-form-item label="多轮改写">
          <el-switch v-model="createForm.indexConfig.enableRewrite" />
          <span class="form-tip" style="margin-left: 8px">多轮对话时对问题做改写后检索</span>
        </el-form-item>
        <el-form-item label="仓库描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" maxlength="255" placeholder="选填" />
        </el-form-item>
        <div class="form-tip standalone">创建成功后上传首个文档，解析完成将自动在百炼云端建库。</div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 类目管理弹窗 -->
    <KnowledgeCategoryDialog v-model="categoryDialogVisible" />
  </div>
</template>

<style scoped lang="scss">
.knowledge-page {
  display: flex;
  gap: 16px;
  align-items: flex-start;

  .tree-card {
    width: 320px;
    flex-shrink: 0;

    .page-head {
      display: flex;
      flex-direction: column;
      gap: 4px;
      margin-bottom: 12px;

      .card-title {
        font-size: 15px;
        font-weight: 600;
        color: #1f2329;
      }
      .page-tip {
        font-size: 12px;
        color: #909399;
        line-height: 1.5;
      }
      .tree-toolbar {
        margin-top: 8px;
      }
    }

    .tree-node {
      display: flex;
      align-items: center;
      gap: 8px;
      flex: 1;

      .tree-label {
        font-size: 13px;
      }
    }
  }

  .detail-area {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;

    .empty-card {
      .empty-tip {
        text-align: center;
        color: #909399;
        font-size: 13px;
        margin-top: -8px;
        padding-bottom: 16px;
      }
    }

    .summary-card {
      .summary-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 8px;

        .summary-title {
          display: flex;
          align-items: center;
          gap: 8px;

          .repo-name {
            font-size: 16px;
            font-weight: 600;
            color: #1f2329;
          }
          .repo-code {
            font-size: 12px;
            color: #909399;
          }
          .inherit-tag {
            margin-left: 2px;
          }
        }
      }

      .summary-meta {
        display: flex;
        gap: 24px;
        flex-wrap: wrap;
        margin-top: 12px;
        color: #606266;
        font-size: 13px;
      }

      .retriever-block {
        margin-top: 16px;
        padding-top: 14px;
        border-top: 1px dashed #e4e7ed;

        .retriever-title {
          margin-bottom: 12px;
          font-size: 13px;
          font-weight: 600;
          color: #303133;
        }
      }
    }

    .doc-upload {
      margin-bottom: 16px;
      :deep(.el-upload-dragger) {
        padding: 18px 0;
      }
      .upload-inner {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 6px;
      }
      .upload-text {
        color: #606266;
        font-size: 14px;
        em {
          color: #409eff;
          font-style: normal;
        }
      }
      .upload-tip {
        color: #909399;
        font-size: 12px;
      }
    }

    .task-card {
      margin-bottom: 16px;
      .task-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 6px 0;
        border-bottom: 1px dashed #ebeef5;
        &:last-child {
          border-bottom: none;
        }
        .task-name {
          font-size: 13px;
          color: #303133;
        }
      }
    }

    .upload-file-list {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      max-height: 120px;
      overflow-y: auto;
    }
    .no-tags {
      color: #c0c4cc;
    }

    .ask-row {
      display: flex;
      gap: 12px;
    }
    .answer-box {
      margin-top: 16px;
      padding: 16px;
      background: #f5f7fa;
      border-radius: 6px;
      .answer-text {
        font-size: 14px;
        line-height: 1.8;
        color: #303133;
        white-space: pre-wrap;
      }
      .cite-collapse {
        margin-top: 12px;
        .cite-item {
          padding: 8px 0;
          border-bottom: 1px dashed #e4e7ed;
          &:last-child {
            border-bottom: none;
          }
          .cite-head {
            display: flex;
            gap: 8px;
            align-items: center;
            margin-bottom: 4px;
            .cite-index {
              font-size: 12px;
              color: #409eff;
              font-weight: 600;
            }
            .cite-score {
              font-size: 12px;
              color: #909399;
            }
          }
          .cite-text {
            font-size: 13px;
            color: #606266;
            line-height: 1.7;
            display: -webkit-box;
            -webkit-line-clamp: 4;
            -webkit-box-orient: vertical;
            overflow: hidden;
          }
        }
      }
    }
    .hits-box {
      margin-top: 16px;
      .hit-item {
        padding: 10px 0;
        border-bottom: 1px dashed #e4e7ed;
        &:last-child {
          border-bottom: none;
        }
        .hit-head {
          display: flex;
          gap: 8px;
          align-items: center;
          margin-bottom: 4px;
          .hit-index {
            font-size: 12px;
            color: #67c23a;
            font-weight: 600;
          }
          .hit-score {
            font-size: 12px;
            color: #909399;
          }
        }
        .hit-text {
          font-size: 13px;
          color: #606266;
          line-height: 1.7;
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
      }
    }

    .form-tip {
      font-size: 12px;
      color: #909399;
      line-height: 1.5;
      width: 100%;
      &.standalone {
        padding-left: 110px;
      }
    }
  }
}
</style>
