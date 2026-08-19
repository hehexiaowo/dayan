<script setup lang="ts">
/**
 * 知识仓库详情页。
 *
 * 顶部摘要 + el-tabs：基本信息 / 文档管理 / 问答测试。
 * 详情路由 KnowledgeDetail（静态注册，params.id = 仓库自增 id）。
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getKnowledgeRepo, updateKnowledgeRepo, syncKnowledgeRepo, getKnowledgeRepoBuildStatus } from '@/api/knowledge'
import type { KnowledgeRepo, KnowledgeIndexConfig } from '@/types/knowledge'
import { knowledgeRepoTypeLabel, knowledgeRepoStatusLabel, knowledgeRepoStatusTagType } from '@/types/knowledge'
import { formatDateTime } from '@/utils/format'
import DocTab from './DocTab.vue'
import ChatTab from './ChatTab.vue'

const route = useRoute()
const router = useRouter()
const repoId = computed(() => Number(route.params.id))

const activeTab = ref('basic')
const detailLoading = ref(false)
const repo = ref<KnowledgeRepo | null>(null)

const basicForm = ref({
  repoName: '',
  description: '',
  sortOrder: 0
})
const saving = ref(false)

/** 检索参数行内编辑表单（已建库可改，默认与新建弹窗一致） */
const retrieverForm = ref<KnowledgeIndexConfig>({
  denseTopK: 4,
  sparseTopK: 4,
  rerankMinScore: 0.01
})
const retrieverSaving = ref(false)

/** 是否已在百炼建库（有远端索引 ID） */
const isIndexed = computed(() => Boolean(repo.value?.indexId))

/** 索引配置（可能为空 = 未配置，使用百炼默认） */
const cfg = computed(() => repo.value?.indexConfig)

/** 重排模式标签 */
function rerankModeLabel(v?: string): string {
  switch (v) {
    case 'qa':
      return '问答模式（qa）'
    case 'similar':
      return '相似模式（similar）'
    case 'custom':
      return '自定义（custom）'
    default:
      return '--'
  }
}

async function loadDetail() {
  detailLoading.value = true
  try {
    repo.value = await getKnowledgeRepo(repoId.value)
    basicForm.value = {
      repoName: repo.value.repoName ?? '',
      description: repo.value.description ?? '',
      sortOrder: repo.value.sortOrder ?? 0
    }
    retrieverForm.value = {
      denseTopK: repo.value.indexConfig?.denseTopK ?? 4,
      sparseTopK: repo.value.indexConfig?.sparseTopK ?? 4,
      rerankMinScore: repo.value.indexConfig?.rerankMinScore ?? 0.01
    }
  } catch {
    repo.value = null
  } finally {
    detailLoading.value = false
  }
}

onMounted(loadDetail)

async function handleSaveBasic() {
  if (!repo.value?.id) return
  saving.value = true
  try {
    await updateKnowledgeRepo(repo.value.id, basicForm.value)
    ElMessage.success('保存成功')
    loadDetail()
  } finally {
    saving.value = false
  }
}

/** 保存检索参数：提交完整 indexConfig（原值 + 修改的三个字段），已建库后端仅允许这三项变更 */
async function handleSaveRetriever() {
  if (!repo.value?.id) return
  retrieverSaving.value = true
  try {
    await updateKnowledgeRepo(repo.value.id, {
      indexConfig: {
        ...(repo.value.indexConfig ?? {}),
        denseTopK: retrieverForm.value.denseTopK,
        sparseTopK: retrieverForm.value.sparseTopK,
        rerankMinScore: retrieverForm.value.rerankMinScore
      }
    })
    ElMessage.success('检索参数已保存')
    loadDetail()
  } finally {
    retrieverSaving.value = false
  }
}

async function handleSync() {
  if (!repo.value?.id) return
  await syncKnowledgeRepo(repo.value.id)
  ElMessage.success('同步成功')
  loadDetail()
}

async function handleCheckBuild() {
  if (!repo.value?.id) return
  const status = await getKnowledgeRepoBuildStatus(repo.value.id)
  const text =
    status === 'UNBOUND'
      ? '尚未在百炼建库：上传首个文档并等待解析完成后将自动建库'
      : status === 'FINISH'
        ? '索引构建已完成，仓库可用'
        : status === 'FAILED'
          ? '索引构建失败（可在百炼控制台查看）'
          : '索引构建中，请稍后刷新'
  ElMessage.info(text)
  loadDetail()
}

function goBack() {
  router.back()
}
</script>

