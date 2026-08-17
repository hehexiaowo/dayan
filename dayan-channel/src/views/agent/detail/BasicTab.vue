<script setup lang="ts">
/**
 * 代理人详情页 - 基本信息 tab（只读）。
 *
 * 调 getAgentAccount(agentCode) 拉取账号 + 代理人信息，
 * el-descriptions :column="2" border 全字段只读展示
 * （账号编码/渠道/用户名/姓名/手机/等级/认证/状态/最后登录/头像/创建/更新时间）。
 * 接口失败降级为空态提示。
 */
import { ref } from 'vue'
import { getAgentAccount } from '@/api/agent'
import { AGENT_LEVEL_OPTIONS, type AgentAccount } from '@/types/agent'
import { formatDateTime, statusTagType } from '@/utils/format'

const props = defineProps<{
  /** 代理人编码（路由参数） */
  agentCode: string
}>()

const loading = ref(false)
const account = ref<AgentAccount | null>(null)

async function loadDetail() {
  if (!props.agentCode) return
  loading.value = true
  try {
    account.value = await getAgentAccount(props.agentCode)
  } catch {
    account.value = null
  } finally {
    loading.value = false
  }
}

loadDetail()

/** 代理人等级文本（1-4 → 普通/银牌/金牌/钻石） */
function agentLevelText(v?: number): string {
  return AGENT_LEVEL_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}
</script>

<template>
  <div v-loading="loading">
    <el-descriptions v-if="account" :column="2" border>
      <el-descriptions-item label="账号编码">{{ account.agentCode || '--' }}</el-descriptions-item>
      <el-descriptions-item label="所属渠道">{{ account.channelCode || '--' }}</el-descriptions-item>
      <el-descriptions-item label="用户名">{{ account.username || '--' }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ account.realName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ account.phone || '--' }}</el-descriptions-item>
      <el-descriptions-item label="代理人等级">{{ agentLevelText(account.agentLevel) }}</el-descriptions-item>
      <el-descriptions-item label="认证状态">
        <el-tag v-if="account.isCertified != null" :type="account.isCertified === 1 ? 'success' : 'info'">
          {{ account.isCertified === 1 ? '已认证' : '未认证' }}
        </el-tag>
        <span v-else>--</span>
      </el-descriptions-item>
      <el-descriptions-item label="账号状态">
        <el-tag v-if="account.accountStatus != null" :type="statusTagType(account.accountStatus)">
          {{ account.accountStatus === 1 ? '正常' : '禁用' }}
        </el-tag>
        <span v-else>--</span>
      </el-descriptions-item>
      <el-descriptions-item label="最后登录时间">{{ formatDateTime(account.lastLoginTime) }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatDateTime(account.createdAt) }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ formatDateTime(account.updatedAt) }}</el-descriptions-item>
      <el-descriptions-item label="头像" :span="2">{{ account.avatar || '--' }}</el-descriptions-item>
    </el-descriptions>
    <el-empty v-else-if="!loading" description="未加载到代理人信息" />
  </div>
</template>
