<script setup lang="ts">
/**
 * 渠道详情页 - 知识仓库 tab（树形视图，admin 全权管理）。
 *
 * 展示「当前渠道 + 全部后代」的树形知识库：
 * - 左侧 el-tree：渠道树，节点显示「渠道简称 + 库状态」（自有/继承自XX/未配置）；
 * - 右侧面板按选中节点渲染，admin 可全权管理任意节点的仓库：
 *   有独立库 → 摘要（编辑/同步/删除）+ 文档管理（复用 DocTab）+ 问答（复用 ChatTab）；
 *   无独立库但有继承 → 摘要 + 问答（继承库只读使用）；
 *   无库 → 空态 + 创建弹窗（按选中渠道简称预填「{简称}知识库」）。
 *
 * 数据走 admin 自身接口（/admin-api/knowledge/repos/tree），与 channel 端各自独立。
 */
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getKnowledgeRepoTree, createKnowledgeRepo, updateKnowledgeRepo, deleteKnowledgeRepo, syncKnowledgeRepo } from '@/api/knowledge'
import type { KnowledgeRepoTreeNode } from '@/types/knowledge'
import { knowledgeRepoStatusLabel, knowledgeRepoStatusTagType } from '@/types/knowledge'
import { formatDateTime } from '@/utils/format'
import DocTab from '@/views/system/knowledge/detail/DocTab.vue'
import ChatTab from '@/views/system/knowledge/detail/ChatTab.vue'

const props = defineProps<{
  channelCode: string
}>()

const loading = ref(false)
const treeData = ref<KnowledgeRepoTreeNode[]>([])
const selectedNode = ref<KnowledgeRepoTreeNode | null>(null)

async function loadTree() {
  if (!props.channelCode) return
  loading.value = true
  try {
    const tree = await getKnowledgeRepoTree(props.channelCode)
    treeData.value = tree ?? []
    selectedNode.value = tree?.[0] ?? null
  } catch {
    treeData.value = []
    selectedNode.value = null
  } finally {
    loading.value = false
  }
}

// channelCode 变化时重新加载（watch immediate 由父组件传值时机决定）
watch(() => props.channelCode, loadTree, { immediate: true })

/** 选中节点的独立仓库（admin 全权管理对象） */
const ownRepo = computed(() => selectedNode.value?.repo ?? null)
/** 选中节点的实际可用仓库（独立或继承） */
const effectiveRepo = computed(() => selectedNode.value?.effectiveRepo ?? null)

// ---------- 创建 ----------
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = ref({ repoName: '', description: '' })

const createRules: FormRules = {
  repoName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }]
}

function openCreate() {
  createForm.value = { repoName: '', description: '' }
  // 默认名：选中渠道简称 + 知识库
  const node = selectedNode.value
  if (node) {
    createForm.value.repoName = `${node.shortName || node.fullName}知识库`
  }
  createVisible.value = true
}

async function handleCreate() {
  if (!createFormRef.value || !selectedNode.value) return
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  createLoading.value = true
  try {
    await createKnowledgeRepo({
      repoName: createForm.value.repoName.trim(),
      repoType: 2,
      channelCode: selectedNode.value.channelCode,
      description: createForm.value.description.trim() || undefined
    })
    ElMessage.success('创建成功，上传首个文档后将自动在百炼建库')
    createVisible.value = false
    loadTree()
  } finally {
    createLoading.value = false
  }
}

// ---------- 编辑 ----------
const editVisible = ref(false)
const editLoading = ref(false)
const editForm = ref({ repoName: '', description: '' })

function openEdit() {
  if (!ownRepo.value?.id) return
  editForm.value = {
    repoName: ownRepo.value.repoName ?? '',
    description: ownRepo.value.description ?? ''
  }
  editVisible.value = true
}

