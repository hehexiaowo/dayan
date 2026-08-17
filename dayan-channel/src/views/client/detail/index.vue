<script setup lang="ts">
/**
 * 客户详情页（tab 式主从详情，路由 /client/detail/:clientCode）。
 *
 * 从客户账号列表页"详情"按钮进入（携带 clientCode 路由参数）。
 * 顶部为返回按钮 + 客户摘要（姓名 + 编码 + 手机号 + 性别）；
 * 下方 el-tabs 分 3 个 tab（基本信息 / 激活记录 / 服务记录），全部 lazy 懒加载、只读。
 *
 * 摘要数据源 getClient（BasicTab 内部会再次拉全量字段，各自独立降级）。
 * 渠道视角 Client 为后端 ClientInfoVO 子集（无等级/状态字段），摘要不展示等级/状态 tag。
 * 后端端点未实现时降级：摘要区提示未找到，tab 内空列表。
 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getClient } from '@/api/client'
import { GENDER_OPTIONS, type Client } from '@/types/client'
import BasicTab from './BasicTab.vue'
import ActivationTab from './ActivationTab.vue'
import ServiceTab from './ServiceTab.vue'

const route = useRoute()
const router = useRouter()

/** 客户编码（路由参数） */
const clientCode = computed(() => (route.params.clientCode as string) || '')

const activeTab = ref('basic')
const detailLoading = ref(false)
const client = ref<Client | null>(null)

async function loadSummary() {
  if (!clientCode.value) return
  detailLoading.value = true
  try {
    client.value = await getClient(clientCode.value)
  } catch {
    client.value = null
  } finally {
    detailLoading.value = false
  }
}

loadSummary()

/** 返回上一页（列表页） */
function goBack() {
  router.back()
}

/** 性别文本（1 男 / 2 女 / 0 未知） */
function genderText(v?: number): string {
  return GENDER_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}
</script>

<template>
  <div v-loading="detailLoading" class="client-detail">
    <!-- 顶部：返回 + 客户摘要 -->
    <div class="detail-header">
      <el-button :icon="'ArrowLeft'" @click="goBack">返回列表</el-button>
      <div v-if="client" class="summary">
        <span class="title">{{ client.fullName || client.clientCode }}</span>
        <el-tag size="small">{{ client.clientCode }}</el-tag>
        <span v-if="client.phone" class="meta">{{ client.phone }}</span>
        <span v-if="client.gender != null" class="meta"> · {{ genderText(client.gender) }}</span>
      </div>
      <div v-else-if="!detailLoading" class="summary">
        <span class="title">未找到客户（clientCode={{ clientCode }}）</span>
      </div>
    </div>

    <el-divider />

    <!-- tab 区：3 个 tab，全部 lazy 懒加载 -->
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="基本信息" name="basic" lazy>
        <BasicTab :client-code="clientCode" />
      </el-tab-pane>
      <el-tab-pane label="激活记录" name="activations" lazy>
        <ActivationTab :client-code="clientCode" />
      </el-tab-pane>
      <el-tab-pane label="服务记录" name="services" lazy>
        <ServiceTab :client-code="clientCode" />
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
