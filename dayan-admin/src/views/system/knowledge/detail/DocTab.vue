<script setup lang="ts">
/**
 * 知识仓库详情 - 文档管理 Tab。
 *
 * 文档全链路（全部实时代理百炼云端）：
 * 选文件 → 上传设置（类目/解析器/标签）→ 上传 → 解析（DescribeFile 轮询）→
 * 导入索引（SubmitIndexAddDocumentsJob）→ 入库轮询（GetIndexJobStatus）→ 刷新列表。
 * 任一环节失败展示原因。
 */
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import {
  getKnowledgeRepo,
  listKnowledgeDocs,
  uploadKnowledgeDoc,
  getKnowledgeDocParseStatus,
  importKnowledgeDocs,
  getKnowledgeImportStatus,
  initKnowledgeRepo,
  getKnowledgeRepoBuildStatus,
  deleteKnowledgeDoc,
  listKnowledgeCategories,
  updateKnowledgeDocTags
} from '@/api/knowledge'
import type { KnowledgeDoc, KnowledgeCategory } from '@/types/knowledge'
import { indexStatusLabel, indexStatusTagType, KNOWLEDGE_PARSER_OPTIONS, parseStatusLabel } from '@/types/knowledge'
import ChunkDialog from '@/components/ChunkDialog/index.vue'

const props = defineProps<{ repoId: number }>()

const docs = ref<KnowledgeDoc[]>([])
const loading = ref(false)
/** 仓库是否已在百炼建库（懒建库模式下首个文档解析后自动建库） */
const indexed = ref(false)

/** 上传处理中任务（解析/建库/导入轮询） */
interface UploadTask {
  fileId: string
  fileName: string
  status: 'parsing' | 'creating' | 'importing' | 'done' | 'failed'
  message?: string
}

const tasks = ref<UploadTask[]>([])
const timers: ReturnType<typeof setTimeout>[] = []

onMounted(async () => {
  await loadRepoInfo()
  loadDocs()
  loadCategories()
})
onBeforeUnmount(() => timers.forEach(clearTimeout))

async function loadRepoInfo() {
  try {
    const repo = await getKnowledgeRepo(props.repoId)
    indexed.value = !!repo.indexId
  } catch {
    indexed.value = false
  }
}

async function loadDocs() {
  loading.value = true
  try {
    docs.value = await listKnowledgeDocs(props.repoId, { pageNumber: 1, pageSize: 100 })
  } catch {
    docs.value = []
  } finally {
    loading.value = false
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

/** 确认上传：按设置逐个上传 */
async function confirmUpload() {
  uploadDialogVisible.value = false
  const files = pendingFiles.value
  pendingFiles.value = []
  for (const file of files) {
    await handleUpload(file, {
      categoryId: uploadSetting.categoryId || undefined,
      parser: uploadSetting.parser,
      tags: uploadSetting.tags
    })
  }
}

async function handleUpload(file: File, opts: { categoryId?: string; parser: string; tags: string[] }) {
  const fileName = file.name
  let fileId: string
  try {
    fileId = await uploadKnowledgeDoc(props.repoId, file, opts, true)
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
  timers.push(
    setTimeout(async () => {
      try {
        const info = await getKnowledgeDocParseStatus(props.repoId, task.fileId)
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
  timers.push(
    setTimeout(async () => {
      try {
        const jobId = await initKnowledgeRepo(props.repoId, [task.fileId])
        pollBuildJob(task, jobId)
      } catch {
        task.status = 'failed'
        task.message = '初始化建库失败'
      }
    }, 0)
  )
}

function pollBuildJob(task: UploadTask, jobId: string) {
  timers.push(
    setTimeout(async () => {
      try {
        const status = await getKnowledgeRepoBuildStatus(props.repoId)
        if (status === 'FINISH' || status === 'COMPLETED') {
          task.status = 'done'
          indexed.value = true
          ElMessage.success(`「${task.fileName}」已入库，知识库创建完成`)
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
  timers.push(
    setTimeout(async () => {
      try {
        const jobId = await importKnowledgeDocs(props.repoId, [task.fileId])
        pollImportJob(task, jobId)
      } catch {
        task.status = 'failed'
        task.message = '导入索引失败'
      }
    }, 0)
  )
}

function pollImportJob(task: UploadTask, jobId: string) {
  timers.push(
    setTimeout(async () => {
      try {
        const status = await getKnowledgeImportStatus(props.repoId, jobId)
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

// ---------- 删除 ----------
async function handleDelete(row: KnowledgeDoc) {
  await ElMessageBox.confirm(`确定从知识库永久删除「${row.fileName}」？删除后不可恢复。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteKnowledgeDoc(props.repoId, row.fileId)
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

async function openEditTags(row: KnowledgeDoc) {
  editingFile.value = row
  editTags.value = [...(row.tags || [])]
  editTagsVisible.value = true
}

async function confirmEditTags() {
  if (!editingFile.value) return
  const tags = editTags.value.slice(0, 10)
  await updateKnowledgeDocTags(props.repoId, editingFile.value.fileId, tags)
  editingFile.value.tags = tags
  editTagsVisible.value = false
  ElMessage.success('标签已更新')
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
</script>

<template>
  <div class="doc-tab">
    <div class="doc-upload">
      <el-upload
        :show-file-list="false"
        :http-request="handleSelectFile"
        :multiple="true"
        :accept="'.pdf,.doc,.docx,.md,.txt,.xls,.xlsx,.ppt,.pptx'"
        drag
      >
        <div class="upload-inner">
          <el-icon size="36" color="#909399"><UploadFilled /></el-icon>
          <div class="upload-text">拖拽文件到此处，或<em>点击上传</em></div>
          <div class="upload-tip">支持 PDF / Word / Markdown / TXT / Excel / PPT，单文件 ≤ 100MB；上传后自动解析并入索引</div>
        </div>
      </el-upload>
    </div>

    <!-- 上传设置 -->
    <el-dialog v-model="uploadDialogVisible" title="上传设置" width="480px" :close-on-click-modal="false">
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
        <el-button @click="uploadDialogVisible = false">取消</el-button>
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

    <el-table v-loading="loading" :data="docs" border stripe row-key="fileId" class="doc-table">
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
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 切片管理弹窗 -->
    <ChunkDialog ref="chunkDialogRef" :repo-id="props.repoId" />
  </div>
</template>

<style scoped lang="scss">
.doc-tab {
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
}
</style>
