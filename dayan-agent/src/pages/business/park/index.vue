<template>
  <view class="categories-page">
    <!-- Hero 渐变区 -->
    <view class="hero">
      <text class="hero-title">全国养老机构网络</text>
      <text class="hero-sub">按居住形态查询，为长辈找到合适的家</text>
    </view>

    <!-- 加载骨架屏 -->
    <view v-if="loading" class="cat-list">
      <DySkeleton v-for="i in 3" :key="i" :rows="1" avatar card />
    </view>

    <!-- 分类卡片 -->
    <view v-else class="cat-list">
      <view
        v-for="cat in categories"
        :key="cat.category"
        class="cat-card dy-clickable"
        @click="onCategoryClick(cat)"
      >
        <DyIconBlock
          :text="iconText[cat.category]"
          :color="iconColor[cat.category]"
          size="lg"
        />
        <view class="cat-info">
          <text class="cat-name">{{ cat.categoryName }}</text>
          <text class="cat-desc">{{ descMap[cat.category] }}</text>
        </view>
        <view class="cat-count" :class="{ empty: !cat.available }">
          <text v-if="cat.available">{{ cat.count }} 家</text>
          <text v-else>即将上线</text>
        </view>
        <text v-if="cat.available" class="cat-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getCategories } from '@/api/park';
import type { CategoryCount, ParkCategory } from '@/types/park';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';

const categories = ref<CategoryCount[]>([]);
const loading = ref(true);

const iconText: Record<ParkCategory, string> = {
  vital: '活',
  care: '护',
  sojourn: '旅',
};

const iconColor: Record<ParkCategory, 'blue' | 'orange' | 'green'> = {
  vital: 'blue',
  care: 'orange',
  sojourn: 'green',
};

const descMap: Record<ParkCategory, string> = {
  vital: '自理活力老人 · CCRC 社区',
  care: '专业照护 · 医养结合',
  sojourn: '候鸟式旅居 · 四季轮换',
};

async function loadCategories() {
  loading.value = true;
  try {
    categories.value = await getCategories();
  } catch (e) {
    // 降级：显示默认数据
    categories.value = [
      { category: 'vital', categoryName: '活力长居', count: 0, available: true },
      { category: 'care', categoryName: '照护长居', count: 0, available: true },
      { category: 'sojourn', categoryName: '旅居养老', count: 0, available: false },
    ];
  } finally {
    loading.value = false;
  }
}

function onCategoryClick(cat: CategoryCount) {
  if (!cat.available) {
    uni.showToast({ title: `${cat.categoryName}即将上线`, icon: 'none' });
    return;
  }
  const pathMap: Record<string, string> = {
    vital: '/pages/business/park/vital/list',
    care: '/pages/business/park/care/list',
    sojourn: '/pages/business/park/sojourn/list',
  };
  uni.navigateTo({ url: pathMap[cat.category] || pathMap.vital });
}

onMounted(loadCategories);
</script>

<style lang="scss" scoped>

.categories-page {
  min-height: 100vh;
  background: $bg-page;
}

/* Hero 区 */
.hero {
  background: linear-gradient(135deg, #409eff, #19be6b, #ff9900);
  padding: 56rpx $spacing-lg 48rpx;
  border-bottom-left-radius: $radius-lg;
  border-bottom-right-radius: $radius-lg;
}
.hero-title {
  display: block;
  font-size: 42rpx;
  font-weight: bold;
  color: #fff;
  letter-spacing: 2rpx;
}
.hero-sub {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: $spacing-sm;
}

/* 分类卡片 */
.cat-list {
  padding: $spacing-md;
}
.cat-card {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
  display: flex;
  align-items: center;
  border: 1rpx solid $border-base;
  box-shadow: $shadow-card;
}
.cat-info {
  margin-left: $spacing-md;
  flex: 1;
}
.cat-name {
  display: block;
  font-size: 34rpx;
  font-weight: bold;
  color: $text-primary;
}
.cat-desc {
  display: block;
  font-size: 24rpx;
  color: $text-secondary;
  margin-top: $spacing-xs;
}
.cat-count {
  font-size: 28rpx;
  color: $brand-primary;
  font-weight: bold;
  flex-shrink: 0;

  &.empty {
    font-size: 22rpx;
    color: $text-placeholder;
    background: $brand-info-light;
    padding: 8rpx 20rpx;
    border-radius: 20rpx;
  }
}
.cat-arrow {
  font-size: 40rpx;
  color: $text-placeholder;
  margin-left: $spacing-sm;
}
</style>
