<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">AI 创作</text>
      <text class="hero-sub">文章转写：获取 → 判断 → 策略 → 转写 → 审查 → 配图 → 完成</text>
    </view>

    <!-- 文章转写入口 -->
    <view class="dy-section-title">创作工具</view>
    <view class="cat-card dy-card dy-clickable" @click="goArticleRewrite">
      <DyIconBlock text="转" color="purple" size="lg" shape="circle" />
      <view class="cat-info">
        <text class="cat-name">AI创作（文章转写）</text>
        <text class="cat-desc">从外部文章链接引入，AI辅助转写为适合不同渠道发布的内容</text>
      </view>
      <text class="cat-arrow">›</text>
    </view>

    <!-- 未完成创作草稿 -->
    <view class="dy-section-title" v-if="drafts.length || loading">未完成创作</view>
    <DySkeletonList v-if="loading" :rows="2" />
    <template v-else>
      <view v-for="d in drafts" :key="d.id" class="draft dy-card dy-clickable" @click="goDraft(d)">
        <view class="draft-top">
          <text class="dy-tag dy-tag-blue">文章转写</text>
          <text class="draft-phase">{{ REWRITE_STATUS_LABELS[d.status as keyof typeof REWRITE_STATUS_LABELS] ?? d.status }}</text>
        </view>
        <text class="draft-title">{{ d.originalTitle || '（未定主题）' }}</text>
        <view class="draft-bottom">
          <text class="draft-time">{{ formatTime(d.updatedAt) }}</text>
          <text class="draft-del dy-clickable" @click.stop="onDelete(d)">删除</text>
        </view>
      </view>
      <DyEmpty v-if="!drafts.length" icon="稿" text="暂无进行中的创作" color="gray" />
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getRewriteList, deleteRewrite } from '@/api/toolArticleRewrite'
import type { ArticleRewriteListItem } from '@/types/toolArticleRewrite'
import { REWRITE_STATUS_LABELS, rewritePhaseStep } from '@/types/toolArticleRewrite'
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue'
import DySkeletonList from '@/components/DySkeletonList/DySkeletonList.vue'
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue'

const drafts = ref<ArticleRewriteListItem[]>([])
const loading = ref(false)

onShow(async () => {
  loading.value = true
  try {
    drafts.value = await getRewriteList()
  } catch {
    // request 层已提示，草稿区静默降级为空
  } finally {
    loading.value = false
  }
})

function goArticleRewrite() {
  uni.navigateTo({ url: '/pages/acquisition/tools/article-rewrite/step-content?toolCode=TL90008' })
}

function goDraft(d: ArticleRewriteListItem) {
  uni.navigateTo({ url: `${rewritePhaseStep(d.status)}?id=${d.id}` })
}

function onDelete(d: ArticleRewriteListItem) {
  uni.showModal({
    title: '删除草稿',
    content: '删除后不可恢复，确定删除？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteRewrite(d.id)
        drafts.value = drafts.value.filter((x) => x.id !== d.id)
        uni.showToast({ title: '已删除', icon: 'none' })
      } catch {
        uni.showToast({ title: '删除失败', icon: 'none' })
      }
    },
  })
}

function formatTime(dt?: string) {
  if (!dt) return ''
  return dt.length >= 16 ? dt.substring(0, 16).replace('T', ' ') : dt
}
</script>

<style scoped lang="scss">
.page {
  padding: $spacing-md $spacing-md 60rpx;
  background: $bg-page;
  min-height: 100vh;
}

.hero {
  background: $gradient-purple;
  border-radius: $radius-lg;
  padding: $spacing-xl $spacing-lg;
  margin-bottom: $spacing-md;
}

.hero-title {
  display: block;
  font-size: 38rpx;
  font-weight: 700;
  color: #fff;
}

.hero-sub {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 12rpx;
}

.cat-card {
  display: flex;
  align-items: center;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
}

.cat-info {
  flex: 1;
  margin-left: $spacing-md;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.cat-name {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}

.cat-desc {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: $text-secondary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-arrow {
  font-size: 40rpx;
  color: $text-placeholder;
  margin-left: $spacing-sm;
}

.draft {
  margin-bottom: $spacing-sm;
  padding: $spacing-md;
}

.draft-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.draft-phase {
  font-size: 24rpx;
  color: $text-secondary;
}

.draft-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
  margin: 12rpx 0;
}

.draft-bottom {
  display: flex;
  justify-content: space-between;
}

.draft-time {
  font-size: 22rpx;
  color: $text-placeholder;
}

.draft-del {
  font-size: 24rpx;
  color: $brand-error;
}
</style>
