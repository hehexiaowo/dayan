<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { type FormInstance, type FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageConfigs, createConfig, updateConfig, deleteConfig } from '@/api/config'
import {
  type SystemConfig,
  type ConfigQuery,
  type ConfigValueType,
  CONFIG_VALUE_TYPE_OPTIONS,
  CONFIG_ENV_OPTIONS,
  CONFIG_GROUP_OPTIONS
} from '@/types/config'

/**
 * 系统配置管理页（分组卡片式，外部平台核心凭据仓库：oss / map / sms / payment）。
 *
 * - 每个配置分组一张卡片（标题 + 说明 + 配置项清单），替代原平铺表格；
 * - 卡片内按「名称 + Key + 值 + 操作」一行一项，敏感值双重脱敏（后端已置 ******）；
 * - 编辑敏感值留空提交 = 保持原值不变（后端 update 语义），填写即覆盖；
 * - 已知凭据分组恒展示（空组提示未配置），自定义分组自动追加卡片。
 */

/** 已知凭据分组的卡片元数据（展示顺序即卡片顺序） */
const GROUP_CARDS = [
  {
    value: 'map',
    label: '地图服务',
    desc: '天地图开放平台。前端暴露型 Key，agent H5 运行时拉取（/agent-api/v1/config/map-key），改后全端生效'
  },
  {
    value: 'oss',
    label: '对象存储（MinIO / S3）',
    desc: '文件上传与访问凭据。各服务 60 秒内热生效（无需重启）；配置缺失的键回退 MINIO_* 环境变量'
  },
  {
    value: 'sms',
    label: '短信平台',
    desc: '验证码短信通道。当前 mock=开发态日志验证码；接入阿里云后由真实实现读取下列键'
  },
  {
    value: 'payment',
    label: '支付渠道',
    desc: '微信 / 支付宝收单凭据（预留槽位，接入支付网关后消费）'
  }
] as const

// ---------------- 数据加载与分组 ----------------
const loading = ref(false)
const allRows = ref<SystemConfig[]>([])
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    const result = await pageConfigs({ current: 1, size: 500, configGroup: '', configKey: '' } as ConfigQuery)
    allRows.value = [...result.records].sort(
      (a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || (a.configKey || '').localeCompare(b.configKey || '')
    )
  } finally {
    loading.value = false
  }
}

/** 卡片视图模型：分组元数据 + 该组配置项（按关键字前端过滤） */
interface GroupCard {
  value: string
  label: string
  desc: string
  items: SystemConfig[]
}

const groupCards = computed<GroupCard[]>(() => {
  const kw = keyword.value.trim().toLowerCase()
  const match = (r: SystemConfig) =>
    !kw ||
    (r.configKey || '').toLowerCase().includes(kw) ||
    (r.configName || '').toLowerCase().includes(kw)

  const cards: GroupCard[] = GROUP_CARDS.map((g) => ({
    value: g.value,
    label: g.label,
    desc: g.desc,
    items: allRows.value.filter((r) => r.configGroup === g.value && match(r))
  }))

  // 自定义分组（已知凭据组之外的存量/新增行）自动追加卡片
  const known = new Set<string>(GROUP_CARDS.map((g) => g.value))
  const extras = [...new Set(allRows.value.map((r) => r.configGroup).filter((g) => g && !known.has(g)))]
  for (const g of extras) {
    const opt = CONFIG_GROUP_OPTIONS.find((o) => o.value === g)
    cards.push({
      value: g,
      label: opt ? opt.label : `分组 ${g}`,
      desc: '自定义配置分组',
      items: allRows.value.filter((r) => r.configGroup === g && match(r))
    })
  }
  // 无匹配关键字的空卡片不占位（已知组在无关键字时恒展示）
  return kw ? cards.filter((c) => c.items.length > 0) : cards
})

const totalCount = computed(() => allRows.value.length)

// ---------------- 值展示 ----------------

/** 列表脱敏显示：isSecret=1 时不展示后端返回值（双重保险，后端已统一脱敏 ******） */
function displayValue(row: SystemConfig): string {
  if (row.isSecret === 1) return '******'
  return row.configValue ?? ''
}

