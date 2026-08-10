<template>
  <view class="categories-page">
    <!-- Hero 区 -->
    <view class="hero">
      <text class="hero-title">全国养老机构网络</text>
      <text class="hero-sub">按居住形态查询，为长辈找到合适的家</text>
    </view>

    <!-- 分类卡片 -->
    <view class="cat-list">
      <view
        v-for="cat in categories"
        :key="cat.category"
        class="cat-card"
        @click="onCategoryClick(cat)"
      >
        <view class="cat-icon" :class="'ico-' + cat.category">{{ iconMap[cat.category] }}</view>
        <view class="cat-info">
          <text class="cat-name">{{ cat.categoryName }}</text>
          <text class="cat-desc">{{ descMap[cat.category] }}</text>
        </view>
        <view class="cat-count" :class="{ 'empty': !cat.available }">
          {{ cat.available ? cat.count + ' 家' : '即将上线' }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getCategories } from '@/api/park';
import type { CategoryCount, ParkCategory } from '@/types/park';

const categories = ref<CategoryCount[]>([]);

const iconMap: Record<ParkCategory, string> = {
  vital: '🏃',
  care: '🏥',
  sojourn: '🏖️',
};

const descMap: Record<ParkCategory, string> = {
  vital: '自理活力老人 · CCRC 社区',
  care: '专业照护 · 医养结合',
  sojourn: '候鸟式旅居 · 四季轮换',
};

async function loadCategories() {
  try {
    categories.value = await getCategories();
  } catch (e) {
    // 降级：显示默认数据
    categories.value = [
      { category: 'vital', categoryName: '活力长居', count: 0, available: true },
      { category: 'care', categoryName: '照护长居', count: 0, available: true },
      { category: 'sojourn', categoryName: '旅居养老', count: 0, available: false },
    ];
  }
}

function onCategoryClick(cat: CategoryCount) {
  if (!cat.available) {
    uni.showToast({ title: '旅居机构即将上线', icon: 'none' });
    return;
  }
  uni.navigateTo({
    url: `/pages/business/park/region?category=${cat.category}&level=province`,
  });
}

onMounted(loadCategories);
</script>

<style scoped>
.categories-page {
  min-height: 100vh;
  background: #f5f7fa;
}
.hero {
  background: linear-gradient(135deg, #409eff, #66b1ff);
  padding: 40rpx 32rpx;
  color: #fff;
}
.hero-title {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  margin-bottom: 8rpx;
}
.hero-sub {
  font-size: 24rpx;
  opacity: 0.85;
}
.cat-list {
  padding: 24rpx;
}
.cat-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
  border: 1rpx solid #ebeef5;
}
.cat-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
  flex-shrink: 0;
}
.ico-vital { background: #e8f4ff; }
.ico-care { background: #fdf6ec; }
.ico-sojourn { background: #f0f9eb; }
.cat-info {
  margin-left: 24rpx;
  flex: 1;
}
.cat-name {
  display: block;
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
}
.cat-desc {
  display: block;
  font-size: 22rpx;
  color: #909399;
  margin-top: 6rpx;
}
.cat-count {
  font-size: 26rpx;
  color: #409eff;
  font-weight: bold;
  flex-shrink: 0;
}
.cat-count.empty {
  color: #c0c4cc;
}
</style>
