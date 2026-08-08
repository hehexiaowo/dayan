<script setup lang="ts">
/**
 * 开放平台 - 接入指南页。
 * 渲染 content.md（接入步骤），纯静态，无后端调用。
 */
import MarkdownDoc from '@/components/MarkdownDoc.vue'
import content from './content.md?raw'
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header"><span>接入指南</span></div>
      </template>
      <MarkdownDoc :src="content" />
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container { display: flex; flex-direction: column; gap: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
