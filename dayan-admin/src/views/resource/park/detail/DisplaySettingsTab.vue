<script setup lang="ts">
/**
 * 机构详情页 - 页面展示 tab（容器组件）。
 *
 * 汇总纯前端展示控制的两个子域，内层 el-tabs 切换：
 * - 网络展示：三业态（旅居/活力长居/照护长居）各自的轮播图与缩略图
 * - 展示板块：C 端详情页图文板块（类型+富文本正文+配图+业态）
 */
import { ref } from 'vue'
import DisplayConfigTab from './DisplayConfigTab.vue'
import DisplayPane from './DisplayPane.vue'

defineProps<{
  parkCode: string
}>()

const activeName = ref('display-config')
</script>

<template>
  <el-tabs v-model="activeName" type="card" class="display-settings-tabs">
    <el-tab-pane label="网络展示" name="display-config" lazy>
      <DisplayConfigTab v-if="activeName === 'display-config'" :park-code="parkCode" />
    </el-tab-pane>
    <el-tab-pane label="展示板块" name="display-block" lazy>
      <DisplayPane v-if="activeName === 'display-block'" :park-code="parkCode" />
    </el-tab-pane>
  </el-tabs>
</template>

<style scoped>
.display-settings-tabs {
  margin-top: 8px;
}
</style>
