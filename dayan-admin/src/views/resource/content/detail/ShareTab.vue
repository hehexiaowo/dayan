<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageContentRecordShare,
  createContentRecordShare,
  updateContentRecordShare,
  deleteContentRecordShare
} from '@/api/content-sub'
import type { ContentRecordShare, ContentRecordShareQuery } from '@/types/content'
import { ShareChannel, SHARE_CHANNEL_OPTIONS } from '@/types/content'
import { formatDateTime } from '@/utils/format'

/** 分享者类型（对齐 DDL sharer_type：agent/client/butler） */
const SHARER_TYPE_OPTIONS = [
  { label: '代理人', value: 'agent' },
  { label: '客户', value: 'client' },
  { label: '管家', value: 'butler' }
] as const

/**
 * 分享记录 tab（按 contentCode 分组）。
 * 注意：后端 update 仅回填 clickCount/convertCount，其余字段编辑后会被忽略。
 */
const props = defineProps<{ contentCode: string }>()

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<ContentRecordShare, ContentRecordShareQuery, number>(
  {
    page: pageContentRecordShare,
    create: createContentRecordShare,
    update: (id, data) => updateContentRecordShare(id, data),
    remove: deleteContentRecordShare
  },
  {
    initialQuery: { shareChannel: undefined },
    idKey: 'id',
    fixedParams: { contentCode: props.contentCode }
  }
)

const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ContentRecordShare>({
  contentCode: props.contentCode,
  shareChannel: ShareChannel.WECHAT,
  sharerCode: '',
  sharerType: '',
  shareUrl: '',
  shareTitle: '',
  shareDescription: '',
  shareImage: '',
  clickCount: 0,
  convertCount: 0,
  shareTime: ''
})

function resetForm() {
  Object.assign(form, {
    id: undefined,
    contentCode: props.contentCode,
    shareChannel: ShareChannel.WECHAT,
    sharerCode: '',
    sharerType: '',
    shareUrl: '',
    shareTitle: '',
    shareDescription: '',
    shareImage: '',
    clickCount: 0,
    convertCount: 0,
    shareTime: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ContentRecordShare) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  submitLoading.value = true
  try {
    if (dialogType.value === 'create') {
      // shareTime 为空时置 undefined（后端默认当前时间），避免空串反序列化 400
      if (!form.shareTime) form.shareTime = undefined
      await createContentRecordShare(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateContentRecordShare(form.id, { clickCount: form.clickCount, convertCount: form.convertCount })
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ContentRecordShare) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该分享记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteContentRecordShare(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

function channelLabel(c?: number) {
  return SHARE_CHANNEL_OPTIONS.find((o) => o.value === c)?.label ?? '-'
}

loadPage()
</script>

<template>
  <div>
    <div class="toolbar">
      <el-select v-model="query.shareChannel" placeholder="分享渠道" clearable style="width: 160px" @change="handleSearch">
        <el-option v-for="o in SHARE_CHANNEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增分享记录</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="sharerCode" label="分享者编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="shareChannel" label="渠道" width="110" align="center">
        <template #default="{ row }">
          <el-tag>{{ channelLabel(row.shareChannel) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="shareTitle" label="分享标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="clickCount" label="点击数" width="90" align="right" />
      <el-table-column prop="convertCount" label="转化数" width="90" align="right" />
      <el-table-column prop="shareTime" label="分享时间" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ formatDateTime(row.shareTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: flex-end; margin-top: 12px">
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增分享记录' : '编辑分享记录'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-alert
          v-if="dialogType === 'edit'"
          type="info"
          :closable="false"
          title="编辑仅回填点击数/转化数，其余字段不可改"
          style="margin-bottom: 12px"
        />
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分享渠道">
              <el-select v-model="form.shareChannel" :disabled="dialogType === 'edit'" style="width: 100%">
                <el-option v-for="o in SHARE_CHANNEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分享人类型">
              <el-select v-model="form.sharerType" :disabled="dialogType === 'edit'" clearable placeholder="选择分享人类型" style="width: 100%">
                <el-option v-for="o in SHARER_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分享者编码">
              <el-input v-model="form.sharerCode" :disabled="dialogType === 'edit'" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="分享标题">
              <el-input v-model="form.shareTitle" :disabled="dialogType === 'edit'" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="分享链接">
              <el-input v-model="form.shareUrl" :disabled="dialogType === 'edit'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="点击数" v-if="dialogType === 'edit'">
              <el-input-number v-model="form.clickCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="转化数" v-if="dialogType === 'edit'">
              <el-input-number v-model="form.convertCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
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
</style>
