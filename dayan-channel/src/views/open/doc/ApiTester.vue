<script setup lang="ts">
/**
 * 接口测试面板。
 * 填环境/AppKey/参数 → 实时生成 curl/HTTP/JSON 代码视图 + 一键复制。
 * 不发真实请求（接口建设中）；X-Sign 用占位（前端不真算 HMAC）。
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ENV_OPTIONS } from './apis'
import type { ApiItem } from './apis'

const props = defineProps<{ api: ApiItem }>()

const env = ref('test')
const appKey = ref('your_app_key')
const timestamp = ref(Date.now())
const nonce = ref(Math.random().toString(36).slice(2, 10))
const paramValues = ref<Record<string, string>>({})
const customHeaders = ref<{ name: string; value: string }[]>([])
const customQuerys = ref<{ name: string; value: string }[]>([])
const codeLang = ref<'curl' | 'http' | 'json'>('curl')

// 按 location 拆分参数
const pathParams = computed(() => (props.api.params || []).filter((p) => p.location === 'path'))
const queryParams = computed(() => (props.api.params || []).filter((p) => p.location === 'query'))
const headerParams = computed(() => [
  ...(props.api.headers || []),
  ...(props.api.params || []).filter((p) => p.location === 'header')
])
const bodyParams = computed(() => (props.api.params || []).filter((p) => p.location === 'body'))

// 初始化默认值（api 变化时重新预填）
function initDefaults() {
  paramValues.value = {}
  customHeaders.value = []
  customQuerys.value = []
  for (const p of [...(props.api.params || []), ...(props.api.headers || [])]) {
    if (p.default) paramValues.value[p.name] = p.default
  }
}
watch(() => props.api.id, initDefaults, { immediate: true })

const baseUrl = computed(() => ENV_OPTIONS.find((e) => e.value === env.value)?.baseUrl || '')

// 实际 path（替换 {param} 占位）
const actualPath = computed(() => {
  let p = props.api.path
  for (const param of props.api.params || []) {
    if (param.location === 'path') {
      const val = paramValues.value[param.name] || `{${param.name}}`
      p = p.replace(`{${param.name}}`, val)
    }
  }
  return p
})

// query 字符串
const queryString = computed(() => {
  const items: string[] = []
  for (const param of props.api.params || []) {
    if (param.location === 'query') {
      const val = paramValues.value[param.name]
      if (val) items.push(`${param.name}=${encodeURIComponent(val)}`)
    }
  }
  for (const c of customQuerys.value) {
    if (c.name && c.value) items.push(`${c.name}=${encodeURIComponent(c.value)}`)
  }
  return items.length ? '?' + items.join('&') : ''
})

const fullUrl = computed(() => baseUrl.value + actualPath.value + queryString.value)

// 认证头（除 token 接口外都注入）
const needsAuth = computed(() => props.api.id !== 'token')
const authHeaders = computed(() => {
  if (!needsAuth.value) return []
  return [
    { name: 'X-App-Key', value: appKey.value },
    { name: 'X-Timestamp', value: String(timestamp.value) },
    { name: 'X-Nonce', value: nonce.value },
    { name: 'X-Sign', value: 'a3f8e2c1（示例，实际需后端用 AppSecret 计算 HMAC-SHA256）' }
  ]
})

// 额外请求头（api.headers + 自定义）
const extraHeaders = computed(() => {
  const list = (props.api.headers || []).map((h) => ({
    name: h.name,
    value: paramValues.value[h.name] || h.default || ''
  }))
  return [...list, ...customHeaders.value.filter((c) => c.name)]
})

// 请求体
const requestBody = computed(() => {
  const custom: Record<string, string> = {}
  for (const p of bodyParams.value) {
    const val = paramValues.value[p.name]
    if (val) custom[p.name] = val
  }
  if (Object.keys(custom).length) return JSON.stringify(custom, null, 2)
  return props.api.requestExample || ''
})

const hasBody = computed(() => props.api.method === 'POST' || props.api.method === 'PUT')

// 代码生成
const codeContent = computed(() => {
  const allHeaders = [...authHeaders.value, ...extraHeaders.value]
  if (codeLang.value === 'curl') {
    let cmd = `curl -X ${props.api.method} '${fullUrl.value}'`
    for (const h of allHeaders) {
      if (h.value) cmd += ` \\\n  -H '${h.name}: ${h.value}'`
    }
    if (hasBody.value && requestBody.value) {
      cmd += ` \\\n  -d '${requestBody.value.replace(/\n/g, ' ')}'`
    }
    return cmd
  }
  if (codeLang.value === 'http') {
    const url = new URL(fullUrl.value)
    let txt = `${props.api.method} ${url.pathname}${url.search} HTTP/1.1\n`
    txt += `Host: ${url.host}\n`
    for (const h of allHeaders) {
      if (h.value) txt += `${h.name}: ${h.value}\n`
    }
    if (hasBody.value && requestBody.value) {
      txt += `Content-Type: application/json\n\n${requestBody.value}`
    }
    return txt
  }
  // json
  return hasBody.value ? requestBody.value : '（GET 请求无请求体）'
})

async function copyCode() {
  try {
    await navigator.clipboard.writeText(codeContent.value)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败，请手动选择复制')
  }
}

function regenNonce() {
  nonce.value = Math.random().toString(36).slice(2, 10)
  timestamp.value = Date.now()
}
</script>

<template>
  <div class="api-tester">
    <h3>接口测试</h3>
    <el-alert type="warning" :closable="false" show-icon
      title="接口建设中，此面板仅展示请求构造，不发送真实请求" />

    <!-- 环境与凭证 -->
    <div class="tester-row">
      <div class="form-item">
        <label>环境</label>
        <el-select v-model="env" style="width: 160px">
          <el-option v-for="e in ENV_OPTIONS" :key="e.value" :label="e.label" :value="e.value" />
        </el-select>
      </div>
      <div class="form-item">
        <label>AppKey</label>
        <el-input v-model="appKey" style="width: 200px" />
      </div>
      <div class="form-item" v-if="needsAuth">
        <el-button text @click="regenNonce">重新生成时间戳/Nonce</el-button>
      </div>
    </div>

    <!-- path 参数单独区 -->
    <div v-if="pathParams.length" class="param-section">
      <p class="section-title">路径参数</p>
      <div v-for="p in pathParams" :key="p.name" class="param-row">
        <code class="param-name">{{ p.name }}</code>
        <span class="param-required" v-if="p.required">*</span>
        <el-input v-model="paramValues[p.name]" :placeholder="p.description" style="flex: 1" />
      </div>
    </div>

    <!-- 其余参数按 location 分 tab -->
    <el-tabs v-if="queryParams.length || headerParams.length || bodyParams.length">
      <el-tab-pane v-if="queryParams.length" label="Query 参数">
        <div v-for="p in queryParams" :key="p.name" class="param-row">
          <code class="param-name">{{ p.name }}</code>
          <span class="param-required" v-if="p.required">*</span>
          <el-input v-model="paramValues[p.name]" :placeholder="p.description" style="flex: 1" />
        </div>
        <div v-for="(c, i) in customQuerys" :key="'cq' + i" class="param-row">
          <el-input v-model="c.name" placeholder="参数名" style="width: 140px" />
          <el-input v-model="c.value" placeholder="参数值" style="flex: 1" />
          <el-button text @click="customQuerys.splice(i, 1)">✕</el-button>
        </div>
        <el-button text @click="customQuerys.push({ name: '', value: '' })">+ 添加自定义参数</el-button>
      </el-tab-pane>

      <el-tab-pane v-if="headerParams.length" label="请求头">
        <div v-for="p in headerParams" :key="p.name" class="param-row">
          <code class="param-name">{{ p.name }}</code>
          <span class="param-required" v-if="p.required">*</span>
          <el-input v-model="paramValues[p.name]" :placeholder="p.description" style="flex: 1" />
        </div>
        <div v-for="(c, i) in customHeaders" :key="'ch' + i" class="param-row">
          <el-input v-model="c.name" placeholder="Header 名" style="width: 140px" />
          <el-input v-model="c.value" placeholder="Header 值" style="flex: 1" />
          <el-button text @click="customHeaders.splice(i, 1)">✕</el-button>
        </div>
        <el-button text @click="customHeaders.push({ name: '', value: '' })">+ 添加自定义请求头</el-button>
      </el-tab-pane>

      <el-tab-pane v-if="bodyParams.length" label="Body">
        <div v-for="p in bodyParams" :key="p.name" class="param-row">
          <code class="param-name">{{ p.name }}</code>
          <span class="param-required" v-if="p.required">*</span>
          <el-input v-model="paramValues[p.name]" :placeholder="p.description" style="flex: 1" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 代码视图 -->
    <div class="code-section">
      <div class="code-header">
        <span class="section-title">生成的请求</span>
        <div class="code-actions">
          <el-select v-model="codeLang" size="small" style="width: 100px">
            <el-option label="curl" value="curl" />
            <el-option label="HTTP" value="http" />
            <el-option label="JSON" value="json" />
          </el-select>
          <el-button size="small" @click="copyCode">
            <el-icon><CopyDocument /></el-icon>
            <span>复制</span>
          </el-button>
        </div>
      </div>
      <pre class="code-block"><code>{{ codeContent }}</code></pre>
    </div>
  </div>
</template>

<style scoped lang="scss">
.api-tester {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.tester-row {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
}
.form-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  label {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}
.param-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.section-title {
  font-weight: 600;
  font-size: 14px;
  margin: 0 0 4px 0;
}
.param-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.param-name {
  background: var(--el-fill-color-light);
  padding: 4px 8px;
  border-radius: 3px;
  font-size: 13px;
  min-width: 100px;
}
.param-required {
  color: var(--el-color-danger);
  font-weight: bold;
}
.code-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.code-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.code-block {
  background: var(--el-fill-color-darker);
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 0;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
