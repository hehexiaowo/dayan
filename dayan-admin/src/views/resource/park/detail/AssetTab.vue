<script setup lang="ts">
/**
 * 机构详情页 - 素材仓库 tab。
 *
 * 锁定分类三元组（refType1=park + refCode=机构编码）复用统一 AssetPane，
 * 图片/视频/文档/VR 不做子 tab，作为筛选条件与清单列统一呈现。
 */
import AssetPane from './AssetPane.vue'

defineProps<{
  parkCode: string
}>()
</script>

<template>
  <AssetPane ref-type1="park" :ref-code="parkCode" />
</template>
