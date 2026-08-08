<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, VideoPlay, Document, Delete } from '@element-plus/icons-vue'
import { uploadFile } from '@/api/file'
import { formatFileUrl } from '@/utils/file'

defineOptions({ name: 'FileUploader' })

type FileType = 'image' | 'video' | 'file' | 'vr' | 'any'

const props = withDefaults(defineProps<{
  type?: FileType
  multiple?: boolean
  modelValue?: string | string[]
  accept?: string
  maxSize?: number // MB
  limit?: number
  disabled?: boolean
  module?: string
}>(), {
  type: 'any',
  multiple: false,
  modelValue: '',
  maxSize: 10,
  limit: 9,
  disabled: false,
  module: 'common'
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: string | string[]): void
}>()

const uploading = ref(false)

const defaultAccept = computed(() => {
  switch (props.type) {
    case 'image': return 'image/jpeg,image/png,image/gif,image/webp'
    case 'video': return 'video/mp4,video/webm'
    case 'file': return '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt'
    default: return ''
  }
})

const acceptVal = computed(() => props.accept || defaultAccept.value)

// 单文件模式：返回 string
const singleValue = computed(() => {
  if (props.multiple) return ''
  return typeof props.modelValue === 'string' ? props.modelValue : ''
})

// 多文件模式：返回 string[]
const multiValue = computed(() => {
  if (!props.multiple) return []
  if (Array.isArray(props.modelValue)) return props.modelValue
  return []
})

async function handleUpload(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return
  const file = input.files[0]
  // 前端预校验大小
  if (file.size > props.maxSize * 1024 * 1024) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`)
    input.value = ''
    return
  }
  uploading.value = true
  try {
    const res = await uploadFile(file, props.module)
    if (props.multiple) {
      const arr = [...multiValue.value, res.key]
      emit('update:modelValue', arr)
    } else {
      emit('update:modelValue', res.key)
    }
    ElMessage.success('上传成功')
  } catch {
    // request 拦截器已弹错误 toast
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function removeSingle() {
  emit('update:modelValue', '')
}

function removeMulti(index: number) {
  const arr = multiValue.value.filter((_, i) => i !== index)
  emit('update:modelValue', arr)
}

function fileName(key: string): string {
  const parts = key.split('/')
  return parts[parts.length - 1]
}
</script>

<template>
  <!-- 单图 -->
  <div v-if="type === 'image' && !multiple" class="uploader-single-image">
    <div v-if="singleValue" class="image-preview">
      <el-image :src="formatFileUrl(singleValue)" fit="cover" class="preview-img" :preview-src-list="[formatFileUrl(singleValue)]" />
      <div v-if="!disabled" class="image-actions">
        <label class="action-btn">替换
          <input type="file" :accept="acceptVal" class="hidden-input" @change="handleUpload" :disabled="uploading" />
        </label>
        <el-icon class="action-btn" @click="removeSingle"><Delete /></el-icon>
      </div>
    </div>
    <label v-else class="upload-placeholder" :class="{ disabled }">
      <el-icon><Plus /></el-icon>
      <span>{{ uploading ? '上传中...' : '上传图片' }}</span>
      <input type="file" :accept="acceptVal" class="hidden-input" @change="handleUpload" :disabled="uploading || disabled" />
    </label>
  </div>

  <!-- 多图 -->
  <div v-else-if="type === 'image' && multiple" class="uploader-multi-image">
    <div v-for="(key, i) in multiValue" :key="i" class="image-preview">
      <el-image :src="formatFileUrl(key)" fit="cover" class="preview-img" :preview-src-list="multiValue.map(formatFileUrl)" :initial-index="i" />
      <div v-if="!disabled" class="image-actions">
        <el-icon class="action-btn" @click="removeMulti(i)"><Delete /></el-icon>
      </div>
    </div>
    <label v-if="!disabled && multiValue.length < limit" class="upload-placeholder" :class="{ disabled }">
      <el-icon><Plus /></el-icon>
      <span>{{ uploading ? '上传中...' : '添加图片' }}</span>
      <input type="file" :accept="acceptVal" class="hidden-input" @change="handleUpload" :disabled="uploading || disabled" />
    </label>
  </div>

  <!-- 视频 -->
  <div v-else-if="type === 'video'" class="uploader-media">
    <div v-if="singleValue" class="media-item">
      <el-icon class="media-icon"><VideoPlay /></el-icon>
      <span class="media-name">{{ fileName(singleValue) }}</span>
      <el-icon v-if="!disabled" class="action-btn" @click="removeSingle"><Delete /></el-icon>
    </div>
    <label v-else class="upload-btn" :class="{ disabled }">
      <el-icon><Plus /></el-icon><span>{{ uploading ? '上传中...' : '上传视频' }}</span>
      <input type="file" :accept="acceptVal" class="hidden-input" @change="handleUpload" :disabled="uploading || disabled" />
    </label>
  </div>

  <!-- 文件 / VR / any -->
  <div v-else class="uploader-media">
    <div v-if="!multiple && singleValue" class="media-item">
      <el-icon class="media-icon"><Document /></el-icon>
      <span class="media-name">{{ fileName(singleValue) }}</span>
      <el-icon v-if="!disabled" class="action-btn" @click="removeSingle"><Delete /></el-icon>
    </div>
    <div v-for="(key, i) in (multiple ? multiValue : [])" :key="i" class="media-item">
      <el-icon class="media-icon"><Document /></el-icon>
      <span class="media-name">{{ fileName(key) }}</span>
      <el-icon v-if="!disabled" class="action-btn" @click="removeMulti(i)"><Delete /></el-icon>
    </div>
    <label v-if="!disabled && (!multiple || multiValue.length < limit)" class="upload-btn" :class="{ disabled }">
      <el-icon><Plus /></el-icon><span>{{ uploading ? '上传中...' : '点击上传' }}</span>
      <input type="file" :accept="acceptVal" class="hidden-input" @change="handleUpload" :disabled="uploading || disabled" />
    </label>
  </div>
</template>

<style scoped>
.hidden-input { display: none; }
.upload-placeholder, .upload-btn, .action-btn { cursor: pointer; }
.upload-placeholder.disabled, .upload-btn.disabled { cursor: not-allowed; opacity: 0.5; }
.upload-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  width: 80px; height: 80px; border: 1px dashed #d9d9d9; border-radius: 6px;
  color: #999; font-size: 12px; gap: 4px;
}
.upload-placeholder:hover { border-color: #409eff; color: #409eff; }
.image-preview { position: relative; width: 80px; height: 80px; }
.preview-img { width: 80px; height: 80px; border-radius: 6px; border: 1px solid #ebeef5; }
.image-actions {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  gap: 8px; background: rgba(0,0,0,0.5); border-radius: 6px; opacity: 0; transition: opacity 0.2s;
}
.image-preview:hover .image-actions { opacity: 1; }
.action-btn { color: #fff; font-size: 16px; }
.uploader-multi-image, .uploader-single-image, .uploader-media { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.media-item { display: flex; align-items: center; gap: 6px; padding: 4px 8px; border: 1px solid #ebeef5; border-radius: 4px; font-size: 13px; }
.media-icon { color: #409eff; }
.media-name { max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.upload-btn { display: inline-flex; align-items: center; gap: 4px; color: #409eff; font-size: 13px; }
</style>
