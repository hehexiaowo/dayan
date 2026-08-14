<template>
  <div v-loading="loading" class="display-config-tab">
    <el-alert v-if="!networks.length" type="warning" :closable="false" show-icon>
      该机构未配置网络归属（networkTags），请先在「基本信息」Tab 中选择网络后再配置展示。
    </el-alert>

    <div v-for="net in networks" :key="net.tag" class="net-card">
      <div class="net-header">
        <el-tag :type="net.colorType" effect="dark">{{ net.label }}</el-tag>
        <span class="net-summary">
          轮播 {{ configData[net.tag].banners.length }} 张 ·
          {{ thumbOf(net.tag) ? `缩略图：第 ${configData[net.tag].banners.indexOf(thumbOf(net.tag)) + 1} 张` : '缩略图未设' }}
        </span>
      </div>

      <div class="row">
        <span class="row-label">轮播图</span>
        <div class="chosen-row">
          <div
            v-for="(key, i) in configData[net.tag].banners"
            :key="net.tag + key"
            class="chosen-cell"
            :class="{ 'is-thumb': key === thumbOf(net.tag) }"
            @click="setThumbnail(net.tag, key)"
          >
            <el-image :src="formatFileUrl(key)" fit="cover" class="chosen-img" :preview-src-list="[]" />
            <span class="order-badge">{{ i + 1 }}</span>
            <span v-if="key === thumbOf(net.tag)" class="thumb-badge">缩略图</span>
            <div class="cell-ops" @click.stop>
              <el-icon :class="{ disabled: i === 0 }" @click="moveBanner(net.tag, i, -1)"><ArrowLeft /></el-icon>
              <el-icon :class="{ disabled: i === configData[net.tag].banners.length - 1 }" @click="moveBanner(net.tag, i, 1)"><ArrowRight /></el-icon>
              <el-icon @click="removeBanner(net.tag, i)"><Delete /></el-icon>
            </div>
          </div>
          <el-button :icon="Plus" plain @click="openPicker(net.tag)">从图片库选择</el-button>
        </div>
      </div>

      <div class="row hint-row">
        <span class="row-label">缩略图</span>
        <span class="hint">点击上方任一轮播图设为缩略图；未手动指定时默认第一张。</span>
      </div>
    </div>

    <div v-if="networks.length" class="save-bar">
      <el-button type="primary" :loading="saving" @click="onSave">保存配置</el-button>
      <el-button @click="loadData">重置</el-button>
    </div>

    <AssetPicker
      v-model="pickerVisible"
      type="image"
      multiple
      :limit="9"
      :park-code="parkCode"
      @select="onPicked"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 机构详情页 - 网络展示 tab（轻量版）。
 *
 * 每业态 = 轮播图（已选横排：排序/删除/从图片库选择）+ 缩略图（点击轮播图点选，默认第一张）。
 * 数据结构不变：park_info.xxxConfig 存 JSON {banners:[key...], thumbnail:"key"}；
 * 选择弹窗复用 AssetPicker（机构素材库），组件不再全量加载机构图。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Delete, Plus } from '@element-plus/icons-vue'
import { getPark, updatePark } from '@/api/park'
import { formatFileUrl } from '@/utils/file'
import { NETWORK_TAG_OPTIONS } from '@/types/park'
import type { ParkInfo } from '@/types/park'
import AssetPicker from '@/components/AssetPicker/index.vue'

const props = defineProps<{ parkCode: string }>()

const loading = ref(true)
const saving = ref(false)
const parkInfo = ref<ParkInfo | null>(null)

interface NetworkConfig {
  banners: string[]
  thumbnail: string
}

const configData = reactive<Record<string, NetworkConfig>>({
  vital: { banners: [], thumbnail: '' },
  care: { banners: [], thumbnail: '' },
  sojourn: { banners: [], thumbnail: '' },
})

const networks = computed(() => {
  const tags = parkInfo.value?.networkTags || []
  return NETWORK_TAG_OPTIONS.filter((o) => tags.includes(o.value)).map((o) => ({
    tag: o.value,
    label: o.label,
    colorType: o.value === 'vital' ? 'primary' : o.value === 'care' ? 'warning' : 'success',
  }))
})

const pickerVisible = ref(false)
const pickerTarget = ref('')

