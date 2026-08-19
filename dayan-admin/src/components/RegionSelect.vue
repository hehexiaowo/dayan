<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listProvinces, listRegionChildren, type Region } from '@/api/region'

/**
 * 省市区三级联动下拉。
 *
 * 对外通过三个 v-model 绑定 provinceCode / cityCode / districtCode。
 * 行为：
 * - onMounted 拉省级列表
 * - 选省 → 拉市级，清空市/区
 * - 选市 → 拉区级，清空区
 * - 编辑回显：外部赋入已有 code 时，watch 逐级加载下级列表
 */
defineOptions({ name: 'RegionSelect' })

const props = defineProps<{
  provinceCode?: string
  cityCode?: string
  districtCode?: string
}>()

const emit = defineEmits<{
  (e: 'update:provinceCode', v: string): void
  (e: 'update:cityCode', v: string): void
  (e: 'update:districtCode', v: string): void
}>()

const provinceList = ref<Region[]>([])
const cityList = ref<Region[]>([])
const districtList = ref<Region[]>([])

/** 拉省级（只拉一次） */
async function loadProvinces() {
  if (provinceList.value.length) return
  try {
    provinceList.value = await listProvinces()
  } catch {
    ElMessage.error('省份列表加载失败')
  }
}

/** 拉市级（按省码） */
async function loadCities(parentCode: string) {
  if (!parentCode) {
    cityList.value = []
    return
  }
  try {
    cityList.value = await listRegionChildren(parentCode)
  } catch {
    ElMessage.error('城市列表加载失败')
  }
}

/** 拉区级（按市码） */
async function loadDistricts(parentCode: string) {
  if (!parentCode) {
    districtList.value = []
    return
  }
  try {
    districtList.value = await listRegionChildren(parentCode)
  } catch {
    ElMessage.error('区县列表加载失败')
  }
}

/** 选省：更新省码，清空市/区，拉市级 */
function onProvinceChange(code: string) {
  emit('update:provinceCode', code)
  emit('update:cityCode', '')
  emit('update:districtCode', '')
  cityList.value = []
  districtList.value = []
  loadCities(code)
}

/** 选市：更新市码，清空区，拉区级 */
function onCityChange(code: string) {
  emit('update:cityCode', code)
  emit('update:districtCode', '')
  districtList.value = []
  loadDistricts(code)
}

function onDistrictChange(code: string) {
  emit('update:districtCode', code)
}

// 初始化：拉省级
loadProvinces()

// 编辑回显：外部赋入 provinceCode 后拉市级
watch(
  () => props.provinceCode,
  (code) => {
    if (code) loadCities(code)
  },
  { immediate: true }
)

// 编辑回显：外部赋入 cityCode 后拉区级
watch(
  () => props.cityCode,
  (code) => {
    if (code) loadDistricts(code)
  },
  { immediate: true }
)
</script>

<template>
  <div class="region-select">
    <el-select
      :model-value="provinceCode"
      placeholder="请选择省"
      clearable
      filterable
      style="width: 33%"
      @update:model-value="onProvinceChange($event ?? '')"
    >
      <el-option
        v-for="p in provinceList"
        :key="p.regionCode"
        :label="p.regionName"
        :value="p.regionCode"
      />
    </el-select>
    <el-select
      :model-value="cityCode"
      placeholder="请选择市"
      clearable
      filterable
      :disabled="!provinceCode"
      style="width: 33%"
      @update:model-value="onCityChange($event ?? '')"
    >
      <el-option
        v-for="c in cityList"
        :key="c.regionCode"
        :label="c.regionName"
        :value="c.regionCode"
      />
    </el-select>
    <el-select
      :model-value="districtCode"
      placeholder="请选择区/县"
      clearable
      filterable
      :disabled="!cityCode"
      style="width: 33%"
      @update:model-value="onDistrictChange($event ?? '')"
    >
      <el-option
        v-for="d in districtList"
        :key="d.regionCode"
        :label="d.regionName"
        :value="d.regionCode"
      />
    </el-select>
  </div>
</template>

<style scoped lang="scss">
.region-select {
  display: flex;
  gap: 8px;
  width: 100%;
}
</style>
