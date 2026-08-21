<template>
  <view class="page">
    <!-- 步骤进度 -->
    <ArticleRewriteSteps :current="0" />

    <!-- 输入方式切换（解析成功后隐藏） -->
    <template v-if="!contentData">
      <view class="tab-row">
        <text
          v-for="tab in tabs"
          :key="tab.value"
          class="tab-item dy-clickable"
          :class="{ on: inputType === tab.value }"
          @click="inputType = tab.value"
        >{{ tab.label }}</text>
      </view>

      <!-- URL链接输入 -->
      <template v-if="inputType === 'url'">
        <view class="dy-section-title">文章链接</view>
        <input
          class="dy-input"
          v-model="url"
          placeholder="粘贴微信公众号、新闻等文章链接"
          @confirm="onFetchUrl"
        />
        <view
          class="btn-primary dy-clickable"
          :class="{ 'dy-btn-disabled': fetching }"
          @click="onFetchUrl"
        >
          <text class="btn-primary-text">{{ fetching ? '解析中...' : '解析内容' }}</text>
        </view>
      </template>

      <!-- 手动输入 -->
      <template v-if="inputType === 'manual'">
        <view class="dy-section-title">文章标题 <text class="req">*</text></view>
        <input class="dy-input" v-model="title" placeholder="请输入文章标题" />

        <view class="dy-section-title">文章来源</view>
        <input class="dy-input" v-model="source" placeholder="请输入文章来源（选填）" />

        <view class="dy-section-title">文章正文 <text class="req">*</text></view>
        <textarea
          class="dy-textarea"
          v-model="content"
          maxlength="10000"
          placeholder="粘贴文章全文（最多10000字）"
        />
        <view class="word-count">
          <text class="word-count-text">{{ content.length }}/10000</text>
        </view>

        <view
          class="btn-primary dy-clickable"
          :class="{ 'dy-btn-disabled': !title || !content || submitting }"
          @click="onInputManual"
        >
          <text class="btn-primary-text">{{ submitting ? '提交中...' : '下一步' }}</text>
        </view>
      </template>

      <!-- 文章引入（暂不支持） -->
      <template v-if="inputType === 'article'">
        <view class="empty-state">
          <text class="empty-text">暂不支持从平台文章引入，敬请期待</text>
        </view>
      </template>
    </template>

    <!-- 解析结果预览 -->
    <view v-if="contentData && contentData.fetchStatus === 'success'" class="result-card dy-card">
      <view class="dy-section-title">解析结果</view>
      <view class="result-item">
        <text class="result-label">标题</text>
        <text class="result-value">{{ contentData.originalTitle }}</text>
      </view>
      <view class="result-item">
        <text class="result-label">来源</text>
        <text class="result-value">{{ contentData.originalSource }}</text>
      </view>
      <view class="result-item" v-if="contentData.originalPublishTime">
        <text class="result-label">发布时间</text>
        <text class="result-value">{{ contentData.originalPublishTime }}</text>
      </view>
      <view class="result-item">
        <text class="result-label">正文内容</text>
        <text class="result-value content-full">{{ contentData.originalContent }}</text>
      </view>
      <view class="result-actions">
        <view class="btn-secondary dy-clickable" @click="onReset">
          <text class="btn-secondary-text">重新输入</text>
        </view>
        <view class="btn-primary dy-clickable" @click="onNext">
          <text class="btn-primary-text">下一步</text>
        </view>
      </view>
    </view>

    <!-- 解析失败提示 -->
    <view v-if="contentData && contentData.fetchStatus === 'failed'" class="error-card dy-card">
      <text class="error-text">{{ contentData.fetchError || '解析失败，请检查链接或手动输入内容' }}</text>
      <view class="btn-secondary dy-clickable" @click="onReset">
        <text class="btn-secondary-text">重新输入</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchByUrl, inputManual, getRewriteDetail } from '@/api/toolArticleRewrite'
import type { ArticleRewriteRecord, ContentFetch } from '@/types/toolArticleRewrite'
import { rewritePhaseStep, getErrMsg } from '@/types/toolArticleRewrite'
import ArticleRewriteSteps from '@/components/ArticleRewriteSteps/ArticleRewriteSteps.vue'

const tabs = [
  { value: 'url', label: '链接输入' },
  { value: 'manual', label: '手动输入' },
  { value: 'article', label: '文章引入' },
]

const inputType = ref<'url' | 'manual' | 'article'>('url')
const url = ref('')
const title = ref('')
const source = ref('')
const content = ref('')
const fetching = ref(false)
const record = ref<ArticleRewriteRecord | null>(null)
const toolCode = ref('TL90008')
const submitting = ref(false)

