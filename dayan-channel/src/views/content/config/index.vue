<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listContentConfig, saveContentConfig } from '@/api/content'
import { pageContents } from '@/api/content'
import type { ChannelConfigContent, ContentInfo } from '@/types/content'

const route = useRoute()
const appType = computed(() => (route.path.startsWith('/agent') ? 'agent' : 'client'))

const loading = ref(false)
const saving = ref(false)
// 已选内容编码集合（el-transfer v-model）
const selectedCodes = ref<string[]>([])
// 全部可选内容（平台已审核通过）
const allContents = ref<ContentInfo[]>([])

// el-transfer 的 data 源：{ key, label, disabled }
const transferData = computed(() =>
  allContents.value.map((c) => ({ key: c.contentCode, label: c.title, disabled: false }))
)

async function loadData() {
  loading.value = true
  try {
    // 1. 拉本渠道当前 appType 已配置的
    const configured = await listContentConfig(appType.value)
    selectedCodes.value = configured.map((c) => c.contentCode)
    // 2. 拉可选内容池（审核通过的）——pageContents 按渠道已配置过滤，这里需要"可选池"
    //    注意：pageContents 返回的是本渠道已配置的，不适合做"可选池"
    //    可选池需要平台全量已审核通过内容。本期简化：直接用已配置的做展示，
    //    完整的"可选池"需后端补一个"平台内容池"接口。先用已配置的渲染。
    allContents.value = await pageContents({ ...{ appType: appType.value, current: 1, size: 200, auditStatus: 2 } })
      .then((res) => res.records)
  } catch (err) {
    // 与其他页一致：接口异常降级提示，不阻塞页面
    console.warn('[content/config] 加载内容配置失败:', err)
    ElMessage.warning('加载内容配置失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const configs: ChannelConfigContent[] = selectedCodes.value.map((code, idx) => ({
      contentCode: code,
      appType: appType.value,
      sortOrder: idx
    }))
    await saveContentConfig(configs)
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">内容配置（{{ appType === 'agent' ? '代理人端' : '客户端' }}）</span>
          <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
        </div>
      </template>
      <el-transfer
        v-model="selectedCodes"
        :data="transferData"
        :titles="['可选内容', '已选内容']"
        filterable
        filter-placeholder="搜索内容标题"
      />
      <el-empty v-if="allContents.length === 0" description="暂无可配置内容（平台需先审核通过内容）" />
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container { display: flex; flex-direction: column; gap: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