function thumbOf(tag: string): string {
  const cfg = configData[tag]
  if (cfg.thumbnail && cfg.banners.includes(cfg.thumbnail)) return cfg.thumbnail
  return cfg.banners[0] || ''
}

function setThumbnail(tag: string, key: string) {
  configData[tag].thumbnail = thumbOf(tag) === key ? '' : key
}

function openPicker(tag: string) {
  pickerTarget.value = tag
  pickerVisible.value = true
}

function onPicked(keys: string[]) {
  const tag = pickerTarget.value
  // 防御：AssetPicker 对脏数据（assetUrl 为空）可能 emit ''，合并前过滤
  const picked = keys.filter((k) => k)
  if (!tag || !picked.length) return
  const cfg = configData[tag]
  const merged = [...new Set([...cfg.banners, ...picked])]
  cfg.banners = merged.slice(0, 12)
  if (merged.length > cfg.banners.length) ElMessage.info('轮播图最多 12 张，超出部分已忽略')
}

function moveBanner(tag: string, index: number, dir: -1 | 1) {
  const arr = configData[tag].banners
  const target = index + dir
  if (target < 0 || target >= arr.length) return
  ;[arr[index], arr[target]] = [arr[target], arr[index]]
}

function removeBanner(tag: string, index: number) {
  const cfg = configData[tag]
  cfg.banners.splice(index, 1)
  if (cfg.thumbnail && !cfg.banners.includes(cfg.thumbnail)) cfg.thumbnail = ''
}

function parseConfig(raw?: string): NetworkConfig {
  if (!raw) return { banners: [], thumbnail: '' }
  try {
    const parsed = JSON.parse(raw)
    // 防御：历史数据可能含空串/重复项（避免 :key 冲突），过滤+去重并截断到上限 12
    const list: string[] = Array.isArray(parsed.banners)
      ? parsed.banners.filter((k: unknown): k is string => typeof k === 'string' && !!k)
      : []
    return {
      banners: [...new Set(list)].slice(0, 12),
      thumbnail: typeof parsed.thumbnail === 'string' ? parsed.thumbnail : '',
    }
  } catch {
    return { banners: [], thumbnail: '' }
  }
}

async function loadData() {
  loading.value = true
  try {
    const park = await getPark(props.parkCode)
    parkInfo.value = park
    configData.vital = parseConfig(park.vitalConfig)
    configData.care = parseConfig(park.careConfig)
    configData.sojourn = parseConfig(park.sojournConfig)
  } catch {
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
      const thumbnail = thumbOf(net.tag)
      const json: Record<string, unknown> = {}
      if (cfg.banners.length) json.banners = cfg.banners
      if (thumbnail) json.thumbnail = thumbnail
      const field = `${net.tag}Config` as keyof ParkInfo
      ;(data as Record<string, unknown>)[field] = Object.keys(json).length ? JSON.stringify(json) : ''
    }
    await updatePark(props.parkCode, data)
    ElMessage.success('展示配置已保存')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.display-config-tab {
  padding: 16px;
}

.net-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
}

.net-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.net-summary {
  font-size: 13px;
  color: #909399;
}

.row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 6px;
}

.row-label {
  font-size: 14px;
  font-weight: 600;
  line-height: 32px;
  flex-shrink: 0;
}

.chosen-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.chosen-cell {
  position: relative;
  width: 96px;
  height: 64px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s;

  &:hover .cell-ops {
    opacity: 1;
  }
  &.is-thumb {
    border-color: var(--el-color-success);
  }
}

.chosen-img {
  width: 100%;
  height: 100%;
}

.order-badge {
  position: absolute;
  top: 3px;
  left: 3px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 9px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-badge {
  position: absolute;
  bottom: 3px;
  left: 3px;
  background: var(--el-color-success);
  color: #fff;
  font-size: 11px;
  padding: 1px 5px;
  border-radius: 3px;
}

.cell-ops {
  position: absolute;
  top: 3px;
  right: 3px;
  display: flex;
  gap: 4px;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 4px;
  padding: 2px 4px;
  opacity: 0;
  transition: opacity 0.2s;

  .el-icon {
    color: #fff;
    font-size: 13px;
    cursor: pointer;

    &.disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }
  }
}

.hint-row .hint {
  font-size: 12px;
  color: #909399;
  line-height: 32px;
}

.save-bar {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color);
}
</style>