// 解析 contentFetch 字符串为对象
const contentData = computed<ContentFetch | null>(() => {
  if (!record.value?.contentFetch) return null
  if (typeof record.value.contentFetch === 'string') {
    try {
      return JSON.parse(record.value.contentFetch)
    } catch {
      return null
    }
  }
  return record.value.contentFetch as ContentFetch
})

onLoad((options) => {
  if (options?.toolCode) {
    toolCode.value = options.toolCode
  }
  if (options?.id) {
    loadDetail(Number(options.id))
  }
})

async function loadDetail(id: number) {
  try {
    record.value = await getRewriteDetail(id)
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

async function onFetchUrl() {
  if (!url.value || fetching.value) return

  // 简单的URL格式校验
  if (!url.value.startsWith('http://') && !url.value.startsWith('https://')) {
    uni.showToast({ title: '请输入正确的链接', icon: 'none' })
    return
  }

  fetching.value = true
  try {
    record.value = await fetchByUrl(toolCode.value, url.value)
  } catch (e: any) {
    uni.showToast({ title: getErrMsg(e, '解析失败'), icon: 'none' })
  } finally {
    fetching.value = false
  }
}

async function onInputManual() {
  if (!title.value || !content.value || submitting.value) return

  submitting.value = true
  try {
    record.value = await inputManual(toolCode.value, title.value, source.value, content.value)
  } catch (e: any) {
    uni.showToast({ title: getErrMsg(e, '提交失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}

function onReset() {
  record.value = null
  url.value = ''
  title.value = ''
  source.value = ''
  content.value = ''
}

function onNext() {
  if (!record.value) return
  uni.navigateTo({
    url: `/pages/acquisition/tools/article-rewrite/step-summary?id=${record.value.id}`,
  })
}
</script>

<style scoped lang="scss">
.page {
  padding: $spacing-md;
  padding-bottom: 40rpx;
  background: $bg-page;
  min-height: 100vh;
}

.tab-row {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.tab-item {
  flex: 1;
  height: $control-height;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: $radius-md;
  font-size: 28rpx;
  color: $text-secondary;
  border: 2rpx solid transparent;

  &.on {
    color: $brand-primary;
    border-color: $brand-primary;
    background: rgba($brand-primary, 0.05);
  }
}

.req {
  color: $brand-error;
  margin-left: 4rpx;
}

.dy-input {
  background: #fff;
  border-radius: $radius-md;
  padding: 0 $spacing-md;
  height: $control-height;
  font-size: 28rpx;
  margin-bottom: $spacing-md;
}

.dy-textarea {
  background: #fff;
  border-radius: $radius-md;
  padding: $spacing-md;
  font-size: 28rpx;
  width: 100%;
  min-height: 300rpx;
  margin-bottom: $spacing-sm;
}

.word-count {
  text-align: right;
  margin-bottom: $spacing-md;
}

.word-count-text {
  font-size: 24rpx;
  color: $text-placeholder;
}

.btn-primary {
  height: $control-height;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $gradient-blue;
  border-radius: $radius-md;
  margin-bottom: $spacing-md;
}

.btn-primary-text {
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
}

.btn-secondary {
  height: $control-height;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: $radius-md;
  border: 2rpx solid $brand-primary;
}

.btn-secondary-text {
  color: $brand-primary;
  font-size: 30rpx;
}

.result-card {
  padding: $spacing-md;
  margin-top: $spacing-md;
}

.result-item {
  margin-bottom: $spacing-md;
}

.result-label {
  font-size: 24rpx;
  color: $text-secondary;
  margin-bottom: 8rpx;
  display: block;
}

.result-value {
  font-size: 28rpx;
  color: $text-primary;
  display: block;
}

.content-full {
  font-size: 28rpx;
  color: $text-primary;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
}

.result-actions {
  display: flex;
  gap: $spacing-md;
  margin-top: $spacing-lg;
}

.result-actions .btn-primary,
.result-actions .btn-secondary {
  flex: 1;
}

.error-card {
  padding: $spacing-md;
  margin-top: $spacing-md;
  text-align: center;
}

.error-text {
  font-size: 28rpx;
  color: $brand-error;
  margin-bottom: $spacing-md;
  display: block;
}

.empty-state {
  padding: 100rpx 0;
  text-align: center;
}

.empty-text {
  font-size: 28rpx;
  color: $text-placeholder;
}
</style>