/** 未配置判断看原始值（空值敏感项显示「未配置」而非 ******，避免误读为已配置） */
function isUnset(row: SystemConfig): boolean {
  return !(row.configValue ?? '').trim()
}

// ---------------- 弹窗与表单 ----------------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()
const submitting = ref(false)

/** 表单默认值（新增时使用，可预置分组） */
function defaultForm(group = 'system'): SystemConfig {
  return {
    configGroup: group,
    configKey: '',
    configValue: '',
    valueType: 'string',
    env: 'prod',
    scope: 'global',
    organCode: null,
    userCode: null,
    configName: '',
    description: null,
    isSecret: 0,
    isRuntime: 1,
    sortOrder: 0
  }
}

const form = reactive<SystemConfig>(defaultForm())

const rules: FormRules<SystemConfig> = {
  configGroup: [{ required: true, message: '请选择配置分组', trigger: 'change' }],
  configKey: [
    { required: true, message: '请输入配置 Key', trigger: 'blur' },
    { max: 100, message: '配置 Key 长度不能超过 100', trigger: 'blur' }
  ],
  configName: [
    { required: true, message: '请输入配置名称', trigger: 'blur' },
    { max: 100, message: '配置名称长度不能超过 100', trigger: 'blur' }
  ],
  valueType: [{ required: true, message: '请选择值类型', trigger: 'change' }],
  env: [{ required: true, message: '请选择环境', trigger: 'change' }]
}

/** 打开新增弹窗（卡片入口可预置分组） */
function openCreate(group?: string) {
  dialogMode.value = 'create'
  Object.assign(form, defaultForm(group))
  dialogVisible.value = true
}

/** 打开编辑弹窗（回填；敏感值不回填——后端已脱敏，留空提交即保持原值） */
function openEdit(row: SystemConfig) {
  dialogMode.value = 'edit'
  Object.assign(form, defaultForm(), row)
  if (form.isSecret === 1) {
    form.configValue = ''
  }
  dialogVisible.value = true
}

/** 提交表单 */
async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createConfig({ ...form })
      ElMessage.success('配置已新增')
    } else {
      await updateConfig(form.configKey, { ...form })
      ElMessage.success('配置已保存（运行时配置各服务最迟 60 秒生效）')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    // 错误消息已由响应拦截器统一提示
    void err
  } finally {
    submitting.value = false
  }
}

