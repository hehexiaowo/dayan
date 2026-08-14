<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOpenPlatform } from '@/api/open'
import { DOCK_TYPE_OPTIONS, AUTH_TYPE_OPTIONS } from '@/types/open'
import type { ChannelOpenPlatform } from '@/types/open'
import { statusTagType } from '@/utils/format'

/**
 * 开放平台 - 应用管理页（只读）。
 *
 * 展示本渠道的对接配置（appKey/appSecret 脱敏/回调/IP 白名单等）。
 * 不支持自行修改，配置由运营在 admin 端操作。
 */

const loading = ref(false)
const config = ref<ChannelOpenPlatform | null>(null)

function dockTypeText(v?: number) {
  return DOCK_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}
function authTypeText(v?: number) {
  return AUTH_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getOpenPlatform()
    config.value = res.records[0] || null
  } catch (err) {
    console.warn('[open-app] 加载对接配置失败:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="card-header">
          <span>应用管理</span>
        </div>
      </template>

      <template v-if="config">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="平台名称">{{ config.platformName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="对接类型">{{ dockTypeText(config.dockType) }}</el-descriptions-item>
          <el-descriptions-item label="AppKey">
            <code>{{ config.appKey || '--' }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="AppSecret">
            <code>{{ config.appSecret || '--' }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="认证方式">{{ authTypeText(config.authType) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(config.status)">{{ config.status === 1 ? '启用' : '禁用' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="API 地址" :span="2">{{ config.apiBaseUrl || '--' }}</el-descriptions-item>
          <el-descriptions-item label="回调地址" :span="2">{{ config.callbackUrl || '--' }}</el-descriptions-item>
          <el-descriptions-item label="IP 白名单" :span="2">{{ config.ipWhitelist || '--' }}</el-descriptions-item>
          <el-descriptions-item label="限流(QPS)">{{ config.rateLimit ?? '--' }}</el-descriptions-item>
          <el-descriptions-item label="超时(秒)">{{ config.timeout ?? '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="config.dockType === 1" label="H5 域名" :span="2">{{ config.h5Domain || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="config.dockType === 1" label="H5 主题">{{ config.h5Theme || '--' }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <el-empty v-else-if="!loading" description="暂未配置对接信息，请联系运营开通" />
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
