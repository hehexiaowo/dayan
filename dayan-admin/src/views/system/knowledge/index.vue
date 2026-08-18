<script setup lang="ts">
/**
 * 知识仓库管理页（系统管理 → 知识仓库）。
 *
 * - 列表 + 分页 + 平台/渠道筛选 + 新建（新建远端索引 / 绑定已有 IndexId）+ 删除（同步删远端）；
 * - 主键 id（自增），详情页 KnowledgeDetail 跳转；
 * - 渠道下拉复用渠道列表接口（树形渠道取全部节点）。
 */
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageKnowledgeRepos,
  createKnowledgeRepo,
  deleteKnowledgeRepo,
  syncKnowledgeRepo
} from '@/api/knowledge'
import type { KnowledgeRepo, KnowledgeRepoQuery } from '@/types/knowledge'
import {
  KNOWLEDGE_REPO_TYPE_OPTIONS,
  KNOWLEDGE_REPO_STATUS_OPTIONS,
  knowledgeRepoTypeLabel,
  knowledgeRepoStatusLabel,
  knowledgeRepoStatusTagType
} from '@/types/knowledge'
import { listChannels } from '@/api/channel'
import type { ChannelInfo } from '@/types/channel'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<KnowledgeRepo, KnowledgeRepoQuery, number>(
    { page: pageKnowledgeRepos, remove: deleteKnowledgeRepo },
    {
      idKey: 'id',
      initialQuery: {
        repoName: '',
        repoType: undefined,
        channelCode: undefined,
        status: undefined
      }
    }
  )

loadPage()

/** 重置筛选并重新查询 */
function handleReset() {
  Object.assign(query, { repoName: '', repoType: undefined, channelCode: undefined, status: undefined })
  handleSearch()
}

// ---------- 渠道下拉 ----------
const channels = ref<ChannelInfo[]>([])

onMounted(async () => {
  try {
    channels.value = await listChannels()
  } catch {
    channels.value = []
  }
})

// ---------- 新建仓库弹窗 ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  repoName: '',
  repoType: 1,
  channelCode: undefined as string | undefined,
  mode: 'create' as 'create' | 'bind',
  indexId: '',
  description: '',
  sortOrder: 0
})

