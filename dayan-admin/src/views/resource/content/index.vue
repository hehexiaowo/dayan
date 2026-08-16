<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageContents,
  getContent,
  createContent,
  updateContent,
  deleteContent,
  submitContent,
  auditContent,
  publishContent,
  offlineContent
} from '@/api/content'
import { useDictOptions } from '@/composables/useDict'
import type { ContentInfo, ContentInfoQuery } from '@/types/content'
import {
  ContentType,
  ContentStatus,
  CONTENT_TYPE_OPTIONS,
  CONTENT_STATUS_OPTIONS,
  AUDIT_STATUS_OPTIONS,
  SOURCE_TYPE_OPTIONS
} from '@/types/content'
import FileUploader from '@/components/FileUploader/index.vue'
import RichEditor from '@/components/RichEditor/index.vue'
import { NETWORK_TYPE_OPTIONS, networkTagsToList } from '@/types/park'
import { formatDateTime } from '@/utils/format'

/**
 * 内容素材管理页。
 *
 * - 标准 CRUD（搜索 + 表格 + 分页 + 新增/编辑弹窗）；
 * - 额外审核流：提交审核 / 审核（通过或驳回）/ 发布 / 下线，操作按钮按 contentStatus 动态显示。
 */

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<ContentInfo, ContentInfoQuery>(
  { page: pageContents },
  {
    initialQuery: {
      title: '',
      contentType: undefined,
      contentStatus: undefined,
      categoryCode: ''
    }
  }
)

const router = useRouter()

/** 分类下拉选项 + 名称映射（业务字典 content_category 承载，VO 不带名称前端自行映射） */
const { options: categoryOptions } = useDictOptions('content_category')
const categoryNameMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  for (const c of categoryOptions.value) {
    if (c.dictCode) map[c.dictCode] = c.dictName ?? ''
  }
  return map
})

/** 跳转内容详情 */
function openDetail(row: ContentInfo) {
  if (!row.contentCode) return
  router.push({ name: 'ContentDetail', params: { contentCode: row.contentCode } })
}

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ContentInfo>({
  contentCode: undefined,
  title: '',
  subtitle: '',
  contentType: ContentType.ARTICLE,
  categoryCode: '',
  authorName: '',
  coverImage: '',
  summary: '',
  contentBody: '',
  sourceType: 1,
  sourceUrl: '',
  tags: '',
  networkTags: '',
  isTop: 0,
  isRecommend: 0,
  isComment: 1,
  sortOrder: 0,
  remark: ''
})

/** 业态多选数组态：提交时 join 为 form.networkTags，回显时 split */
const networkTagsArr = ref<string[]>([])

