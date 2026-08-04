<template>
  <view class="page">
    <view class="header">
      <text class="header-title">活动 / 内容素材</text>
      <text class="header-sub">分享优质内容，触达更多客户</text>
    </view>

    <!-- 活动列表 -->
    <view class="list">
      <view v-if="loading && !activities.length" class="empty">加载中...</view>
      <view v-else-if="!activities.length" class="empty">
        暂无活动（接口待后端提供）
      </view>

      <view v-else>
        <view
          v-for="a in activities"
          :key="a.activityId"
          class="card"
          @click="onCard(a)"
        >
          <image
            v-if="a.coverImage"
            class="cover"
            :src="a.coverImage"
            mode="aspectFill"
          />
          <view class="card-body">
            <view class="card-title">{{ a.title }}</view>
            <view class="card-summary">{{ a.summary || '（无摘要）' }}</view>
            <view class="card-foot">
              <text class="read">
                阅读：{{ typeof a.readCount === 'number' ? a.readCount : 0 }}
              </text>
              <view class="footer-right">
                <text class="time" v-if="a.createdAt">{{ a.createdAt }}</text>
                <button
                  class="btn-share"
                  size="mini"
                  @click.stop="onShare(a)"
                >
                  分享
                </button>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { onPullDownRefresh } from '@dcloudio/uni-app';
import { getActivities } from '@/api/activity';
import type { Activity } from '@/types';

const activities = ref<Activity[]>([]);
const loading = ref(false);

async function loadList() {
  loading.value = true;
  try {
    const list = await getActivities();
    activities.value = Array.isArray(list) ? list : [];
  } catch (e) {
    activities.value = [];
  } finally {
    loading.value = false;
  }
}

function onCard(a: Activity) {
  uni.showModal({
    title: a.title,
    content: a.summary || '（无详细内容）',
    showCancel: false,
  });
}

function onShare(a: Activity) {
  uni.showToast({ title: `分享：${a.title}（开发中）`, icon: 'none' });
}

onMounted(() => {
  loadList();
});

onPullDownRefresh(async () => {
  try {
    await loadList();
  } finally {
    uni.stopPullDownRefresh();
  }
});
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 60rpx;
  min-height: 100vh;
  background: #f5f7fa;
}

/* 头部 */
.header {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}
.header-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #303133;
}
.header-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #909399;
}

/* 列表 */
.empty {
  text-align: center;
  color: #909399;
  font-size: 26rpx;
  padding: 80rpx 0;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
}
.cover {
  width: 100%;
  height: 320rpx;
  background: #f0f0f0;
}
.card-body {
  padding: 24rpx 28rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}
.card-summary {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #606266;
  line-height: 1.5;
}
.card-foot {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.read {
  font-size: 24rpx;
  color: #909399;
}
.footer-right {
  display: flex;
  align-items: center;
}
.time {
  font-size: 24rpx;
  color: #c0c4cc;
  margin-right: 16rpx;
}
.btn-share {
  background: #ff9900;
  color: #fff;
  font-size: 24rpx;
}
</style>