const rules: FormRules = {
  repoName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  channelCode: [
    {
      validator: (_r, _v, cb) => {
        if (form.repoType === 2 && !form.channelCode) cb(new Error('请选择渠道'))
        else cb()
      },
      trigger: 'change'
    }
  ],
  indexId: [
    {
      validator: (_r, _v, cb) => {
        if (form.mode === 'bind' && !form.indexId.trim()) cb(new Error('请输入百炼索引 ID'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

function resetForm() {
  Object.assign(form, {
    repoName: '',
    repoType: 1,
    channelCode: undefined,
    mode: 'create',
    indexId: '',
    description: '',
    sortOrder: 0
  })
}

/** 归属类型切换：渠道时按渠道简称预填默认仓库名「{简称}知识库」 */
function handleRepoTypeChange() {
  if (form.repoType === 2) {
    const ch = channels.value.find((c) => c.channelCode === form.channelCode)
    if (ch) form.repoName = `${ch.shortName || ch.fullName}知识库`
  }
}

/** 选择渠道后按渠道简称预填默认仓库名（未手动修改时） */
function handleChannelChange() {
  if (form.repoType !== 2) return
  const ch = channels.value.find((c) => c.channelCode === form.channelCode)
  if (ch) form.repoName = `${ch.shortName || ch.fullName}知识库`
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    await createKnowledgeRepo({
      repoName: form.repoName,
      repoType: form.repoType,
      channelCode: form.repoType === 2 ? form.channelCode : undefined,
      mode: form.mode,
      indexId: form.mode === 'bind' ? form.indexId.trim() : undefined,
      description: form.description,
      sortOrder: form.sortOrder
    })
    ElMessage.success(form.mode === 'bind' ? '绑定成功' : '创建成功，上传首个文档后将自动在百炼建库')
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

// ---------- 操作 ----------
function goDetail(row: KnowledgeRepo) {
  router.push({ name: 'KnowledgeDetail', params: { id: String(row.id) } })
}

async function handleSync(row: KnowledgeRepo) {
  if (!row.id) return
  try {
    await syncKnowledgeRepo(row.id)
    ElMessage.success('同步成功')
  } catch {
    // 同步失败会置状态为异常，刷新列表展示
  }
  loadPage()
}

async function handleDelete(row: KnowledgeRepo) {
  if (!row.id) return
  await ElMessageBox.confirm(
    '删除仓库将同时删除百炼云端的知识库索引及全部文档，且不可恢复。确定删除？',
    '危险操作',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deleteKnowledgeRepo(row.id)
  ElMessage.success('删除成功')
  loadPage()
}
</script>

<template>
  <div class="knowledge-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input v-model="query.repoName" placeholder="仓库名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        <el-select v-model="query.repoType" placeholder="归属类型" clearable style="width: 130px">
          <el-option v-for="o in KNOWLEDGE_REPO_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.channelCode" placeholder="渠道" clearable filterable style="width: 200px">
          <el-option v-for="c in channels" :key="c.channelCode" :label="c.fullName" :value="c.channelCode" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="o in KNOWLEDGE_REPO_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span class="card-title">知识仓库列表</span>
          <el-button type="primary" :icon="'Plus'" v-permission="'knowledge:repo:create'" @click="openCreate">
            新建知识仓库
          </el-button>
        </div>
      </template>

      <el-alert type="info" :closable="false" style="margin-bottom: 12px">
        知识仓库与阿里云百炼知识库一一对应：平台级一个 + 每个渠道一个。文档与解析状态以百炼云端为准，本页实时同步展示。
      </el-alert>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="repoName" label="仓库名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row)">{{ row.repoName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="repoCode" label="编码" width="110" align="center" />
        <el-table-column label="归属" width="190">
          <template #default="{ row }">
            <el-tag size="small" :type="row.repoType === 2 ? 'warning' : 'primary'">
              {{ knowledgeRepoTypeLabel(row.repoType) }}
            </el-tag>
            <span v-if="row.repoType === 2" class="channel-name">{{ row.channelShortName || row.channelName || row.channelCode }}</span>
            <span v-else class="channel-name">大雁养老</span>
          </template>
        </el-table-column>
        <el-table-column prop="indexId" label="百炼索引 ID" min-width="200" show-overflow-tooltip />
        <el-table-column prop="docCount" label="文档数" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="knowledgeRepoStatusTagType(row.status)" size="small">
              {{ knowledgeRepoStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastSyncAt" label="最近同步" width="160" align="center">
          <template #default="{ row }">{{ row.lastSyncAt ? formatDateTime(row.lastSyncAt) : '--' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ row.createdAt ? formatDateTime(row.createdAt) : '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
            <el-button link type="success" size="small" v-permission="'knowledge:repo:sync'" @click="handleSync(row)">
              同步
            </el-button>
            <el-button link type="danger" size="small" v-permission="'knowledge:repo:delete'" @click="handleDelete(row)">
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

    <!-- 新建仓库弹窗 -->
    <el-dialog v-model="dialogVisible" title="新建知识仓库" width="620px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="仓库名称" prop="repoName">
          <el-input v-model="form.repoName" placeholder="如：大雁养老平台知识库 / xx渠道知识库" maxlength="100" />
        </el-form-item>
        <el-form-item label="归属类型">
          <el-radio-group v-model="form.repoType" @change="handleRepoTypeChange">
            <el-radio :value="1">平台（大雁养老）</el-radio>
            <el-radio :value="2">渠道</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.repoType === 2" label="所属渠道" prop="channelCode">
          <el-select v-model="form.channelCode" placeholder="选择渠道" filterable style="width: 100%" @change="handleChannelChange">
            <el-option v-for="c in channels" :key="c.channelCode" :label="c.fullName" :value="c.channelCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建方式">
          <el-radio-group v-model="form.mode">
            <el-radio value="create">新建（懒建库）</el-radio>
            <el-radio value="bind">绑定已有索引</el-radio>
          </el-radio-group>
          <div class="form-tip">
            新建 = 先创建本地仓库，上传首个文档解析成功后自动在百炼建库；绑定 = 关联百炼控制台已创建的索引（填下方索引 ID）。
          </div>
        </el-form-item>
        <el-form-item v-if="form.mode === 'bind'" label="百炼索引 ID" prop="indexId">
          <el-input v-model="form.indexId" placeholder="百炼控制台-知识库详情中的索引 ID" />
        </el-form-item>
        <el-form-item label="仓库描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" placeholder="选填" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.knowledge-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .card-title {
      font-size: 15px;
      font-weight: 600;
      color: #1f2329;
    }
  }

  .search-card {
    :deep(.el-card__body) {
      padding-bottom: 2px;
    }
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
  .channel-name {
    margin-left: 6px;
    color: #606266;
    font-size: 13px;
  }
  .form-tip {
    font-size: 12px;
    color: #909399;
    line-height: 1.5;
    width: 100%;
  }
}
</style>
