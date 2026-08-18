<script setup lang="ts">
/**
 * 知识库文档切片弹窗（切片管理）。
 *
 * 按文档查看其在百炼索引中的切片列表（分页实时代理远端）：
 * 展示切片文本内容与相关度；无切片（未入库/解析中）时空态提示。
 *
 * 打开方式：父组件调 {@code open(fileId, fileName)} 显式传参（不依赖 props 异步更新竞态）。
 */
import { ref } from 'vue'
import { listKnowledgeDocChunks } from '@/api/knowledge'
import type { KnowledgeChunk } from '@/types/knowledge'

const props = defineProps<{
  /** 仓库 id */
  repoId: number
}>()

const visible = ref(false)
const loading = ref(false)
const chunks = ref<KnowledgeChunk[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 20
const fileId = ref('')
const fileName = ref('')

async function loadChunks() {
  if (!fileId.value) return
  loading.value = true
  try {
    const res = await listKnowledgeDocChunks(props.repoId, fileId.value, {
      pageNum: pageNum.value,
      pageSize
    })
    chunks.value = res.chunks ?? []
    total.value = res.total ?? 0
  } catch {
    chunks.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function open(fid: string, fname: string) {
  fileId.value = fid
  fileName.value = fname
  pageNum.value = 1
  visible.value = true
  loadChunks()
}

function handlePageChange(p: number) {
  pageNum.value = p
  loadChunks()
}

function formatScore(score?: number): string {
  if (score === undefined || score === null) return '--'
  return `${(score * 100).toFixed(1)}%`
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" :title="`切片管理 - ${fileName}`" width="760px" top="8vh" :close-on-click-modal="false">
    <div v-loading="loading" class="chunk-dialog">
      <el-alert type="info" :closable="false" class="chunk-alert">
        切片为文档解析后按语义切分的内容单元，问答检索命中即在这些切片中召回。
      </el-alert>
      <div v-if="chunks.length" class="chunk-list">
        <div v-for="(c, i) in chunks" :key="i" class="chunk-item">
          <div class="chunk-head">
            <span class="chunk-index">切片 {{ (pageNum - 1) * pageSize + i + 1 }}</span>
            <span v-if="c.score !== undefined && c.score !== null" class="chunk-score">
              相关度 {{ formatScore(c.score) }}
            </span>
          </div>
          <div class="chunk-text">{{ c.text }}</div>
        </div>
      </div>
      <el-empty v-else-if="!loading" description="暂无切片（文档可能尚未入库）" :image-size="70" />
      <div v-if="total > pageSize" class="chunk-pagination">
        <el-pagination
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
.chunk-dialog {
  .chunk-alert {
    margin-bottom: 12px;
  }
  .chunk-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    max-height: 55vh;
    overflow-y: auto;
    padding-right: 4px;

    .chunk-item {
      border: 1px solid #e4e7ed;
      border-radius: 6px;
      padding: 10px 12px;
      background: #fafafa;

      .chunk-head {
        display: flex;
        gap: 12px;
        align-items: center;
        margin-bottom: 6px;

        .chunk-index {
          font-size: 12px;
          color: #409eff;
          font-weight: 600;
        }
        .chunk-score {
          font-size: 12px;
          color: #909399;
        }
      }
      .chunk-text {
        font-size: 13px;
        color: #303133;
        line-height: 1.7;
        white-space: pre-wrap;
        word-break: break-all;
      }
    }
  }
  .chunk-pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 12px;
  }
}
</style>
