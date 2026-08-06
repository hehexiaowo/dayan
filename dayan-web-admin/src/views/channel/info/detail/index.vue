<script setup lang="ts">
/**
 * 渠道详情页（主从详情页 / tab 式）。
 *
 * 从渠道列表页"详情"按钮进入（携带 channelCode 路由参数）。
 * 顶部展示渠道主信息摘要（全称 + 编码 + 类型 + 状态 + 审核状态）+ 返回按钮；
 * 下方 el-tabs 按子表维度分 6 个 tab，每个 tab 内是该子表的内联 CRUD（自动携带 channelCode）。
 *
 * tab 划分（1 主表 + 3 RBAC 子表 + 配置 tab 内分 3 子区）：
 * - 基本信息：ChannelInfo 主表字段编辑（复用主列表页编辑表单字段集）
 * - 账户：ChannelAccount（分页 + 重置密码，业务键 accountCode 路径）
 * - 角色：ChannelRole（分页 + CRUD，业务键 roleCode 路径；权限分配端点本次不实现）
 * - 开放平台：ChannelOpenPlatform（分页 + CRUD，**id 路径非编码**；appSecret 脱敏）
 * - 分发配置：ChannelConfig content/scene/goods 三类（list+save 全量覆盖，可编辑表格 + 整体保存）
 *
 * 三套主键约定（详见 api/channel-sub.ts 头注释）：
 * - Account/Role：业务码 String 路径
 * - OpenPlatform：自增 id Long 路径
 * - Config：不带主键路径，按 channelCode 整体覆盖
 *
 * 懒加载：所有 el-tab-pane 带 lazy 属性，未访问的 tab 不渲染内容（但标签常驻可见）。
 */
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getChannel } from '@/api/channel'
import {
  CHANNEL_TYPE_OPTIONS,
  CHANNEL_STATUS_OPTIONS,
  CHANNEL_AUDIT_STATUS_OPTIONS
} from '@/types/channel'
import type { ChannelInfo } from '@/types/channel'
import BasicTab from './BasicTab.vue'
import AccountTab from './AccountTab.vue'
import RoleTab from './RoleTab.vue'
import PlatformTab from './PlatformTab.vue'
import ConfigTab from './ConfigTab.vue'

const route = useRoute()
const router = useRouter()
const channelCode = computed(() => route.params.channelCode as string)

const activeTab = ref('basic')
const detailLoading = ref(false)
const channelInfo = ref<ChannelInfo | null>(null)

async function loadDetail() {
  if (!channelCode.value) return
  detailLoading.value = true
  try {
    channelInfo.value = await getChannel(channelCode.value)
  } catch {
    channelInfo.value = null
  } finally {
    detailLoading.value = false
  }
}

loadDetail()

function goBack() {
  router.push({ path: '/channel/info' })
}

/** 渠道类型文本 */
function channelTypeText(v?: number): string {
  const found = CHANNEL_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

/** 渠道状态文本 */
function channelStatusText(v?: number): string {
  const found = CHANNEL_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

/** 审核状态文本 */
function auditStatusText(v?: number): string {
  const found = CHANNEL_AUDIT_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

/** 审核状态 tag 色 */
function auditStatusTagType(v?: number): 'info' | 'success' | 'danger' {
  if (v === 1) return 'success'
  if (v === 2) return 'danger'
  return 'info'
}

const tabs = [
  { name: 'basic', label: '基本信息' },
  { name: 'account', label: '账户' },
  { name: 'role', label: '角色' },
  { name: 'platform', label: '开放平台' },
  { name: 'config', label: '分发配置' }
] as const
</script>

<template>
  <div class="channel-detail" v-loading="detailLoading">
    <!-- 顶部：返回 + 主实体摘要 -->
    <div class="detail-header">
      <el-button :icon="'ArrowLeft'" @click="goBack">返回列表</el-button>
      <div class="channel-summary" v-if="channelInfo">
        <span class="title">{{ channelInfo.fullName }}</span>
        <el-tag size="small" class="ml-8">{{ channelInfo.channelCode }}</el-tag>
        <el-tag size="small" type="info" class="ml-8">
          {{ channelTypeText(channelInfo.channelType) }}
        </el-tag>
        <el-tag size="small" class="ml-8">
          {{ channelStatusText(channelInfo.status) }}
        </el-tag>
        <el-tag size="small" :type="auditStatusTagType(channelInfo.auditStatus)" effect="light" class="ml-8">
          {{ auditStatusText(channelInfo.auditStatus) }}
        </el-tag>
        <span class="meta" v-if="channelInfo.contactPerson">
          · {{ channelInfo.contactPerson }}
        </span>
        <span class="meta" v-if="channelInfo.contactPhone">
          · {{ channelInfo.contactPhone }}
        </span>
      </div>
      <div v-else-if="!detailLoading" class="channel-summary">
        <span class="title">未找到渠道（channelCode={{ channelCode }}）</span>
      </div>
    </div>

    <el-divider />

    <!-- tab 区：5 个 tab，全部 lazy 懒加载 -->
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" lazy>
        <BasicTab v-if="t.name === 'basic'" :channel-code="channelCode" />
        <AccountTab v-else-if="t.name === 'account'" :channel-code="channelCode" />
        <RoleTab v-else-if="t.name === 'role'" :channel-code="channelCode" />
        <PlatformTab v-else-if="t.name === 'platform'" :channel-code="channelCode" />
        <ConfigTab v-else-if="t.name === 'config'" :channel-code="channelCode" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.channel-detail {
  padding: 16px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}
.channel-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.channel-summary .title {
  font-size: 18px;
  font-weight: 600;
}
.channel-summary .meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-left: 4px;
}
.ml-8 {
  margin-left: 4px;
}
</style>
