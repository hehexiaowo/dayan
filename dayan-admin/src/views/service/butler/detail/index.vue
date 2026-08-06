<script setup lang="ts">
/**
 * 管家详情页（主从详情页 / tab 式）。
 *
 * 从管家列表页"详情"按钮进入（携带 butlerCode 路由参数）。
 * 顶部展示管家主信息摘要（姓名 + 编码 + 等级 + 状态 + 手机号）+ 返回按钮；
 * 下方 el-tabs 按子表维度分 6 个 tab，每个 tab 内是该子表的内联 CRUD（自动携带 butlerCode）。
 *
 * tab 划分（对应 P9.2 brief，1 主表 + 5 子表）：
 * - 基本信息：ButlerInfo 主表字段编辑（el-descriptions + 编辑弹窗）
 * - 账号：ButlerAccount（分页 + CRUD + 重置密码，主键 number id）
 * - 技能：ButlerSkill（分页 + CRUD，主键 number id）
 * - 服务客户：ButlerClientRel（list + bind/unbind/delete，无 update，主键雪花 string）
 * - 服务记录：ButlerServiceRecord（分页 + CRUD，主键雪花 string）
 * - 评价：ButlerRating（分页 + CRUD + el-rate，主键雪花 string）
 *
 * 排班 tab 不做（scheduleType DDL/VO 冲突待后端确认）；
 * 角色权限 tab 不做（account-role-rel 是 P5 半成品）。
 *
 * 懒加载：所有 el-tab-pane 带 lazy 属性，未访问的 tab 不渲染内容（但标签常驻可见）。
 */
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getButler } from '@/api/service'
import {
  butlerLevelLabel,
  butlerLevelTagType,
  butlerStatusLabel,
  butlerStatusTagType
} from '@/types/service'
import type { ButlerInfo } from '@/types/service'
import BasicTab from './BasicTab.vue'
import AccountTab from './AccountTab.vue'
import SkillTab from './SkillTab.vue'
import ClientRelTab from './ClientRelTab.vue'
import RecordTab from './RecordTab.vue'
import RatingTab from './RatingTab.vue'

const route = useRoute()
const router = useRouter()
const butlerCode = computed(() => route.params.butlerCode as string)

const activeTab = ref('basic')
const detailLoading = ref(false)
const butlerInfo = ref<ButlerInfo | null>(null)

async function loadDetail() {
  if (!butlerCode.value) return
  detailLoading.value = true
  try {
    butlerInfo.value = await getButler(butlerCode.value)
  } catch {
    butlerInfo.value = null
  } finally {
    detailLoading.value = false
  }
}

loadDetail()

function goBack() {
  router.push({ path: '/service/butler' })
}

const tabs = [
  { name: 'basic', label: '基本信息' },
  { name: 'account', label: '账号' },
  { name: 'skill', label: '技能' },
  { name: 'client', label: '服务客户' },
  { name: 'record', label: '服务记录' },
  { name: 'rating', label: '评价' }
] as const
</script>

<template>
  <div class="butler-detail" v-loading="detailLoading">
    <!-- 顶部：返回 + 主实体摘要 -->
    <div class="detail-header">
      <el-button :icon="'ArrowLeft'" @click="goBack">返回列表</el-button>
      <div class="butler-summary" v-if="butlerInfo">
        <span class="title">{{ butlerInfo.fullName }}</span>
        <el-tag size="small" class="ml-8">{{ butlerInfo.butlerCode }}</el-tag>
        <el-tag size="small" :type="butlerLevelTagType(butlerInfo.butlerLevel)" class="ml-8">
          {{ butlerLevelLabel(butlerInfo.butlerLevel) }}
        </el-tag>
        <el-tag
          size="small"
          :type="butlerStatusTagType(butlerInfo.status)"
          effect="light"
          class="ml-8"
        >
          {{ butlerStatusLabel(butlerInfo.status) }}
        </el-tag>
        <span class="meta" v-if="butlerInfo.phone">{{ butlerInfo.phone }}</span>
      </div>
      <div v-else-if="!detailLoading" class="butler-summary">
        <span class="title">未找到管家（butlerCode={{ butlerCode }}）</span>
      </div>
    </div>

    <el-divider />

    <!-- tab 区：6 个 tab，全部 lazy 懒加载 -->
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" lazy>
        <BasicTab v-if="t.name === 'basic'" :butler-code="butlerCode" />
        <AccountTab v-else-if="t.name === 'account'" :butler-code="butlerCode" />
        <SkillTab v-else-if="t.name === 'skill'" :butler-code="butlerCode" />
        <ClientRelTab v-else-if="t.name === 'client'" :butler-code="butlerCode" />
        <RecordTab v-else-if="t.name === 'record'" :butler-code="butlerCode" />
        <RatingTab v-else-if="t.name === 'rating'" :butler-code="butlerCode" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.butler-detail {
  padding: 16px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}
.butler-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}
.butler-summary .title {
  font-size: 18px;
  font-weight: 600;
}
.butler-summary .meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-left: 8px;
}
.ml-8 {
  margin-left: 8px;
}
</style>
