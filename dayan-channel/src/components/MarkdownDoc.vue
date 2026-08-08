<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'

/**
 * Markdown 文档渲染通用组件。
 * @prop src - Markdown 原文（通过 import xxx from './xxx.md?raw' 加载）
 */
const props = defineProps<{ src: string }>()

const html = computed(() => marked.parse(props.src, { async: false }) as string)
</script>

<template>
  <div class="markdown-body" v-html="html" />
</template>

<style scoped lang="scss">
.markdown-body {
  line-height: 1.8;
  :deep(h1) { font-size: 1.5em; margin: 0.67em 0; font-weight: 600; }
  :deep(h2) { font-size: 1.3em; margin: 0.83em 0; font-weight: 600; border-bottom: 1px solid var(--el-border-color-lighter); padding-bottom: 0.3em; }
  :deep(h3) { font-size: 1.1em; margin: 1em 0; font-weight: 600; }
  :deep(p) { margin: 0.8em 0; }
  :deep(ul), :deep(ol) { padding-left: 2em; margin: 0.8em 0; }
  :deep(li) { margin: 0.3em 0; }
  :deep(code) { background: var(--el-fill-color-light); padding: 0.15em 0.4em; border-radius: 3px; font-size: 0.9em; font-family: 'Consolas', 'Monaco', monospace; }
  :deep(pre) { background: var(--el-fill-color-darker); padding: 1em; border-radius: 6px; overflow-x: auto; margin: 1em 0; }
  :deep(pre code) { background: none; padding: 0; }
  :deep(table) { border-collapse: collapse; width: 100%; margin: 1em 0; }
  :deep(th), :deep(td) { border: 1px solid var(--el-border-color); padding: 0.5em 0.8em; text-align: left; }
  :deep(th) { background: var(--el-fill-color-light); font-weight: 600; }
  :deep(blockquote) { border-left: 4px solid var(--el-color-primary-light-5); padding-left: 1em; margin: 1em 0; color: var(--el-text-color-secondary); }
}
</style>
