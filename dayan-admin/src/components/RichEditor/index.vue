<script setup lang="ts">
/**
 * RichEditor 富文本编辑器封装（wangeditor-next）。
 *
 * - 图片/视频插入走统一上传链路（uploadFile），携带素材登记上下文自动入素材仓库。
 * - v-model 为 HTML 字符串；存量手写 HTML 直接回显。
 * - 内嵌资源使用服务端返回的 absoluteUrl（完整 URL）——agent/client rich-text
 *   渲染 HTML 时不做任何 URL 改写，相对路径在小程序端无法加载。
 */
import '@wangeditor-next/editor/dist/css/style.css'
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor-next/editor'
import { Editor, Toolbar } from '@wangeditor-next/editor-for-vue'
import { uploadFile } from '@/api/file'

defineOptions({ name: 'RichEditor' })

const props = withDefaults(
  defineProps<{
    modelValue?: string
    placeholder?: string
    height?: number
    module?: string
    /** 素材登记上下文（同 FileUploader） */
    registerAsset?: boolean
    assetParkCode?: string
    assetSourceType?: string
    assetSourceRef?: string
  }>(),
  {
    modelValue: '',
    placeholder: '请输入内容...',
    height: 400,
    module: 'rich'
  }
)

const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const editorRef = shallowRef<IDomEditor>()
// 注：fork 的 .d.ts 将 IToolbarConfig/IEditorConfig 字段声明为必填（运行时可选），
// 与 editor-for-vue 组件 props 的 Partial<…> 保持一致即可通过类型检查
const toolbarConfig: Partial<IToolbarConfig> = {}

const editorConfig: Partial<IEditorConfig> = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      // 服务端同事务登记素材仓库（assetType=1 图片）
      async customUpload(file: File, insertFn: (url: string, alt?: string, href?: string) => void) {
        const res = await uploadFile(file, props.module, {
          registerAsset: props.registerAsset,
          assetParkCode: props.assetParkCode,
          assetType: 1,
          assetSourceType: props.assetSourceType,
          assetSourceRef: props.assetSourceRef
        })
        insertFn(res.absoluteUrl || res.url, res.originalName, '')
      }
    },
    uploadVideo: {
      async customUpload(file: File, insertFn: (url: string, poster?: string) => void) {
        const res = await uploadFile(file, props.module, {
          registerAsset: props.registerAsset,
          assetParkCode: props.assetParkCode,
          assetType: 2,
          assetSourceType: props.assetSourceType,
          assetSourceRef: props.assetSourceRef
        })
        insertFn(res.absoluteUrl || res.url)
      }
    }
  }
}

const mode = 'default'
const valueHtml = ref(props.modelValue)

// 外部值 → 编辑器（避免回显死循环）
watch(
  () => props.modelValue,
  (v) => {
    if (v !== valueHtml.value) valueHtml.value = v || ''
  }
)
// 编辑器 → 外部
watch(valueHtml, (v) => {
  if (v !== props.modelValue) emit('update:modelValue', v)
})

function handleCreated(editor: IDomEditor) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  editorRef.value?.destroy()
})
</script>

<template>
  <div class="rich-editor">
    <Toolbar :editor="editorRef" :default-config="toolbarConfig" :mode="mode" class="rich-toolbar" />
    <Editor
      v-model="valueHtml"
      :default-config="editorConfig"
      :mode="mode"
      :style="{ height: height + 'px', overflowY: 'hidden' }"
      @on-created="handleCreated"
    />
  </div>
</template>

<style scoped lang="scss">
.rich-editor {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  /* 编辑器浮层抬高于 Element Plus dialog（默认 2000+），避免下拉菜单被遮挡 */
  z-index: 1;
}
.rich-toolbar {
  border-bottom: 1px solid #eee;
}
</style>

<style lang="scss">
/* wangEditor 全局浮层层级（非 scoped：浮层挂 body 时需要） */
.w-e-toolbar,
.w-e-bar,
.w-e-drop-panel,
.w-e-modal,
.w-e-select-list {
  z-index: 2400 !important;
}
</style>
