<script setup lang="ts">
/**
 * 客户详情页（主从详情页 / tab 式）。
 *
 * 从客户列表页"详情"按钮进入（携带 clientCode 路由参数）。
 * 顶部展示客户主信息摘要（姓名 + 编码 + 等级 + 状态 + 手机号/年龄等）+ 返回按钮；
 * 下方 el-tabs 按子表维度分 7 个 tab，每个 tab 内是该子表的内联 CRUD（自动携带 clientCode）。
 *
 * tab 划分（对应 P9.2 计划，1 主表 + 6 子表）：
 * - 基本信息：ClientInfo 主表字段编辑（复用主列表页编辑表单字段集）
 * - 账号：ClientAccount（分页 + 重置密码，主键 clientCode）
 * - 家庭成员：ClientFamilyMember（by-client list + CRUD）
 * - 收货地址：ClientAddress（by-client list + CRUD + 设为默认）
 * - 健康档案：ClientHealthProfile（一客户一档案，单条 upsert）
 * - 照护需求：ClientCareNeed（分页 + CRUD）
 * - 收藏：ClientFavorite（by-client list + 新增/删除，无编辑）
 *
 * 懒加载：所有 el-tab-pane 带 lazy 属性，未访问的 tab 不渲染内容（但标签常驻可见）。
 */
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getClient } from '@/api/client'
import {
  GENDER_OPTIONS,
  clientLevelLabel,
  clientLevelTagType,
  clientStatusLabel,
  clientStatusTagType
} from '@/types/client'
import type { ClientInfo } from '@/types/client'
import BasicTab from './BasicTab.vue'
import AccountTab from './AccountTab.vue'
import FamilyTab from './FamilyTab.vue'
import AddressTab from './AddressTab.vue'
import HealthTab from './HealthTab.vue'
import CareTab from './CareTab.vue'
import FavoriteTab from './FavoriteTab.vue'

const route = useRoute()
const router = useRouter()
const clientCode = computed(() => route.params.clientCode as string)

const activeTab = ref('basic')
const detailLoading = ref(false)
const clientInfo = ref<ClientInfo | null>(null)

async function loadDetail() {
  if (!clientCode.value) return
  detailLoading.value = true
  try {
    clientInfo.value = await getClient(clientCode.value)
  } catch {
    clientInfo.value = null
  } finally {
    detailLoading.value = false
  }
}

loadDetail()

function goBack() {
  router.push({ path: '/channel/client' })
}

/** 性别文本（兼容数字直传） */
function genderText(v?: number): string {
  const found = GENDER_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

const tabs = [
  { name: 'basic', label: '基本信息' },
  { name: 'account', label: '账号' },
  { name: 'family', label: '家庭成员' },
  { name: 'address', label: '收货地址' },
  { name: 'health', label: '健康档案' },
  { name: 'care', label: '照护需求' },
  { name: 'favorite', label: '收藏' }
] as const
</script>

<template>
  <div v-loading="detailLoading" class="client-detail">
    <!-- 顶部：返回 + 主实体摘要 -->
    <div class="detail-header">
      <el-button :icon="'ArrowLeft'" @click="goBack">返回列表</el-button>
      <div v-if="clientInfo" class="client-summary">
        <span class="title">{{ clientInfo.fullName }}</span>
        <el-tag size="small" class="ml-8">{{ clientInfo.clientCode }}</el-tag>
        <el-tag size="small" :type="clientLevelTagType(clientInfo.clientLevel)" class="ml-8">
          {{ clientLevelLabel(clientInfo.clientLevel) }}
        </el-tag>
        <el-tag
          size="small"
          :type="clientStatusTagType(clientInfo.status)"
          effect="light"
          class="ml-8"
        >
          {{ clientStatusLabel(clientInfo.status) }}
        </el-tag>
        <span v-if="clientInfo.phone" class="meta">
          {{ clientInfo.phone }}
        </span>
        <span v-if="clientInfo.age != null" class="meta"> · {{ clientInfo.age }}岁</span>
        <span v-if="clientInfo.gender != null" class="meta"> · {{ genderText(clientInfo.gender) }}</span>
      </div>
      <div v-else-if="!detailLoading" class="client-summary">
        <span class="title">未找到客户（clientCode={{ clientCode }}）</span>
      </div>
    </div>

    <el-divider />

    <!-- tab 区：7 个 tab，全部 lazy 懒加载 -->
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" lazy>
        <BasicTab v-if="t.name === 'basic'" :client-code="clientCode" />
        <AccountTab
          v-else-if="t.name === 'account'"
          :client-code="clientCode"
          :channel-code="clientInfo?.channelCode"
        />
        <FamilyTab v-else-if="t.name === 'family'" :client-code="clientCode" />
        <AddressTab v-else-if="t.name === 'address'" :client-code="clientCode" />
        <HealthTab v-else-if="t.name === 'health'" :client-code="clientCode" />
        <CareTab v-else-if="t.name === 'care'" :client-code="clientCode" />
        <FavoriteTab v-else-if="t.name === 'favorite'" :client-code="clientCode" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.client-detail {
  padding: 16px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}
.client-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}
.client-summary .title {
  font-size: 18px;
  font-weight: 600;
}
.client-summary .meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-left: 8px;
}
.ml-8 {
  margin-left: 8px;
}
</style>
