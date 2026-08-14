<script setup lang="ts">
/**
 * 服务会话详情页（主从详情页 / tab 式）。
 *
 * 从会话列表页"详情"按钮进入（携带 sessionCode 路由参数）。
 * 顶部展示会话主信息摘要（服务标题 + 编码 + 服务类型 + 会话状态标签）+ 返回按钮；
 * 下方 el-tabs 按业务链维度分 6 个 tab，对应服务会话全生命周期：
 * 基本信息 → 评价 → 需求 → 方案 → 安排 → 回访。
 *
 * tab 划分（1 主表 + 5 子表）：
 * - 基本信息：ServiceSession 主表字段编辑（复用 updateSession，不含状态机）
 * - 评价：ServiceEvaluation（1:1 一会话一评价，前端「有则编辑无则新增」判断）
 * - 需求：ServiceEquityDemand（业务链起点，demandCode DM 前缀）
 * - 方案：ServiceEquitySolution（关联 demand，含 /accept 标记端点，方案被接受是 confirm_solution 前提）
 * - 安排：ServiceEquityArrange（关联 solution，含 /confirm 端点，确认后才能 start_service）
 * - 回访：ServiceEquityFollowup（关联 arrange，4 维满意度，服务端自动算 isFollowupNeeded）
 *
 * 主键约定（全部 id 雪花 Long，路径参数用 id，useCrud 传 idKey:'id'）：
 * - Evaluation：create 返回 Long（id）
 * - Demand/Solution/Arrange/Followup：create 返回 String（业务 code）
 *
 * 懒加载：所有 el-tab-pane 带 lazy 属性，未访问的 tab 不渲染内容（但标签常驻可见）。
 */
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSession } from '@/api/service'
import {
  SessionStatus,
  SESSION_STATUS_OPTIONS,
  SERVICE_TYPE_OPTIONS
} from '@/types/service'
import type { ServiceSession } from '@/types/service'
import BasicTab from './BasicTab.vue'
import EvaluationTab from './EvaluationTab.vue'
import DemandTab from './DemandTab.vue'
import SolutionTab from './SolutionTab.vue'
import ArrangeTab from './ArrangeTab.vue'
import FollowupTab from './FollowupTab.vue'

const route = useRoute()
const router = useRouter()
const sessionCode = computed(() => route.params.sessionCode as string)

const activeTab = ref('basic')
const detailLoading = ref(false)
const sessionInfo = ref<ServiceSession | null>(null)

async function loadDetail() {
  if (!sessionCode.value) return
  detailLoading.value = true
  try {
    sessionInfo.value = await getSession(sessionCode.value)
  } catch {
    sessionInfo.value = null
  } finally {
    detailLoading.value = false
  }
}

loadDetail()

function goBack() {
  router.push({ path: '/service/session' })
}

/** 服务类型文本 */
function serviceTypeLabel(t?: number): string {
  const found = SERVICE_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : '--'
}

/** 会话状态文本 */
function sessionStatusLabel(s?: number): string {
  const found = SESSION_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : '--'
}

/** 会话状态 el-tag type：完成success / 进行中(服务中/需求/方案)primary / 待办warning / 取消info。 */
function sessionStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (status) {
    case SessionStatus.COMPLETED:
      return 'success'
    case SessionStatus.ACCEPTED:
    case SessionStatus.IN_SERVICE:
      return 'warning'
    case SessionStatus.DEMAND_SUBMITTED:
    case SessionStatus.SOLUTION_CONFIRMED:
      return 'primary'
    case SessionStatus.CANCELLED:
    default:
      return 'info'
  }
}

const tabs = [
  { name: 'basic', label: '基本信息' },
  { name: 'evaluation', label: '服务评价' },
  { name: 'demand', label: '权益需求' },
  { name: 'solution', label: '权益方案' },
  { name: 'arrange', label: '全程安排' },
  { name: 'followup', label: '回访品控' }
] as const
</script>

<template>
  <div v-loading="detailLoading" class="session-detail">
    <!-- 顶部：返回 + 主实体摘要 -->
    <div class="detail-header">
      <el-button :icon="'ArrowLeft'" @click="goBack">返回列表</el-button>
      <div v-if="sessionInfo" class="session-summary">
        <span class="title">{{ sessionInfo.serviceTitle || '服务会话' }}</span>
        <el-tag size="small" class="ml-8">{{ sessionInfo.sessionCode }}</el-tag>
        <el-tag size="small" type="info" class="ml-8">
          {{ serviceTypeLabel(sessionInfo.serviceType) }}
        </el-tag>
        <el-tag size="small" :type="sessionStatusTagType(sessionInfo.sessionStatus)" class="ml-8">
          {{ sessionStatusLabel(sessionInfo.sessionStatus) }}
        </el-tag>
        <span v-if="sessionInfo.clientCode" class="meta"> · 客户：{{ sessionInfo.clientCode }}</span>
        <span v-if="sessionInfo.butlerFullName || sessionInfo.butlerCode" class="meta">
          · 管家：{{ sessionInfo.butlerFullName || sessionInfo.butlerCode }}
        </span>
      </div>
      <div v-else-if="!detailLoading" class="session-summary">
        <span class="title">未找到会话（sessionCode={{ sessionCode }}）</span>
      </div>
    </div>

    <el-divider />

    <!-- tab 区：6 个 tab，全部 lazy 懒加载 -->
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" lazy>
        <BasicTab v-if="t.name === 'basic'" :session-code="sessionCode" />
        <EvaluationTab v-else-if="t.name === 'evaluation'" :session-code="sessionCode" />
        <DemandTab v-else-if="t.name === 'demand'" :session-code="sessionCode" />
        <SolutionTab v-else-if="t.name === 'solution'" :session-code="sessionCode" />
        <ArrangeTab v-else-if="t.name === 'arrange'" :session-code="sessionCode" />
        <FollowupTab v-else-if="t.name === 'followup'" :session-code="sessionCode" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.session-detail {
  padding: 16px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}
.session-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.session-summary .title {
  font-size: 18px;
  font-weight: 600;
}
.session-summary .meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-left: 4px;
}
.ml-8 {
  margin-left: 4px;
}
</style>
