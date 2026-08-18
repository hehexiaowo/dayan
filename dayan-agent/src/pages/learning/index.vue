<template>
  <view class="page dy-safe-bottom">
    <!-- ===== Banner 渐变区 ===== -->
    <view class="banner">
      <view class="banner-content">
        <text class="banner-title">学习中心</text>
        <text class="banner-sub">专业赋能，持续提升</text>
      </view>
      <view class="banner-icon">
        <text class="banner-icon-text">学</text>
      </view>
    </view>

    <!-- ===== 四板块宫格（骨架 / 错误 / 数据三态） ===== -->
    <view class="boards">
      <!-- 骨架屏 -->
      <template v-if="loading">
        <DySkeleton v-for="i in 4" :key="i" :rows="2" card />
      </template>

      <!-- 加载失败 -->
      <DyEmpty
        v-else-if="loadError"
        text="加载失败，请检查网络后重试"
        icon="!"
        color="gray"
        action-text="重新加载"
        @action="loadCounts"
      />

      <!-- 板块卡片 -->
      <view
        v-for="board in boards"
        v-else
        :key="board.key"
        class="board-card dy-clickable"
        @click="goBoard(board)"
      >
        <view class="board-icon" :class="'icon-' + board.key">
          <text class="board-icon-text">{{ board.icon }}</text>
        </view>
        <text class="board-title">{{ board.title }}</text>
        <text class="board-sub">{{ board.subtitle }}</text>
        <view class="board-foot">
          <text class="board-count">{{ counts[board.key] }} {{ board.unit }}</text>
          <text class="board-arrow">›</text>
        </view>
      </view>
    </view>

    <!-- ===== 底部提示 ===== -->
    <view class="foot-note">
      <text class="foot-note-text">课程持续更新，学习的每一步都有收获</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getCourses } from '@/api/course';
import { CourseSource } from '@/types';
import type { Course } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

/**
 * 学习中心首页：四大课程板块宫格（2×2）。
 *
 * - 四板块统一 course_info，course_source 区隔（1=大雁 2=渠道 3=外部 4=雁鸣）；
 * - 计数：一次拉全量 /courses（仅上架）后按 courseSource 本地分桶；
 * - tab 页统一 onShow 加载（首次进入也触发，避免 onMounted+onShow 双请求）。
 */

interface BoardDef {
  key: 'dayan' | 'channel' | 'external' | 'yanming';
  title: string;
  subtitle: string;
  icon: string;
  unit: string;
  /** 板块来源；大雁课程走独立课程列表页 */
  source?: CourseSource;
  url: string;
}

const boards: BoardDef[] = [
  {
    key: 'dayan',
    title: '大雁课程',
    subtitle: '平台自研 · 讲师亲授',
    icon: '雁',
    unit: '门',
    url: '/pages/learning/courses/index',
  },
  {
    key: 'channel',
    title: '渠道课程',
    subtitle: '渠道培训 · 实战进阶',
    icon: '渠',
    unit: '门',
    source: CourseSource.CHANNEL,
    url: '/pages/learning/board/index?source=2',
  },
  {
    key: 'external',
    title: '外部课程',
    subtitle: '精选引进 · 拓展视野',
    icon: '外',
    unit: '门',
    source: CourseSource.EXTERNAL,
    url: '/pages/learning/board/index?source=3',
  },
  {
    key: 'yanming',
    title: '雁鸣中国',
    subtitle: '品牌资讯 · 行业洞察',
    icon: '鸣',
    unit: '条',
    source: CourseSource.YANMING,
    url: '/pages/learning/board/index?source=4',
  },
];

const loading = ref(false);
const loadError = ref(false);
const counts = reactive<Record<BoardDef['key'], number>>({
  dayan: 0,
  channel: 0,
  external: 0,
  yanming: 0,
});

async function loadCounts() {
  loading.value = true;
  loadError.value = false;
  try {
    const courses = await getCourses().catch(() => [] as Course[]);
    counts.dayan = courses.filter((c) => (c.courseSource ?? CourseSource.SELF) === CourseSource.SELF).length;
    counts.channel = courses.filter((c) => c.courseSource === CourseSource.CHANNEL).length;
    counts.external = courses.filter((c) => c.courseSource === CourseSource.EXTERNAL).length;
    counts.yanming = courses.filter((c) => c.courseSource === CourseSource.YANMING).length;
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function goBoard(board: BoardDef) {
  uni.navigateTo({ url: board.url });
}

onShow(() => {
  loadCounts();
});
</script>

<style lang="scss" scoped>

.page {
  padding: $spacing-md $spacing-md 60rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* ===== Banner ===== */
.banner {
  background: $gradient-blue;
  border-radius: $radius-lg;
  padding: $spacing-xl $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.banner-content {
  flex: 1;
}

.banner-title {
  font-size: 38rpx;
  font-weight: bold;
  color: #fff;
}

.banner-sub {
  display: block;
  margin-top: $spacing-xs;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

.banner-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: 4rpx solid rgba(255, 255, 255, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.banner-icon-text {
  font-size: 44rpx;
  font-weight: bold;
  color: #fff;
}

/* ===== 四板块宫格（2×2） ===== */
.boards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $spacing-md;
}

.board-card {
  display: flex;
  flex-direction: column;
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-lg $spacing-lg $spacing-md;
  box-shadow: $shadow-card;
  transition: transform 0.15s ease;
}

.board-card:active {
  transform: scale(0.98);
}

.board-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $spacing-sm;
}

.icon-dayan {
  background: $gradient-blue;
}

.icon-channel {
  background: $gradient-green;
}

.icon-external {
  background: $gradient-purple;
}

.icon-yanming {
  background: $gradient-orange;
}

.board-icon-text {
  font-size: 44rpx;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.92);
}

.board-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}

.board-sub {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: $text-secondary;
}

.board-foot {
  margin-top: $spacing-sm;
  padding-top: $spacing-sm;
  border-top: 1rpx solid $border-light;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.board-count {
  font-size: 22rpx;
  color: $brand-primary;
}

.board-arrow {
  font-size: 36rpx;
  line-height: 1;
  color: $text-placeholder;
}

/* ===== 底部提示 ===== */
.foot-note {
  display: flex;
  justify-content: center;
  padding: $spacing-lg 0;
}

.foot-note-text {
  font-size: 24rpx;
  color: $text-placeholder;
}
</style>