async function handleEdit() {
  if (!ownRepo.value?.id) return
  editLoading.value = true
  try {
    await updateKnowledgeRepo(ownRepo.value.id, {
      repoName: editForm.value.repoName.trim(),
      description: editForm.value.description.trim()
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadTree()
  } finally {
    editLoading.value = false
  }
}

// ---------- 同步 / 删除 ----------
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

// ---------- 树节点辅助 ----------
function nodeLabel(node: KnowledgeRepoTreeNode): string {
  return node.shortName || node.fullName || node.channelCode
}

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

function onSelectNode(node: KnowledgeRepoTreeNode) {
  selectedNode.value = node
}
</script>

<template>
  <div v-loading="loading" class="knowledge-tab">
    <div class="tree-panel">
      <el-tree
        :data="treeData"
        node-key="channelCode"
        :props="{ label: 'fullName', children: 'children' }"
        default-expand-all
        highlight-current
        :current-node-key="selectedNode?.channelCode"
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
    </div>

    <div class="detail-panel">
      <!-- 未选中 -->
      <el-empty v-if="!selectedNode && !loading" description="请在左侧选择渠道" :image-size="80" />

      <!-- 无可用库 -->
      <el-card v-else-if="selectedNode && !effectiveRepo" shadow="never" class="empty-card">
        <el-empty :description="`${nodeLabel(selectedNode)} 暂无可用知识库`" :image-size="80">
          <div class="empty-tip">
            创建后即可上传产品手册、服务说明等资料；上传首个文档并解析成功后自动在百炼云端建库，随后可进行 AI 问答。
          </div>
          <el-button type="primary" v-permission="'knowledge:repo:create'" @click="openCreate">
            创建知识仓库
          </el-button>
        </el-empty>
      </el-card>

      <!-- 有可用库 -->
      <template v-else-if="selectedNode && effectiveRepo">
        <el-card shadow="never" class="summary-card">
          <div class="summary-header">
            <div class="summary-title">
              <span class="repo-name">{{ effectiveRepo.repoName }}</span>
              <el-tag size="small" :type="knowledgeRepoStatusTagType(effectiveRepo.status)">
                {{ knowledgeRepoStatusLabel(effectiveRepo.status) }}
              </el-tag>
              <el-tag v-if="selectedNode.inheritedFrom" size="small" type="warning">
                继承自{{ selectedNode.inheritedFromName || selectedNode.inheritedFrom }}
              </el-tag>
              <span class="repo-code">{{ effectiveRepo.repoCode }}</span>
            </div>
            <div class="summary-actions">
              <!-- admin 全权：仅独立库可管理；继承库只读 -->
              <template v-if="ownRepo">
                <el-button size="small" v-permission="'knowledge:repo:update'" @click="openEdit">编辑</el-button>
                <el-button size="small" type="success" v-permission="'knowledge:repo:sync'" @click="handleSync">
                  同步
                </el-button>
                <el-button size="small" type="danger" v-permission="'knowledge:repo:delete'" @click="handleDelete">
                  删除
                </el-button>
              </template>
              <el-tag v-else size="small" type="info">继承库（只读）</el-tag>
            </div>
          </div>
          <div class="summary-meta">
            <span>文档数：{{ effectiveRepo.docCount ?? 0 }}</span>
            <span v-if="effectiveRepo.indexId" :title="effectiveRepo.indexId">索引：{{ effectiveRepo.indexId }}</span>
            <span v-else>索引：未建库</span>
            <span>最近同步：{{ effectiveRepo.lastSyncAt ? formatDateTime(effectiveRepo.lastSyncAt) : '--' }}</span>
            <span>创建时间：{{ effectiveRepo.createdAt ? formatDateTime(effectiveRepo.createdAt) : '--' }}</span>
          </div>
        </el-card>

        <!-- 独立库：文档管理 + 问答；继承库：仅问答 -->
        <el-tabs v-if="ownRepo" type="border-card" class="repo-tabs">
          <el-tab-pane label="文档管理" name="docs" lazy>
            <DocTab :repo-id="ownRepo.id!" />
          </el-tab-pane>
          <el-tab-pane label="问答测试" name="chat" lazy>
            <ChatTab :repo-id="effectiveRepo.id!" />
          </el-tab-pane>
        </el-tabs>
        <el-card v-else shadow="never" class="chat-card">
          <template #header>问答测试（继承库只读使用）</template>
          <ChatTab :repo-id="effectiveRepo.id!" />
        </el-card>
      </template>
    </div>

    <!-- 创建弹窗 -->
    <el-dialog v-model="createVisible" title="创建知识仓库" width="520px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="仓库名称" prop="repoName">
          <el-input v-model="createForm.repoName" placeholder="如：本渠道知识库" maxlength="100" />
        </el-form-item>
        <el-form-item label="所属渠道">
          <el-input :model-value="selectedNode ? nodeLabel(selectedNode) : ''" disabled />
        </el-form-item>
        <el-form-item label="仓库描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" maxlength="255" placeholder="选填" />
        </el-form-item>
        <div class="form-tip">创建成功后上传首个文档，解析完成将自动在百炼云端建库。</div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

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
        <el-button type="primary" :loading="editLoading" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.knowledge-tab {
  display: flex;
  gap: 16px;
  align-items: flex-start;

  .tree-panel {
    width: 300px;
    flex-shrink: 0;
    border: 1px solid #e4e7ed;
    border-radius: 6px;
    padding: 12px;
    background: #fff;

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

  .detail-panel {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;

    .empty-card {
      .empty-tip {
        color: #909399;
        font-size: 13px;
        margin-bottom: 16px;
        max-width: 480px;
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
    }

    .chat-card {
      :deep(.el-card__body) {
        padding-top: 12px;
      }
    }
  }

  .form-tip {
    font-size: 12px;
    color: #909399;
    line-height: 1.5;
    padding-left: 90px;
  }
}
</style>
