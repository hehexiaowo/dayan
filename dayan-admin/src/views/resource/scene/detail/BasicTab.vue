<script setup lang="ts">
/**
 * 场景详情页 - 基本信息 tab。
 *
 * 只读展示 SceneInfo 关键字段（el-descriptions），提供"编辑"按钮打开 el-dialog + el-form
 * 修改主表（提交 updateScene）。复用主列表页编辑表单字段集。
 *
 * 注：场景状态机（提交审核/上架/下架/满期等）在主列表页操作，本 tab 不重复放置状态机按钮。
 * imageUrls / highlight / notice 是 JSON 字符串或逗号分隔，编辑用 textarea 原文编辑（不做复杂解析）。
 */
import { computed, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getScene, updateScene } from '@/api/scene'
import {
  SCENE_TYPE_OPTIONS,
  SCENE_STATUS_OPTIONS,
  AUDIT_STATUS_OPTIONS,
  SceneType
} from '@/types/scene'
import type { SceneInfo } from '@/types/scene'
import FileUploader from '@/components/FileUploader/index.vue'
import { formatFileUrl } from '@/utils/file'

const props = defineProps<{
  /** 场景编码（从详情页路由 prop 带入） */
  sceneCode: string
}>()

const loading = ref(false)
const sceneInfo = ref<SceneInfo | null>(null)

async function loadDetail() {
  if (!props.sceneCode) return
  loading.value = true
  try {
    sceneInfo.value = await getScene(props.sceneCode)
  } catch {
    sceneInfo.value = null
  } finally {
    loading.value = false
  }
}

loadDetail()

// ---------- 辅助渲染 ----------
function sceneTypeLabel(t?: number): string {
  const found = SCENE_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : '--'
}

function sceneStatusLabel(s?: number): string {
  const found = SCENE_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : '--'
}

function auditStatusLabel(s?: number): string {
  const found = AUDIT_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : '--'
}

function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}

/** 图集列表：兼容 JSON 数组字符串与逗号分隔两种存量格式。 */
function imageUrlList(raw?: string): string[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) return parsed.filter((x) => typeof x === 'string')
  } catch {
    // 非 JSON，按逗号分隔
  }
  return raw.split(',').map((s) => s.trim()).filter(Boolean)
}

// ---------- 编辑弹窗 ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<SceneInfo>({
  sceneCode: undefined,
  sceneName: '',
  sceneType: SceneType.VISIT,
  parkCode: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  sceneDescription: '',
  coverImage: '',
  imageUrls: '',
  videoUrl: '',
  capacity: undefined,
  durationHours: undefined,
  minPerson: undefined,
  maxPerson: undefined,
  originalPrice: undefined,
  salePrice: undefined,
  priceUnit: '',
  isFree: 0,
  targetAudience: '',
  highlight: '',
  notice: '',
  sortOrder: 0,
  remark: ''
})

const rules: FormRules<SceneInfo> = {
  sceneName: [{ required: true, message: '请输入场景名称', trigger: 'blur' }],
  sceneType: [{ required: true, message: '请选择场景类型', trigger: 'change' }]
}

/** imageUrls：后端是 string（JSON 数组，兼容逗号分隔存量），FileUploader 多图用 string[] */
const imageUrlsModel = computed<string[]>({
  get() {
    const raw = form.imageUrls
    if (!raw) return []
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) return parsed.filter((x) => typeof x === 'string')
    } catch {
      // 非 JSON，按逗号分隔
    }
    return raw.split(',').map((s) => s.trim()).filter(Boolean)
  },
  set(val: string[]) {
    form.imageUrls = val.length > 0 ? JSON.stringify(val) : ''
  }
})

