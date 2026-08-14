<script setup lang="ts">
import { reactive, ref } from 'vue'
import { type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageConfigs,
  createConfig,
  updateConfig,
  deleteConfig
} from '@/api/config'
import {
  type SystemConfig,
  type ConfigQuery,
  type ConfigValueType,
  CONFIG_VALUE_TYPE_OPTIONS,
  CONFIG_ENV_OPTIONS,
  CONFIG_GROUP_OPTIONS
} from '@/types/config'

/**
 * 系统配置管理页（完整 CRUD）。
 *
 * - configGroup 筛选 + 分页列表；
 * - isSecret=1 的配置在列表脱敏显示（展示 `***`）；
 * - 新增/编辑弹窗（编辑时 configValue 可填写，提交时校验 configKey 唯一）。
 */

const crud = useCrud<SystemConfig, ConfigQuery>(
  {
    page: pageConfigs,
    create: createConfig,
    update: updateConfig,
    remove: deleteConfig
  },
  { initialQuery: { configGroup: '', configKey: '' } }
)

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange,
  handleCreate,
  handleUpdate,
  handleDelete
} = crud

// ---------------- 弹窗与表单 ----------------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()
const submitting = ref(false)

/** 表单默认值（新增时使用） */
function defaultForm(): SystemConfig {
  return {
    configGroup: 'system',
    configKey: '',
    configValue: '',
    valueType: 'string',
    env: 'all',
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

/** 打开新增弹窗 */
function openCreate() {
  dialogMode.value = 'create'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

/** 打开编辑弹窗（回填） */
function openEdit(row: SystemConfig) {
  dialogMode.value = 'edit'
  Object.assign(form, defaultForm(), row)
  // 编辑时，敏感配置的 value 也回填原值（编辑态允许填写）
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
      await handleCreate({ ...form })
    } else {
      await handleUpdate(form.configKey, { ...form })
    }
    dialogVisible.value = false
  } catch (err) {
    // 错误消息已由响应拦截器统一提示
    void err
  } finally {
    submitting.value = false
  }
}

/** 删除 */
async function onDelete(row: SystemConfig) {
  try {
    await handleDelete(row.configKey, `确定删除配置「${row.configName || row.configKey}」？`)
  } catch (err) {
    void err
  }
}

/** 列表脱敏显示：isSecret=1 时展示 `***` */
function displayValue(row: SystemConfig): string {
  if (row.isSecret === 1) return '***'
  return row.configValue ?? ''
}

/** 值类型标签颜色 */
function valueTypeTagType(vt: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    string: 'info',
    number: 'success',
    boolean: 'warning',
    json: 'danger'
  }
  return map[vt] || 'info'
}

/** 重置搜索条件 */
function handleReset() {
  query.configGroup = ''
  query.configKey = ''
  handleSearch()
}

/** env 标签文案 */
function envLabel(env: string): string {
  const o = CONFIG_ENV_OPTIONS.find((i) => i.value === env)
  return o ? o.label : env
}

// 首次加载
loadPage()
</script>

<template>
  <div class="config-page">
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <div class="toolbar">
        <el-select
          v-model="query.configGroup"
          placeholder="全部分组"
          clearable
          style="width: 180px"
          @change="handleSearch"
        >
          <el-option
            v-for="item in CONFIG_GROUP_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-input
          v-model="query.configKey"
          placeholder="配置 Key"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <div class="toolbar-actions">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
          <el-button @click="openCreate">
            <el-icon><Plus /></el-icon>新增配置
          </el-button>
        </div>
      </div>

      <!-- 列表 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        empty-text="暂无配置数据"
      >
        <el-table-column prop="configKey" label="配置 Key" min-width="180" show-overflow-tooltip />
        <el-table-column prop="configName" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="值" min-width="180">
          <template #default="{ row }">
            <span :class="{ masked: row.isSecret === 1 }">{{ displayValue(row) }}</span>
            <el-tag v-if="row.isSecret === 1" type="danger" size="small" class="secret-tag">敏感</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="configGroup" label="分组" width="110" align="center" />
        <el-table-column label="值类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="valueTypeTagType(row.valueType)" size="small">{{ row.valueType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="环境" width="80" align="center">
          <template #default="{ row }">{{ envLabel(row.env) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

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
                placeholder="如 system.title"
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
            :placeholder="form.isSecret === 1 ? '敏感值，请填写（保存后列表将脱敏）' : '请输入配置值'"
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
  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .toolbar-actions {
      display: flex;
      gap: 8px;
    }
  }

  .masked {
    color: #909399;
    letter-spacing: 2px;
  }

  .secret-tag {
    margin-left: 6px;
  }

  .pager {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