const rules: FormRules<ContentInfo> = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  contentType: [{ required: true, message: '请选择内容类型', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    contentCode: undefined,
    title: '',
    subtitle: '',
    contentType: ContentType.ARTICLE,
    categoryCode: '',
    authorName: '',
    coverImage: '',
    summary: '',
    contentBody: '',
    sourceType: 1,
    sourceUrl: '',
    tags: '',
    networkTags: '',
    isTop: 0,
    isRecommend: 0,
    isComment: 1,
    sortOrder: 0,
    remark: ''
  })
  networkTagsArr.value = []
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: ContentInfo) {
  if (!row.contentCode) return
  dialogType.value = 'edit'
  resetForm()
  try {
    const detail = await getContent(row.contentCode)
    Object.assign(form, {
      contentCode: detail.contentCode,
      title: detail.title ?? '',
      subtitle: detail.subtitle ?? '',
      contentType: detail.contentType ?? ContentType.ARTICLE,
      categoryCode: detail.categoryCode ?? '',
      authorName: detail.authorName ?? '',
      coverImage: detail.coverImage ?? '',
      summary: detail.summary ?? '',
      contentBody: detail.contentBody ?? '',
      sourceType: detail.sourceType ?? 1,
      sourceUrl: detail.sourceUrl ?? '',
      tags: detail.tags ?? '',
      networkTags: detail.networkTags ?? '',
      isTop: detail.isTop ?? 0,
      isRecommend: detail.isRecommend ?? 0,
      isComment: detail.isComment ?? 1,
      sortOrder: detail.sortOrder ?? 0,
      remark: detail.remark ?? ''
    })
    networkTagsArr.value = networkTagsToList(detail.networkTags)
  } catch {
    // 拉取详情失败时回退到行数据
    Object.assign(form, {
      contentCode: row.contentCode,
      title: row.title ?? '',
      subtitle: row.subtitle ?? '',
      contentType: row.contentType ?? ContentType.ARTICLE,
      categoryCode: row.categoryCode ?? '',
      authorName: row.authorName ?? '',
      coverImage: row.coverImage ?? '',
      summary: row.summary ?? '',
      contentBody: row.contentBody ?? '',
      sourceType: row.sourceType ?? 1,
      sourceUrl: row.sourceUrl ?? '',
      tags: row.tags ?? '',
      networkTags: row.networkTags ?? '',
      isTop: row.isTop ?? 0,
      isRecommend: row.isRecommend ?? 0,
      isComment: row.isComment ?? 1,
      sortOrder: row.sortOrder ?? 0,
      remark: row.remark ?? ''
    })
    networkTagsArr.value = networkTagsToList(row.networkTags)
  }
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
    // 勾选数组 → 逗号串（空串 = 全部业态）
    form.networkTags = networkTagsArr.value.join(',')
    if (dialogType.value === 'create') {
      await createContent(form)
      ElMessage.success('新增成功')
    } else if (form.contentCode) {
      await updateContent(form.contentCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

function handleReset() {
  query.title = ''
  query.contentType = undefined
  query.contentStatus = undefined
  query.categoryCode = ''
  handleSearch()
}

// ---------- 审核流操作 ----------
async function handleSubmitAudit(row: ContentInfo) {
  if (!row.contentCode) return
  await ElMessageBox.confirm(`确定提交「${row.title}」进入审核吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await submitContent(row.contentCode)
  ElMessage.success('已提交审核')
  loadPage()
}

async function handlePublish(row: ContentInfo) {
  if (!row.contentCode) return
  await ElMessageBox.confirm(`确定发布「${row.title}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await publishContent(row.contentCode)
  ElMessage.success('发布成功')
  loadPage()
}

async function handleOffline(row: ContentInfo) {
  if (!row.contentCode) return
  await ElMessageBox.confirm(`确定下线「${row.title}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await offlineContent(row.contentCode)
  ElMessage.success('已下线')
  loadPage()
}

async function handleDeleteRow(row: ContentInfo) {
  if (!row.contentCode) return
  try {
    await ElMessageBox.confirm(`确定删除内容「${row.title}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // 用户取消
  }
  await deleteContent(row.contentCode)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 审核弹窗 ----------
const auditDialogVisible = ref(false)
const auditSubmitLoading = ref(false)
const auditForm = reactive<{ contentCode: string; title: string; auditStatus: number; auditRemark: string }>({
  contentCode: '',
  title: '',
  // 2=通过 / 3=拒绝（对齐后端 audit 约定），默认通过
  auditStatus: 2,
  auditRemark: ''
})

function openAudit(row: ContentInfo) {
  if (!row.contentCode) return
  auditForm.contentCode = row.contentCode
  auditForm.title = row.title ?? ''
  auditForm.auditStatus = 2
  auditForm.auditRemark = ''
  auditDialogVisible.value = true
}

async function handleAuditSubmit() {
  auditSubmitLoading.value = true
  try {
    await auditContent({
      contentCode: auditForm.contentCode,
      auditStatus: auditForm.auditStatus,
      auditRemark: auditForm.auditRemark || undefined
    })
    ElMessage.success(auditForm.auditStatus === 2 ? '已通过' : '已驳回')
    auditDialogVisible.value = false
    loadPage()
  } finally {
    auditSubmitLoading.value = false
  }
}

// ---------- 辅助渲染 ----------
function contentTypeLabel(t?: number): string {
  const found = CONTENT_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function contentStatusLabel(s?: number): string {
  const found = CONTENT_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

/** 根据内容状态返回 el-tag type。 */
function contentStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case ContentStatus.PASS:
      return 'success'
    case ContentStatus.PENDING:
      return 'warning'
    case ContentStatus.REJECT:
      return 'danger'
    case ContentStatus.DRAFT:
    case ContentStatus.OFFLINE:
    default:
      return 'info'
  }
}

// 初始化加载
onMounted(() => {
  loadPage()
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input
          v-model="query.title"
          placeholder="标题"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.contentType" placeholder="内容类型" clearable style="width: 130px">
          <el-option v-for="o in CONTENT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.contentStatus" placeholder="内容状态" clearable style="width: 130px">
          <el-option v-for="o in CONTENT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.categoryCode" placeholder="分类" clearable filterable style="width: 160px">
          <el-option
            v-for="c in categoryOptions"
            :key="c.dictCode"
            :label="c.dictName"
            :value="c.dictCode"
          />
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
          <span class="card-title">内容列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增内容</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="contentCode">
        <el-table-column prop="contentCode" label="内容编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contentType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ contentTypeLabel(row.contentType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分类" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ categoryNameMap[row.categoryCode] || row.categoryCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="authorName" label="作者" min-width="120" show-overflow-tooltip />
        <el-table-column prop="viewCount" label="浏览量" width="90" align="center" />
        <el-table-column prop="contentStatus" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="contentStatusTagType(row.contentStatus)">
              {{ contentStatusLabel(row.contentStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.contentStatus === ContentStatus.DRAFT"
              link
              type="warning"
              size="small"
              @click="handleSubmitAudit(row)"
            >
              提交审核
            </el-button>
            <el-button
              v-if="row.contentStatus === ContentStatus.PENDING"
              link
              type="primary"
              size="small"
              @click="openAudit(row)"
            >
              审核
            </el-button>
            <el-button
              v-if="row.contentStatus === ContentStatus.PASS"
              link
              type="success"
              size="small"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-if="row.contentStatus === ContentStatus.PASS"
              link
              type="warning"
              size="small"
              @click="handleOffline(row)"
            >
              下线
            </el-button>
            <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
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
      :title="dialogType === 'create' ? '新增内容' : '编辑内容'"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="标题" prop="title">
              <el-input v-model="form.title" placeholder="标题" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="副标题">
              <el-input v-model="form.subtitle" placeholder="副标题" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="内容类型" prop="contentType">
              <el-select v-model="form.contentType" placeholder="内容类型" style="width: 100%">
                <el-option v-for="o in CONTENT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源类型">
              <el-select v-model="form.sourceType" placeholder="来源类型" style="width: 100%">
                <el-option v-for="o in SOURCE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.categoryCode" placeholder="选择分类" clearable filterable style="width: 100%">
                <el-option
                  v-for="c in categoryOptions"
                  :key="c.dictCode"
                  :label="c.dictName"
                  :value="c.dictCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者">
              <el-input v-model="form.authorName" placeholder="作者名称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="适用业态">
              <el-select v-model="networkTagsArr" multiple clearable placeholder="不选 = 全部业态展示" style="width: 100%">
                <el-option v-for="o in NETWORK_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <FileUploader v-model="form.coverImage" type="image" module="content" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="来源链接">
              <el-input v-model="form.sourceUrl" placeholder="转载来源 URL（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="摘要">
              <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="内容摘要" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="正文">
              <RichEditor
                v-model="form.contentBody"
                module="content"
                register-asset
                asset-ref-type1="content"
                :asset-ref-code="form.contentCode"
                asset-ref-type2="content"
                placeholder="正文支持图文混排，插图自动上传并登记素材仓库"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标签">
              <el-input v-model="form.tags" placeholder="多个标签用逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="置顶">
              <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="推荐">
              <el-switch v-model="form.isRecommend" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="允许评论">
              <el-switch v-model="form.isComment" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="内部备注（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditDialogVisible" title="内容审核" width="520px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="内容标题">
          <span>{{ auditForm.title }}</span>
        </el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio v-for="o in AUDIT_STATUS_OPTIONS" :key="o.value" :value="o.value">
              {{ o.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" placeholder="审核备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditSubmitLoading" @click="handleAuditSubmit">确定</el-button>
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
</style>