function openEdit() {
  if (!sceneInfo.value) return
  Object.assign(form, {
    sceneCode: sceneInfo.value.sceneCode,
    sceneName: sceneInfo.value.sceneName ?? '',
    sceneType: sceneInfo.value.sceneType ?? SceneType.VISIT,
    parkCode: sceneInfo.value.parkCode ?? '',
    provinceCode: sceneInfo.value.provinceCode ?? '',
    cityCode: sceneInfo.value.cityCode ?? '',
    districtCode: sceneInfo.value.districtCode ?? '',
    address: sceneInfo.value.address ?? '',
    sceneDescription: sceneInfo.value.sceneDescription ?? '',
    coverImage: sceneInfo.value.coverImage ?? '',
    imageUrls: sceneInfo.value.imageUrls ?? '',
    videoUrl: sceneInfo.value.videoUrl ?? '',
    capacity: sceneInfo.value.capacity,
    durationHours: sceneInfo.value.durationHours,
    minPerson: sceneInfo.value.minPerson,
    maxPerson: sceneInfo.value.maxPerson,
    originalPrice: sceneInfo.value.originalPrice,
    salePrice: sceneInfo.value.salePrice,
    priceUnit: sceneInfo.value.priceUnit ?? '',
    isFree: sceneInfo.value.isFree ?? 0,
    targetAudience: sceneInfo.value.targetAudience ?? '',
    highlight: sceneInfo.value.highlight ?? '',
    notice: sceneInfo.value.notice ?? '',
    sortOrder: sceneInfo.value.sortOrder ?? 0,
    remark: sceneInfo.value.remark ?? ''
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (!form.sceneCode) return
  submitLoading.value = true
  try {
    await updateScene(form.sceneCode, form)
    ElMessage.success('修改成功')
    dialogVisible.value = false
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

/** 暴露刷新方法，供详情页外部刷新 */
defineExpose({ loadDetail })
</script>

<template>
  <div v-loading="loading">
    <template v-if="sceneInfo">
      <div class="basic-toolbar">
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑基本信息</el-button>
        </div>
      </div>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="场景编码">{{ sceneInfo.sceneCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="场景名称">{{ sceneInfo.sceneName }}</el-descriptions-item>
        <el-descriptions-item label="场景类型">
          {{ sceneTypeLabel(sceneInfo.sceneType) }}
        </el-descriptions-item>
        <el-descriptions-item label="关联机构">{{ sceneInfo.parkCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="场景状态">
          {{ sceneStatusLabel(sceneInfo.sceneStatus) }}
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">
          {{ auditStatusLabel(sceneInfo.auditStatus) }}
        </el-descriptions-item>
        <el-descriptions-item label="地址" :span="3">{{ sceneInfo.address ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="容量">{{ sceneInfo.capacity ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="时长(小时)">{{ sceneInfo.durationHours ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="排序号">{{ sceneInfo.sortOrder ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="最少人数">{{ sceneInfo.minPerson ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="最多人数">{{ sceneInfo.maxPerson ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="是否免费">
          {{ sceneInfo.isFree === 1 ? '免费' : '收费' }}
        </el-descriptions-item>
        <el-descriptions-item label="原价">{{ sceneInfo.originalPrice ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="售价">{{ sceneInfo.salePrice ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="价格单位">{{ sceneInfo.priceUnit ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="目标人群" :span="3">
          {{ sceneInfo.targetAudience ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="封面图" :span="3">
          <el-image
            v-if="sceneInfo.coverImage"
            :src="formatFileUrl(sceneInfo.coverImage)"
            :preview-src-list="[formatFileUrl(sceneInfo.coverImage)]"
            fit="cover"
            style="width: 80px; height: 80px"
          />
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="图集" :span="3">
          <div v-if="imageUrlList(sceneInfo.imageUrls).length" class="image-list">
            <el-image
              v-for="(url, i) in imageUrlList(sceneInfo.imageUrls)"
              :key="url"
              :src="formatFileUrl(url)"
              :preview-src-list="imageUrlList(sceneInfo.imageUrls).map(formatFileUrl)"
              :initial-index="i"
              fit="cover"
              class="detail-img"
            />
          </div>
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="视频链接" :span="3">
          <el-link
            v-if="sceneInfo.videoUrl"
            type="primary"
            :href="formatFileUrl(sceneInfo.videoUrl)"
            target="_blank"
          >
            {{ sceneInfo.videoUrl }}
          </el-link>
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="活动亮点" :span="3">
          <span class="multiline">{{ sceneInfo.highlight ?? '--' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="参与须知" :span="3">
          <span class="multiline">{{ sceneInfo.notice ?? '--' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="场景描述" :span="3">
          <span class="multiline">{{ sceneInfo.sceneDescription ?? '--' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">
          <span class="multiline">{{ sceneInfo.remark ?? '--' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(sceneInfo.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">{{ formatDate(sceneInfo.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <el-empty v-else-if="!loading" description="未加载到场景信息" />

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑场景基本信息"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="场景编码">
              <el-input v-model="form.sceneCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景名称" prop="sceneName">
              <el-input v-model="form.sceneName" placeholder="场景名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景类型" prop="sceneType">
              <el-select v-model="form.sceneType" placeholder="场景类型" style="width: 100%">
                <el-option v-for="o in SCENE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联机构">
              <el-input v-model="form.parkCode" placeholder="养老机构编码（parkCode）" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="省级编码">
              <el-input v-model="form.provinceCode" placeholder="省级编码" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="市级编码">
              <el-input v-model="form.cityCode" placeholder="市级编码" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区县编码">
              <el-input v-model="form.districtCode" placeholder="区县编码" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="form.address" placeholder="详细地址" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="场景描述">
              <el-input v-model="form.sceneDescription" type="textarea" :rows="3" placeholder="场景描述" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <FileUploader v-model="form.coverImage" type="image" module="scene" register-asset asset-ref-type1="scene" :asset-ref-code="props.sceneCode" asset-ref-type2="scene" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图集">
              <FileUploader v-model="imageUrlsModel" type="image" multiple module="scene" register-asset asset-ref-type1="scene" :asset-ref-code="props.sceneCode" asset-ref-type2="scene" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="视频链接">
              <el-input v-model="form.videoUrl" placeholder="宣传视频 URL" maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="容量">
              <el-input-number v-model="form.capacity" :min="0" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时长(小时)">
              <el-input-number v-model="form.durationHours" :min="0" :max="9999" :precision="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最少人数">
              <el-input-number v-model="form.minPerson" :min="0" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最多人数">
              <el-input-number v-model="form.maxPerson" :min="0" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否免费">
              <el-switch v-model="form.isFree" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原价">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="售价">
              <el-input-number v-model="form.salePrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="价格单位">
              <el-input v-model="form.priceUnit" placeholder="元/人 等" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="目标人群">
              <el-input v-model="form.targetAudience" placeholder="目标人群" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="活动亮点">
              <el-input v-model="form.highlight" type="textarea" :rows="2" placeholder="活动亮点（逗号分隔或原文）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="参与须知">
              <el-input v-model="form.notice" type="textarea" :rows="2" placeholder="参与须知" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="内部备注（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.basic-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;

  .toolbar-actions {
    display: flex;
    gap: 8px;
    margin-left: auto;
  }
}
.url-cell {
  word-break: break-all;
  color: var(--el-text-color-secondary);
}
.multiline {
  white-space: pre-wrap;
  word-break: break-all;
}
.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.detail-img {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}
</style>
