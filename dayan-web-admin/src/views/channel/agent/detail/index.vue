<script setup lang="ts">
/**
 * 代理人详情页（主从详情页 / tab 式）。
 *
 * 从代理人列表页"详情"按钮进入（携带 agentCode 路由参数）。
 * 顶部展示代理人主信息摘要（姓名 + 编码 + 等级 + 状态 + 手机号 + 保险公司）+ 返回按钮；
 * 下方 el-tabs 按子表维度分 6 个 tab，每个 tab 内是该子表的内联 CRUD（自动携带 agentCode）。
 *
 * tab 划分（1 主表 + 5 子表）：
 * - 基本信息：AgentInfo 主表字段编辑
 * - 账号：AgentAccount（1:1 强约束，主键 agentCode，开通/编辑/重置密码/删除）
 * - 绑定客户：AgentClientRel（by-agent list + bind/unbind，无 update）
 * - 业绩：AgentPerformance（by-agent list + summary 汇总 + 新增，只增不改不删）
 * - 分享记录：AgentShareRecord（by-agent list + 新增，只增不改不删）
 * - 收藏：AgentFavorite（by-agent list + 幂等 add/remove，无 update）
 *
 * 懒加载：所有 el-tab-pane 带 lazy 属性，未访问的 tab 不渲染内容（但标签常驻可见）。
 */
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAgent } from '@/api/agent'
import {
  GENDER_OPTIONS,
  agentLevelLabel,
  agentLevelTagType,
  agentStatusLabel,
  agentStatusTagType
} from '@/types/agent'
import type { AgentInfo } from '@/types/agent'
import BasicTab from './BasicTab.vue'
import AccountTab from './AccountTab.vue'
import ClientTab from './ClientTab.vue'
import PerformanceTab from './PerformanceTab.vue'
import ShareTab from './ShareTab.vue'
import FavoriteTab from './FavoriteTab.vue'

const route = useRoute()
const router = useRouter()
const agentCode = computed(() => route.params.agentCode as string)

const activeTab = ref('basic')
const detailLoading = ref(false)
const agentInfo = ref<AgentInfo | null>(null)

async function loadDetail() {
  if (!agentCode.value) return
  detailLoading.value = true
  try {
    agentInfo.value = await getAgent(agentCode.value)
  } catch {
    agentInfo.value = null
  } finally {
    detailLoading.value = false
  }
}

loadDetail()

function goBack() {
  router.push({ path: '/channel/agent' })
}

/** 性别文本（兼容数字直传） */
function genderText(v?: number): string {
  const found = GENDER_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

const tabs = [
  { name: 'basic', label: '基本信息' },
  { name: 'account', label: '账号' },
  { name: 'client', label: '绑定客户' },
  { name: 'performance', label: '业绩' },
  { name: 'share', label: '分享记录' },
  { name: 'favorite', label: '收藏' }
] as const
</script>

<template>
  <div class="agent-detail" v-loading="detailLoading">
    <!-- 顶部：返回 + 主实体摘要 -->
    <div class="detail-header">
      <el-button :icon="'ArrowLeft'" @click="goBack">返回列表</el-button>
      <div class="agent-summary" v-if="agentInfo">
        <span class="title">{{ agentInfo.fullName }}</span>
        <el-tag size="small" class="ml-8">{{ agentInfo.agentCode }}</el-tag>
        <el-tag size="small" :type="agentLevelTagType(agentInfo.agentLevel)" class="ml-8">
          {{ agentLevelLabel(agentInfo.agentLevel) }}
        </el-tag>
        <el-tag
          size="small"
          :type="agentStatusTagType(agentInfo.status)"
          effect="light"
          class="ml-8"
        >
          {{ agentStatusLabel(agentInfo.status) }}
        </el-tag>
        <span class="meta" v-if="agentInfo.phone">
          {{ agentInfo.phone }}
        </span>
        <span class="meta" v-if="agentInfo.companyName"> · {{ agentInfo.companyName }}</span>
        <span class="meta" v-if="agentInfo.gender != null">
          · {{ genderText(agentInfo.gender) }}
        </span>
      </div>
      <div v-else-if="!detailLoading" class="agent-summary">
        <span class="title">未找到代理人（agentCode={{ agentCode }}）</span>
      </div>
    </div>

    <el-divider />

    <!-- tab 区：6 个 tab，全部 lazy 懒加载 -->
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" lazy>
        <BasicTab v-if="t.name === 'basic'" :agent-code="agentCode" />
        <AccountTab
          v-else-if="t.name === 'account'"
          :agent-code="agentCode"
          :channel-code="agentInfo?.channelCode"
        />
        <ClientTab v-else-if="t.name === 'client'" :agent-code="agentCode" />
        <PerformanceTab v-else-if="t.name === 'performance'" :agent-code="agentCode" />
        <ShareTab v-else-if="t.name === 'share'" :agent-code="agentCode" />
        <FavoriteTab v-else-if="t.name === 'favorite'" :agent-code="agentCode" />
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
  gap: 16px;
}
.agent-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}
.agent-summary .title {
  font-size: 18px;
  font-weight: 600;
}
.agent-summary .meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-left: 8px;
}
.ml-8 {
  margin-left: 8px;
}
</style>
