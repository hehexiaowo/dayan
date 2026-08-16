<script setup lang="ts">
// 首页看板占位页
</script>

<template>
  <div class="dashboard">
    <el-card shadow="never">
      <div class="welcome">
        <el-icon class="welcome-icon"><Odometer /></el-icon>
        <div class="welcome-text">
          <h2>欢迎使用大雁核心</h2>
          <p>首页看板占位页 · 后续将展示运营核心指标与待办。</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  .welcome {
    display: flex;
    align-items: center;
    gap: 16px;

    .welcome-icon {
      font-size: 48px;
      color: #409eff;
    }

    .welcome-text {
      h2 {
        margin: 0 0 8px;
        font-size: 20px;
        color: #1f2329;
      }

      p {
        margin: 0;
        font-size: 14px;
        color: #8a8f99;
      }
    }
  }
}
</style>