<template>
  <div class="knowledge-detail" v-loading="detailLoading">
    <el-page-header @back="goBack">
      <template #content>
        <div v-if="repo" class="header-info">
          <span class="repo-name">{{ repo.repoName }}</span>
          <el-tag size="small" :type="knowledgeRepoStatusTagType(repo.status)" class="repo-tag">
            {{ knowledgeRepoStatusLabel(repo.status) }}
          </el-tag>
          <el-tag size="small" type="info" class="repo-tag">{{ knowledgeRepoTypeLabel(repo.repoType) }}</el-tag>
          <span v-if="repo.channelName" class="channel-name">{{ repo.channelName }}</span>
          <span class="index-id" title="百炼远端索引 ID">索引：{{ repo.indexId }}</span>
        </div>
      </template>
      <template #extra>
        <el-button v-if="repo" @click="handleCheckBuild">构建状态</el-button>
        <el-button v-if="repo" type="success" @click="handleSync">同步</el-button>
      </template>
    </el-page-header>

    <el-card v-if="repo" shadow="never" class="detail-card">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="基本信息" name="basic">
          <el-form label-width="100px" style="max-width: 640px" class="basic-form">
            <el-form-item label="仓库编码">
              <el-input :model-value="repo.repoCode" disabled />
            </el-form-item>
            <el-form-item label="仓库名称">
              <el-input v-model="basicForm.repoName" maxlength="100" />
            </el-form-item>
            <el-form-item label="归属类型">
              <el-input :model-value="knowledgeRepoTypeLabel(repo.repoType)" disabled />
            </el-form-item>
            <el-form-item v-if="repo.repoType === 2" label="所属渠道">
              <el-input :model-value="repo.channelName || repo.channelCode" disabled />
            </el-form-item>
            <el-form-item label="百炼索引 ID">
              <el-input :model-value="repo.indexId" disabled />
            </el-form-item>
            <el-form-item label="文档数">
              <el-input :model-value="String(repo.docCount ?? 0)" disabled />
            </el-form-item>
            <el-form-item label="最近同步">
              <el-input :model-value="repo.lastSyncAt ? formatDateTime(repo.lastSyncAt) : '--'" disabled />
            </el-form-item>
            <el-form-item label="创建时间">
              <el-input :model-value="repo.createdAt ? formatDateTime(repo.createdAt) : '--'" disabled />
            </el-form-item>
            <el-form-item label="仓库描述">
              <el-input v-model="basicForm.description" type="textarea" :rows="2" maxlength="255" />
            </el-form-item>
            <el-form-item label="排序号">
              <el-input-number v-model="basicForm.sortOrder" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSaveBasic">保存</el-button>
            </el-form-item>
          </el-form>

          <!-- 索引配置：只读展示；已建库时检索参数可编辑 -->
          <div class="index-config-card">
            <div class="card-header">
              <span class="card-title">索引配置</span>
              <el-tag size="small" :type="isIndexed ? 'success' : 'warning'">
                {{ isIndexed ? '已建库' : '未建库' }}
              </el-tag>
            </div>

            <el-descriptions v-if="cfg" :column="2" border>
              <el-descriptions-item label="切分方式">
                {{ cfg.chunkMode === 'regex' ? '自定义切分' : '智能切分' }}
              </el-descriptions-item>
              <template v-if="cfg.chunkMode === 'regex'">
                <el-descriptions-item label="分隔符">{{ cfg.separator || '--' }}</el-descriptions-item>
                <el-descriptions-item label="切块长度">{{ cfg.chunkSize ?? '--' }}</el-descriptions-item>
                <el-descriptions-item label="重叠长度">{{ cfg.overlapSize ?? '--' }}</el-descriptions-item>
              </template>
              <el-descriptions-item label="向量模型">{{ cfg.embeddingModel || '--' }}</el-descriptions-item>
              <el-descriptions-item label="重排模型">{{ cfg.rerankModel || '--' }}</el-descriptions-item>
              <el-descriptions-item label="重排模式">{{ rerankModeLabel(cfg.rerankMode) }}</el-descriptions-item>
              <el-descriptions-item label="相似度阈值">{{ cfg.rerankMinScore ?? '--' }}</el-descriptions-item>
              <el-descriptions-item label="多轮改写">{{ cfg.enableRewrite === false ? '关闭' : '开启' }}</el-descriptions-item>
              <el-descriptions-item label="召回 TopK">
                dense {{ cfg.denseTopK ?? '--' }} / sparse {{ cfg.sparseTopK ?? '--' }}
              </el-descriptions-item>
            </el-descriptions>
            <div v-else class="default-tip">使用百炼默认（智能切分）</div>

            <template v-if="isIndexed">
              <div class="retriever-block">
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
            </template>
            <el-alert
              v-else
              class="unbound-tip"
              type="info"
              :closable="false"
              show-icon
              title="上传首个文档并建库后，此处仅检索参数可编辑"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="文档管理" name="docs">
          <DocTab :repo-id="repoId" />
        </el-tab-pane>

        <el-tab-pane label="问答测试" name="chat">
          <ChatTab :repo-id="repoId" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.knowledge-detail {
  .header-info {
    display: flex;
    align-items: center;
    gap: 8px;
    .repo-name {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
    .channel-name {
      font-size: 13px;
      color: #606266;
    }
    .index-id {
      font-size: 12px;
      color: #909399;
      margin-left: 4px;
    }
  }
  .detail-card {
    margin-top: 16px;
  }
  .index-config-card {
    max-width: 860px;
    margin-top: 24px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;

    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;

      .card-title {
        font-size: 15px;
        font-weight: 600;
        color: #1f2329;
      }
    }

    .default-tip {
      padding: 8px 0;
      font-size: 13px;
      color: #606266;
    }

    .retriever-block {
      margin-top: 20px;
      padding-top: 16px;
      border-top: 1px solid #ebeef5;

      .retriever-title {
        margin-bottom: 12px;
        font-size: 14px;
        font-weight: 600;
        color: #303133;
      }
    }

    .unbound-tip {
      margin-top: 16px;
    }
  }
}
</style>
