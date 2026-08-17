<script setup lang="ts">
/**
 * 客户详情页 - 基本信息 tab（只读）。
 *
 * 调 getClient(clientCode) 拉取客户信息（渠道视角 ClientInfoVO 子集），
 * el-descriptions :column="2" border 全字段只读展示
 * （ID/客户编码/姓名/手机号/性别/所属渠道）。
 * 接口失败降级为空态提示。
 */
import { ref } from 'vue'
import { getClient } from '@/api/client'
import { GENDER_OPTIONS, type Client } from '@/types/client'

const props = defineProps<{
  /** 客户编码（路由参数） */
  clientCode: string
}>()

const loading = ref(false)
const client = ref<Client | null>(null)

async function loadDetail() {
  if (!props.clientCode) return
  loading.value = true
  try {
    client.value = await getClient(props.clientCode)
  } catch {
    client.value = null
  } finally {
    loading.value = false
  }
}

loadDetail()

/** 性别文本（1 男 / 2 女 / 0 未知） */
function genderText(v?: number): string {
  return GENDER_OPTIONS.find((o) => o.value === v)?.label ?? '--'
}
</script>

<template>
  <div v-loading="loading">
    <el-descriptions v-if="client" :column="2" border>
      <el-descriptions-item label="ID">{{ client.id ?? '--' }}</el-descriptions-item>
      <el-descriptions-item label="客户编码">{{ client.clientCode || '--' }}</el-descriptions-item>
      <el-descriptions-item label="客户姓名">{{ client.fullName || '--' }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ client.phone || '--' }}</el-descriptions-item>
      <el-descriptions-item label="性别">{{ genderText(client.gender) }}</el-descriptions-item>
      <el-descriptions-item label="所属渠道">{{ client.channelCode || '--' }}</el-descriptions-item>
    </el-descriptions>
    <el-empty v-else-if="!loading" description="未加载到客户信息" />
  </div>
</template>
