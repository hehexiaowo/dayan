<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getDashboardStats, type DashboardStats } from '@/api/dashboard'

/**
 * 工作台 - 渠道概览。
 *
 * 展示本渠道 4 项核心指标（代理人/客户/权益/订单）。
 * 接口已实现，失败时降级显示 --，不弹错误 toast。
 */

interface StatCard {
  key: keyof DashboardStats
  title: string
  icon: string
  color: string
}

const cards: StatCard[] = [
  { key: 'agentCount', title: '代理人总数', icon: 'User', color: '#409eff' },
  { key: 'clientCount', title: '客户总数', icon: 'UserFilled', color: '#67c23a' },
  { key: 'equityCount', title: '权益总数', icon: 'Ticket', color: '#e6a23c' },
  { key: 'orderCount', title: '订单总数', icon: 'List', color: '#f56c6c' }
]

const stats = ref<DashboardStats>({})
const loading = ref(false)

function statValue(key: keyof DashboardStats): string {
  const v = stats.value[key]
  if (v === undefined || v === null) {
    return '--'
  }
  return String(v)
}

async function loadStats() {
  loading.value = true
  try {
    const data = await getDashboardStats()
    stats.value = data || {}
  } catch (err) {
    // 后端端点未实现，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[dashboard] 加载渠道统计失败（接口可能未实现）:', err)
    stats.value = {}
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadStats()
})
</script>

<template>
  <div class="dashboard">
    <!-- 欢迎卡 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome">
        <el-icon class="welcome-icon"><Odometer /></el-icon>
        <div class="welcome-text">
          <h2>欢迎使用大雁养老渠道管理后台</h2>
          <p>工作台 · 渠道核心指标概览</p>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片区 -->
    <div v-loading="loading" class="stat-grid">
      <el-card v-for="c in cards" :key="c.key" shadow="hover" class="stat-card">
        <div class="stat-body">
          <div class="stat-icon" :style="{ backgroundColor: c.color }">
            <el-icon :size="28"><component :is="c.icon" /></el-icon>
          </div>
          <div class="stat-meta">
            <div class="stat-title">{{ c.title }}</div>
            <div class="stat-value">{{ statValue(c.key) }}</div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.welcome-card {
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

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  .stat-body {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #ffffff;
    flex-shrink: 0;
  }

  .stat-meta {
    .stat-title {
      font-size: 13px;
      color: #8a8f99;
      margin-bottom: 6px;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 600;
      color: #1f2329;
      line-height: 1;
    }
  }
}

@media (max-width: 992px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 576px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
