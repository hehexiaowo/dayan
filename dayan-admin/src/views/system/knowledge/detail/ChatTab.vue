<script setup lang="ts">
/**
 * 知识仓库详情 - 问答测试 Tab。
 *
 * RAG 问答：检索命中片段 → 大模型生成回答，附引用片段；
 * 另提供「仅检索」模式直接查看召回片段。
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { chatKnowledgeRepo, retrieveKnowledgeRepo } from '@/api/knowledge'
import type { KnowledgeChatResult } from '@/types/knowledge'

const props = defineProps<{ repoId: number }>()

const question = ref('')
const asking = ref(false)
const chatResult = ref<KnowledgeChatResult | null>(null)

const retrieveQuery = ref('')
const retrieving = ref(false)
const hits = ref<KnowledgeChatResult['citations']>([])

async function handleAsk() {
  if (!question.value.trim()) return
  asking.value = true
  try {
    chatResult.value = await chatKnowledgeRepo(props.repoId, { question: question.value.trim(), topK: 4 })
  } catch {
    chatResult.value = null
  } finally {
    asking.value = false
  }
}

async function handleRetrieve() {
  if (!retrieveQuery.value.trim()) return
  retrieving.value = true
  try {
    hits.value = await retrieveKnowledgeRepo(props.repoId, { query: retrieveQuery.value.trim(), topK: 5 })
    if (!hits.value.length) ElMessage.info('未检索到相关内容')
  } catch {
    hits.value = []
  } finally {
    retrieving.value = false
  }
}
</script>

<template>
  <div class="chat-tab">
    <el-card shadow="never">
      <template #header>知识库问答（RAG）</template>
      <div class="ask-row">
        <el-input
          v-model="question"
          placeholder="输入问题，如：大雁养老的终身养老权益包含哪些服务？"
          clearable
          @keyup.enter="handleAsk"
        />
        <el-button type="primary" :loading="asking" @click="handleAsk">提问</el-button>
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
</template>

<style scoped lang="scss">
.chat-tab {
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
}
</style>
