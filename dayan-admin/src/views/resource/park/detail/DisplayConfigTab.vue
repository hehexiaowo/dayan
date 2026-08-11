<template>
  <div class="display-config-tab" v-loading="loading">
    <el-alert v-if="!networks.length" type="warning" :closable="false" show-icon>
      该机构未配置网络归属（networkTags），请先在「基本信息」Tab 中选择网络后再配置展示。
    </el-alert>

    <template v-for="net in networks" :key="net.tag">
      <div class="network-panel">
        <div class="panel-header">
          <el-tag :type="net.colorType" effect="dark" size="large">{{ net.label }}</el-tag>
          <span class="panel-hint">配置该网络下详情页头图和列表缩略图</span>
        </div>

        <!-- 头图选择 -->
        <div class="section">
          <div class="section-title">
            <span>详情页头图轮播</span>
            <el-text type="info" size="small">点击图片选择/取消，已选 {{ configData[net.tag].banners.length }} 张</el-text>
          </div>
          <div class="image-grid" v-if="images.length">
            <div
              v-for="img in images"
              :key="img.assetUrl"
              class="image-cell"
              :class="{ selected: bannerIndex(net.tag, img.assetUrl) >= 0 }"
              @click="toggleBanner(net.tag, img.assetUrl)"
            >
              <el-image :src="formatFileUrl(img.assetUrl)" fit="cover" class="grid-image" :preview-src-list="[]" />
              <div v-if="bannerIndex(net.tag, img.assetUrl) >= 0" class="order-badge">
                {{ bannerIndex(net.tag, img.assetUrl) + 1 }}
              </div>
              <div class="image-check">
                <el-icon v-if="bannerIndex(net.tag, img.assetUrl) >= 0"><Check /></el-icon>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无图片素材，请先在「素材库」上传图片" :image-size="60" />
        </div>

        <!-- 缩略图选择 -->
        <div class="section">
          <div class="section-title">
            <span>列表卡片缩略图</span>
            <el-text type="info" size="small">选择一张作为列表卡片展示图</el-text>
          </div>
          <div class="image-grid" v-if="images.length">
            <div
              v-for="img in images"
              :key="'thumb-' + img.assetUrl"
              class="image-cell thumb"
              :class="{ selected: configData[net.tag].thumbnail === img.assetUrl }"
              @click="configData[net.tag].thumbnail = configData[net.tag].thumbnail === img.assetUrl ? '' : img.assetUrl"
            >
              <el-image :src="formatFileUrl(img.assetUrl)" fit="cover" class="grid-image" :preview-src-list="[]" />
              <div v-if="configData[net.tag].thumbnail === img.assetUrl" class="image-check">
                <el-icon><Check /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <el-divider />
      </div>
    </template>

    <!-- 保存按钮 -->
    <div class="save-bar" v-if="networks.length">
      <el-button type="primary" :loading="saving" @click="onSave">保存配置</el-button>
      <el-button @click="loadData">重置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { getPark, updatePark } from '@/api/park'
import { listAssets } from '@/api/park-asset'
import { formatFileUrl } from '@/utils/file'
import { NETWORK_TAG_OPTIONS } from '@/types/park'
import type { ParkAsset, ParkInfo } from '@/types/park'

const props = defineProps<{ parkCode: string }>()

const loading = ref(true)
const saving = ref(false)
const images = ref<ParkAsset[]>([])
const parkInfo = ref<ParkInfo | null>(null)

interface NetworkConfig {
  banners: string[]
  thumbnail: string
}

/** 每个网络 tag 的配置数据 */
const configData = reactive<Record<string, NetworkConfig>>({
  vital: { banners: [], thumbnail: '' },
  care: { banners: [], thumbnail: '' },
  sojourn: { banners: [], thumbnail: '' },
})

/** 机构所属网络列表（仅展示这些网络的配置面板） */
const networks = computed(() => {
  const tags = parkInfo.value?.networkTags || []
  return NETWORK_TAG_OPTIONS.filter((o) => tags.includes(o.value)).map((o) => ({
    tag: o.value,
    label: o.label,
    colorType: o.value === 'vital' ? 'primary' : o.value === 'care' ? 'warning' : 'success',
  }))
})

/** banner 在数组中的位置（-1 = 未选） */
function bannerIndex(tag: string, assetUrl: string): number {
  return configData[tag].banners.indexOf(assetUrl)
}

/** 点击图片：已选则取消，未选则追加到末尾 */
function toggleBanner(tag: string, assetUrl: string) {
  const idx = bannerIndex(tag, assetUrl)
  if (idx >= 0) {
    configData[tag].banners.splice(idx, 1)
  } else {
    configData[tag].banners.push(assetUrl)
  }
}

/** 从 JSON 字符串解析配置 */
function parseConfig(raw?: string): NetworkConfig {
  if (!raw) return { banners: [], thumbnail: '' }
  try {
    const parsed = JSON.parse(raw)
    return {
      banners: Array.isArray(parsed.banners) ? parsed.banners : [],
      thumbnail: parsed.thumbnail || '',
    }
  } catch {
    return { banners: [], thumbnail: '' }
  }
}

async function loadData() {
  loading.value = true
  try {
    const [park, assetList] = await Promise.all([
      getPark(props.parkCode),
      listAssets(props.parkCode, 1),
    ])
    parkInfo.value = park
    images.value = (assetList || []).filter((a) => a.status === 1)

    configData.vital = parseConfig(park.vitalConfig)
    configData.care = parseConfig(park.careConfig)
    configData.sojourn = parseConfig(park.sojournConfig)
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    const data: Partial<ParkInfo> = {}
    for (const net of networks.value) {
      const cfg = configData[net.tag]
      const json: Record<string, unknown> = {}
      if (cfg.banners.length) json.banners = cfg.banners
      if (cfg.thumbnail) json.thumbnail = cfg.thumbnail
      const field = `${net.tag}Config` as keyof ParkInfo
      data[field] = Object.keys(json).length ? JSON.stringify(json) : '' as any
    }
    await updatePark(props.parkCode, data)
    ElMessage.success('展示配置已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.display-config-tab {
  padding: 16px;
}

.network-panel {
  margin-bottom: 24px;
}
.panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.panel-hint {
  font-size: 13px;
  color: #909399;
}

.section {
  margin-bottom: 20px;
}
.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.image-cell {
  position: relative;
  width: 120px;
  height: 90px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 3px solid transparent;
  transition: border-color 0.2s;
}
.image-cell.selected {
  border-color: var(--el-color-primary);
}
.image-cell.thumb.selected {
  border-color: var(--el-color-success);
}
.grid-image {
  width: 100%;
  height: 100%;
}
.order-badge {
  position: absolute;
  top: 4px;
  left: 4px;
  background: var(--el-color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: bold;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.image-check {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}
.image-cell.thumb .image-check {
  background: var(--el-color-success);
}

.save-bar {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color);
}
</style>
