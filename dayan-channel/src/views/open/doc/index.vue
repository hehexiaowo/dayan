<script setup lang="ts">
/**
 * 接口文档页 - 左右分栏布局。
 * 左侧目录树（ApiCatalog，4 分组 9 接口）+ 右侧接口介绍（ApiDetail，含 ApiTester）。
 * 纯前端，无后端调用。接口当前建设中，测试面板仅展示请求构造。
 */
import { ref, computed } from 'vue'
import ApiCatalog from './ApiCatalog.vue'
import ApiDetail from './ApiDetail.vue'
import { groups, findApi } from './apis'

const selectedApiId = ref(groups[0].apis[0].id)
const selectedApi = computed(() => findApi(selectedApiId.value))
</script>

<template>
  <div class="api-doc-page">
    <div class="api-doc-sidebar">
      <ApiCatalog v-model="selectedApiId" :groups="groups" />
    </div>
    <div class="api-doc-main">
      <ApiDetail :api="selectedApi" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.api-doc-page {
  display: flex;
  // header 60 + main padding 16*2 = 92px；不依赖 height:100% 链
  height: calc(100vh - 92px);
  background: var(--el-bg-color);
  border-radius: 4px;
  overflow: hidden;
}
.api-doc-sidebar {
  width: 260px;
  flex-shrink: 0;
  overflow: hidden;
}
.api-doc-main {
  flex: 1;
  overflow-y: auto;
  min-width: 0;
}
</style>
