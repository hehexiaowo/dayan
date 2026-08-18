<template>
  <view class="page">
    <!-- 头部 -->
    <view class="header">
      <text class="header-title">工具获客</text>
      <text class="header-subtitle">专业计算工具，帮客户看清养老规划</text>
    </view>

    <!-- 工具列表 -->
    <view class="tool-list">
      <view
        v-for="tool in tools"
        :key="tool.id"
        class="tool-card dy-clickable"
        @click="onOpenTool(tool)"
      >
        <DyIconBlock :text="tool.icon" :color="tool.color" size="lg" shape="circle" />
        <view class="tool-info">
          <text class="tool-name">{{ tool.name }}</text>
          <text class="tool-desc">{{ tool.desc }}</text>
        </view>
        <text class="tool-arrow">›</text>
      </view>
    </view>

    <!-- 底部提示 -->
    <view class="bottom-tip">
      <text class="tip-text">更多工具持续上线中，敬请期待</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import { getTools } from '@/api/tool';
import type { ToolInfo } from '@/types';

type ToolColor = 'blue' | 'green' | 'orange' | 'red' | 'gray';

interface ToolItem {
  id: string;
  name: string;
  desc: string;
  icon: string;
  color: ToolColor;
  path: string;
}

/** 兜底工具列表 — 接口不可用/未配置时保证页面不空白（与 tool_info 预置种子一致） */
const FALLBACK_TOOLS: ToolItem[] = [
  {
    id: 'TL00001',
    name: '社保养老计算器',
    desc: '根据当前工资、缴费年限，估算退休后每月可领养老金',
    icon: '退',
    color: 'orange',
    path: '/pages/acquisition/tools/pension-calculator',
  },
  {
    id: 'TL00002',
    name: '养老缺口计算器',
    desc: '计算退休资金缺口，帮客户提前做好养老储备规划',
    icon: '缺',
    color: 'red',
    path: '/pages/acquisition/tools/gap-calculator',
  },
  {
    id: 'TL00004',
    name: '你问我答',
    desc: '基于知识库的 AI 问答：选人物、带引用、保留对话',
    icon: '答',
    color: 'red',
    path: '/pages/acquisition/qa/index',
  },
];

const tools = ref<ToolItem[]>(FALLBACK_TOOLS);

const TOOL_COLORS: readonly string[] = ['blue', 'green', 'orange', 'red', 'gray'];

/** 从工具配置 JSON 解析图标颜色（非法配置兜底 blue） */
function parseColor(config?: string): ToolColor {
  if (!config) return 'blue';
  try {
    const color = (JSON.parse(config) as { color?: string }).color;
    return color && TOOL_COLORS.includes(color) ? (color as ToolColor) : 'blue';
  } catch {
    return 'blue';
  }
}

function mapTool(t: ToolInfo): ToolItem {
  return {
    id: t.toolCode,
    name: t.toolName,
    desc: t.toolDesc || '',
    icon: t.icon || '工',
    color: parseColor(t.configJson),
    path: t.entryPath,
  };
}

async function loadTools() {
  try {
    const list = await getTools();
    // 后台已配置则以后台为准；空结果（全部禁用/未配置）保留兜底
    if (list && list.length > 0) {
      tools.value = list.map(mapTool);
    }
  } catch {
    // 接口异常时保留兜底列表
  }
}

onShow(() => {
  loadTools();
});

function onOpenTool(tool: ToolItem) {
  uni.navigateTo({ url: tool.path });
}
</script>

<style lang="scss" scoped>

.page {
  min-height: 100vh;
  background: $bg-page;
}

/* 头部 */
.header {
  background: $gradient-blue;
  padding: $spacing-xl $spacing-lg $spacing-xl;
  display: flex;
  flex-direction: column;
}
.header-title {
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
}
.header-subtitle {
  margin-top: $spacing-xs;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* 工具列表 */
.tool-list {
  padding: $spacing-md;
}

/* 工具卡片 */
.tool-card {
  display: flex;
  align-items: center;
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-lg $spacing-md;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
}
.tool-info {
  flex: 1;
  margin-left: $spacing-md;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.tool-name {
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}
.tool-desc {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $text-secondary;
  line-height: 1.5;
}
.tool-arrow {
  font-size: 48rpx;
  color: $text-placeholder;
  flex-shrink: 0;
  margin-left: $spacing-sm;
}

/* 底部提示 */
.bottom-tip {
  padding: $spacing-xl 0;
  display: flex;
  justify-content: center;
}
.tip-text {
  font-size: 24rpx;
  color: $text-placeholder;
}
</style>
