<script setup lang="ts">
/**
 * 知识仓库详情 - 文档管理 Tab。
 *
 * 文档全链路（全部实时代理百炼云端）：
 * 上传 → 解析（DescribeFile 轮询）→ 导入索引（SubmitIndexAddDocumentsJob）→
 * 入库轮询（GetIndexJobStatus）→ 刷新列表。任一环节失败展示原因。
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'
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
  deleteKnowledgeDoc
} from '@/api/knowledge'
import type { KnowledgeDoc } from '@/types/knowledge'
import { indexStatusLabel, indexStatusTagType } from '@/types/knowledge'

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

// ---------- 上传与轮询 ----------
async function handleUpload(options: UploadRequestOptions) {
  const fileName = options.file.name
  let fileId: string
  try {
    fileId = await uploadKnowledgeDoc(props.repoId, options.file)
  } catch {
    ElMessage.error(`「${fileName}」上传失败`)
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
        :http-request="handleUpload"
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
      <el-table-column label="更新时间" width="170" align="center">
        <template #default="{ row }">{{ formatTime(row.gmtModified) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无文档，上传后自动解析入库" :image-size="80" />
      </template>
    </el-table>
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
