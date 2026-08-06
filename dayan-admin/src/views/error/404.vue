<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="content">
      <h1 class="code">404</h1>
      <p class="desc">抱歉，您访问的页面不存在</p>
      <el-button type="primary" @click="router.replace('/')">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.not-found {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f2f5;

  .content {
    text-align: center;

    .code {
      margin: 0;
      font-size: 96px;
      font-weight: 700;
      color: #409eff;
      line-height: 1;
    }

    .desc {
      margin: 16px 0 24px;
      font-size: 16px;
      color: #595959;
    }
  }
}
</style>
