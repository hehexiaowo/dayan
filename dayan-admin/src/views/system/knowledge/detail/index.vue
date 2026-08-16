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
import type { KnowledgeRepo } from '@/types/knowledge'
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

async function loadDetail() {
  detailLoading.value = true
  try {
    repo.value = await getKnowledgeRepo(repoId.value)
    basicForm.value = {
      repoName: repo.value.repoName ?? '',
      description: repo.value.description ?? '',
      sortOrder: repo.value.sortOrder ?? 0
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
}
</style>
