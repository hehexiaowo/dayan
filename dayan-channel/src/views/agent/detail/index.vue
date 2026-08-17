<script setup lang="ts">
/**
 * 代理人详情页（tab 式主从详情，路由 /agent/detail/:agentCode）。
 *
 * 从代理人账号列表页"详情"按钮进入（携带 agentCode 路由参数）。
 * 顶部为返回按钮 + 账号摘要（姓名 + 编码 + 等级 + 认证 + 状态 + 手机号）；
 * 下方 el-tabs 分 3 个 tab（基本信息 / 客户绑定 / 分享记录），全部 lazy 懒加载。
 *
 * 摘要数据源 getAgentAccount（BasicTab 内部会再次拉全量字段，各自独立降级）。
 * 后端端点未实现时降级：摘要区提示未找到，tab 内空列表。
 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAgentAccount } from '@/api/agent'
import { AGENT_LEVEL_OPTIONS, type AgentAccount } from '@/types/agent'
import { statusTagType } from '@/utils/format'
import BasicTab from './BasicTab.vue'
import ClientTab from './ClientTab.vue'
import ShareTab from './ShareTab.vue'

const route = useRoute()
const router = useRouter()

/** 代理人编码（路由参数） */
const agentCode = computed(() => (route.params.agentCode as string) || '')

const activeTab = ref('basic')
const detailLoading = ref(false)
const account = ref<AgentAccount | null>(null)

async function loadSummary() {
  if (!agentCode.value) return
  detailLoading.value = true
  try {
    account.value = await getAgentAccount(agentCode.value)
  } catch {
    account.value = null
  } finally {
    detailLoading.value = false
  }
}

loadSummary()

/** 返回代理人账号列表 */
function goBack() {
  router.push({ path: '/agent/account' })
}

/** 代理人等级文本（1-4 → 普通/银牌/金牌/钻石） */
function agentLevelText(v?: number): string {
  return AGENT_LEVEL_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}
</script>

<template>
  <div v-loading="detailLoading" class="agent-detail">
    <!-- 顶部：返回 + 账号摘要 -->
    <div class="detail-header">
      <el-button :icon="'ArrowLeft'" @click="goBack">返回列表</el-button>
      <div v-if="account" class="summary">
        <span class="title">{{ account.realName || account.username }}</span>
        <el-tag size="small">{{ account.agentCode }}</el-tag>
        <el-tag size="small">{{ agentLevelText(account.agentLevel) }}</el-tag>
        <el-tag v-if="account.isCertified != null" size="small" :type="account.isCertified === 1 ? 'success' : 'info'">
          {{ account.isCertified === 1 ? '已认证' : '未认证' }}
        </el-tag>
        <el-tag
          v-if="account.accountStatus != null"
          size="small"
          :type="statusTagType(account.accountStatus)"
        >
          {{ account.accountStatus === 1 ? '正常' : '禁用' }}
        </el-tag>
        <span v-if="account.phone" class="meta">{{ account.phone }}</span>
      </div>
      <div v-else-if="!detailLoading" class="summary">
        <span class="title">未找到代理人（agentCode={{ agentCode }}）</span>
      </div>
    </div>

    <el-divider />

    <!-- tab 区：3 个 tab，全部 lazy 懒加载 -->
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="基本信息" name="basic" lazy>
        <BasicTab :agent-code="agentCode" />
      </el-tab-pane>
      <el-tab-pane label="客户绑定" name="clients" lazy>
        <ClientTab :agent-code="agentCode" />
      </el-tab-pane>
      <el-tab-pane label="分享记录" name="shares" lazy>
        <ShareTab :agent-code="agentCode" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.agent-detail {
  padding: 16px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.summary {
  display: flex;
  align-items: center;
  gap: 8px;
}
.summary .title {
  font-size: 16px;
  font-weight: 600;
}
.summary .meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-left: 4px;
}
</style>
