<script setup lang="ts">
/**
 * AssetPicker 素材选择弹窗：从系统素材仓库（system_asset）选图片/视频，回填资源地址。
 * v-model 控制显隐；confirm 后 emit('select', keys)（OSS key 或外链 URL 数组，调用方自行拼 URL）。
 */
import { computed, ref, watch } from 'vue'
import { VideoPlay } from '@element-plus/icons-vue'
import { pageAssets } from '@/api/system-asset'
import { useDictOptions } from '@/composables/useDict'
import { REF_TYPE1_OPTIONS } from '@/types/asset'
import { formatFileUrl } from '@/utils/file'
import type { SystemAsset } from '@/types/asset'

defineOptions({ name: 'AssetPicker' })

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    /** 素材类型：image=图片 assetType=1；video=视频 assetType=2 */
    type?: 'image' | 'video'
    multiple?: boolean
    limit?: number
    /** 可选：限定类型1（业务维度，如 park） */
    refType1?: string
    /** 可选：限定关联编码（业务实体编码，如机构编码） */
    refCode?: string
  }>(),
  { type: 'image', multiple: false, limit: 9 }
)

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'select', keys: string[]): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

const assetType = computed(() => (props.type === 'video' ? 2 : 1))

/** 类型1 选项：字典 asset_ref_type1 驱动，字典为空时回退内置常量 */
const { options: refType1DictOptions } = useDictOptions('asset_ref_type1')
const refType1Items = computed(() => {
  if (refType1DictOptions.value.length) {
    return refType1DictOptions.value.map((d) => ({ label: d.dictName, value: d.dictCode }))
  }
  return REF_TYPE1_OPTIONS.map((o) => ({ label: o.label, value: o.value }))
})
const loading = ref(false)
const records = ref<SystemAsset[]>([])
const total = ref(0)
const query = ref({ current: 1, size: 12, keyword: '', refType1: props.refType1 || undefined, refCode: props.refCode || undefined })
const selected = ref<string[]>([])

async function load() {
  loading.value = true
  try {
    const res = await pageAssets({
      ...query.value,
      assetType: assetType.value,
      status: 1
    })
    records.value = res.records ?? []
    total.value = res.total ?? 0
  } catch {
    // request 拦截器已弹错误 toast，这里仅静默处理
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      selected.value = []
      query.value.current = 1
      load()
    }
  }
)

function toggle(key: string) {
  if (!props.multiple) {
    selected.value = [key]
    return
  }
  const i = selected.value.indexOf(key)
  if (i >= 0) {
    selected.value = selected.value.filter((k) => k !== key)
  } else if (selected.value.length < props.limit) {
    selected.value = [...selected.value, key]
  }
}

function onCurrentChange(page: number) {
  query.value.current = page
  load()
}

function onConfirm() {
  emit('select', selected.value)
  visible.value = false
}
</script>

<template>
  <el-dialog v-model="visible" :title="type === 'video' ? '从素材仓库选择视频' : '从素材仓库选择图片'" width="720px">
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="名称">
        <el-input
          v-model="query.keyword"
          placeholder="名称/URL 关键字"
          clearable
          style="width: 200px"
          @keyup.enter="((query.current = 1), load())"
        />
      </el-form-item>
      <el-form-item v-if="!refType1" label="类型1">
        <el-select v-model="query.refType1" placeholder="全部" clearable style="width: 120px" @change="((query.current = 1), load())">
          <el-option v-for="o in refType1Items" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!refCode" label="关联编码">
        <el-input v-model="query.refCode" placeholder="空=全部" clearable style="width: 150px" @keyup.enter="((query.current = 1), load())" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="((query.current = 1), load())">查询</el-button>
      </el-form-item>
    </el-form>

    <div v-loading="loading" class="picker-grid">
      <div
        v-for="item in records"
        :key="item.id"
        class="picker-cell"
        :class="{ selected: selected.includes(item.assetUrl || '') }"
        @click="toggle(item.assetUrl || '')"
      >
        <el-image
          v-if="type === 'image'"
          :src="formatFileUrl(item.assetUrl)"
          fit="cover"
          class="picker-img"
        />
        <div v-else class="picker-video">
          <el-icon><VideoPlay /></el-icon>
          <span class="picker-name">{{ item.assetName || '--' }}</span>
        </div>
        <div class="picker-label">{{ item.assetName || '--' }}</div>
      </div>
      <el-empty v-if="!loading && records.length === 0" description="暂无素材，可先在素材仓库上传" />
    </div>

    <el-pagination
      :current-page="query.current"
      :page-size="query.size"
      :total="total"
      layout="prev, pager, next, total"
      background
      style="margin-top: 12px; justify-content: flex-end"
      @current-change="onCurrentChange"
    />

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="selected.length === 0" @click="onConfirm">
        确定{{ multiple ? `（已选 ${selected.length}/${limit}）` : '' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.picker-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  min-height: 200px;
  max-height: 420px;
  overflow-y: auto;
}
.picker-cell {
  border: 2px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  position: relative;
  &.selected {
    border-color: var(--el-color-primary);
  }
}
.picker-img {
  width: 100%;
  height: 90px;
  display: block;
}
.picker-video {
  height: 90px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: #f5f7fa;
  color: var(--el-color-primary);
}
.picker-name {
  font-size: 12px;
  max-width: 90%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.picker-label {
  font-size: 12px;
  color: #606266;
  padding: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