/** 删除（二次确认） */
async function onDelete(row: SystemConfig) {
  try {
    await ElMessageBox.confirm(
      `确定删除配置「${row.configName || row.configKey}」？删除后该键回退默认值/环境变量。`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteConfig(row.configKey)
    ElMessage.success('配置已删除')
    await load()
  } catch (err) {
    void err
  }
}

/** 重置搜索 */
function handleReset() {
  keyword.value = ''
}

onMounted(load)
</script>

<template>
  <div class="config-page">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-hint">
          系统外部平台核心凭据仓库：修改保存后各服务最迟 60 秒热生效；敏感值列表脱敏、编辑留空即保持原值。
        </div>
        <div class="toolbar-actions">
          <el-input
            v-model="keyword"
            placeholder="按 Key / 名称过滤"
            clearable
            style="width: 220px"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
          <el-button type="primary" @click="openCreate()">
            <el-icon><Plus /></el-icon>新增配置
          </el-button>
        </div>
      </div>
    </el-card>

    <div v-loading="loading" class="cards-wrap">
      <el-row :gutter="16">
        <el-col
          v-for="card in groupCards"
          :key="card.value"
          :xs="24"
          :sm="24"
          :md="12"
          :lg="12"
        >
          <el-card shadow="never" class="group-card">
            <template #header>
              <div class="card-header">
                <div class="card-title">
                  <span class="card-name">{{ card.label }}</span>
                  <el-tag size="small" type="info">{{ card.items.length }} 项</el-tag>
                </div>
                <el-button link type="primary" size="small" @click="openCreate(card.value)">
                  <el-icon><Plus /></el-icon>新增
                </el-button>
              </div>
              <div class="card-desc">{{ card.desc }}</div>
            </template>

            <div v-if="card.items.length === 0" class="empty-group">暂无配置项</div>
            <div v-else class="cfg-list">
              <div v-for="row in card.items" :key="row.configKey" class="cfg-row">
                <div class="cfg-info">
                  <div class="cfg-name">
                    {{ row.configName }}
                    <span class="cfg-key">{{ row.configKey }}</span>
                  </div>
                  <div class="cfg-value">
                    <template v-if="isUnset(row)">
                      <span class="unset">未配置</span>
                    </template>
                    <template v-else-if="row.isSecret === 1">
                      <span class="masked">******</span>
                      <el-tag type="danger" size="small" class="secret-tag">敏感</el-tag>
                    </template>
                    <el-tooltip v-else :content="displayValue(row)" placement="top" :disabled="displayValue(row).length < 40">
                      <span class="value-text">{{ displayValue(row) }}</span>
                    </el-tooltip>
                  </div>
                </div>
                <div class="cfg-actions">
                  <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
                  <el-button type="danger" link size="small" @click="onDelete(row)">删除</el-button>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div class="pager-hint">共 {{ totalCount }} 项配置</div>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增配置' : '编辑配置'"
      width="640px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="right"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="配置分组" prop="configGroup">
              <el-select v-model="form.configGroup" placeholder="选择分组" style="width: 100%">
                <el-option
                  v-for="item in CONFIG_GROUP_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="配置 Key" prop="configKey">
              <el-input
                v-model="form.configKey"
                placeholder="如 oss.endpoint（建议带分组前缀）"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" placeholder="便于理解的展示名" />
        </el-form-item>

        <el-form-item label="配置值" prop="configValue">
          <el-input
            v-model="form.configValue"
            type="textarea"
            :rows="2"
            :placeholder="form.isSecret === 1 ? '敏感值：留空保持原值不变，填写即覆盖' : '请输入配置值'"
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="值类型" prop="valueType">
              <el-select v-model="form.valueType" placeholder="选择类型" style="width: 100%">
                <el-option
                  v-for="item in CONFIG_VALUE_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value as ConfigValueType"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="环境" prop="env">
              <el-select v-model="form.env" placeholder="选择环境" style="width: 100%">
                <el-option
                  v-for="item in CONFIG_ENV_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="敏感配置">
              <el-switch v-model="form.isSecret" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="运行时可改">
              <el-switch v-model="form.isRuntime" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.config-page {
  .toolbar-card {
    margin-bottom: 16px;

    .toolbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      flex-wrap: wrap;

      .toolbar-hint {
        color: #909399;
        font-size: 13px;
        flex: 1;
        min-width: 260px;
      }

      .toolbar-actions {
        display: flex;
        gap: 8px;
        align-items: center;
      }
    }
  }

  .cards-wrap {
    min-height: 200px;
  }

  .group-card {
    margin-bottom: 16px;

    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .card-title {
        display: flex;
        align-items: center;
        gap: 8px;

        .card-name {
          font-weight: 600;
        }
      }
    }

    .card-desc {
      margin-top: 6px;
      color: #909399;
      font-size: 12px;
      line-height: 1.5;
    }

    .empty-group {
      color: #c0c4cc;
      text-align: center;
      padding: 18px 0;
    }

    .cfg-list {
      display: flex;
      flex-direction: column;
    }

    .cfg-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 10px 0;
      border-bottom: 1px dashed #ebeef5;

      &:last-child {
        border-bottom: none;
      }

      .cfg-info {
        flex: 1;
        min-width: 0;

        .cfg-name {
          font-size: 13px;
          color: #303133;

          .cfg-key {
            margin-left: 8px;
            color: #909399;
            font-size: 12px;
            font-family: monospace;
          }
        }

        .cfg-value {
          margin-top: 4px;
          font-size: 13px;

          .masked {
            color: #909399;
            letter-spacing: 2px;
          }

          .unset {
            color: #e6a23c;
          }

          .value-text {
            display: inline-block;
            max-width: 100%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            vertical-align: bottom;
            color: #606266;
            font-family: monospace;
          }

          .secret-tag {
            margin-left: 6px;
          }
        }
      }

      .cfg-actions {
        flex-shrink: 0;
      }
    }
  }

  .pager-hint {
    color: #909399;
    font-size: 13px;
    text-align: center;
    padding: 4px 0 8px;
  }
}
</style>
