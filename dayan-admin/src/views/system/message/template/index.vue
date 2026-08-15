<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  pageMessageTemplates,
  createMessageTemplate,
  updateMessageTemplate,
  deleteMessageTemplate
} from '@/api/message'
import {
  type MessageTemplate,
  CHANNEL_TYPE_OPTIONS,
  TEMPLATE_STATUS_OPTIONS,
  BIZ_TYPE_OPTIONS,
  channelTypeLabel
} from '@/types/message'
import { formatDateTime } from '@/utils/format'

/**
 * 消息模板管理页（system_message_template）。
 *
 * - 渠道覆盖短信/站内信/APP推送/企微/微信模板消息/邮件；
 * - 站内信(2)/推送(3)/邮件(6) 渠道标题必填（动态校验）；
 * - 模板编码被发送记录引用，编辑时锁定不可改；
 * - 正文支持 ${var} 占位符，变量定义以 JSON 数组维护（如 [{"name":"code","label":"验证码"}]）。
 */

const loading = ref(false)
const tableData = ref<MessageTemplate[]>([])
const total = ref(0)

const query = reactive({
  templateCode: '',
  templateName: '',
  bizType: '',
  channelType: undefined as number | undefined,
  status: undefined as number | undefined,
  current: 1,
  size: 20
})

async function loadData() {
  loading.value = true
  try {
    const res = await pageMessageTemplates({ ...query })
    tableData.value = res.records
    total.value = res.total
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.current = 1
  loadData()
}

function handleReset() {
  query.templateCode = ''
  query.templateName = ''
  query.bizType = ''
  query.channelType = undefined
  query.status = undefined
  query.current = 1
  loadData()
}

function handlePageChange(page: number) {
  query.current = page
  loadData()
}

function handleSizeChange(size: number) {
  query.size = size
  query.current = 1
  loadData()
}

/** 变量定义 JSON 解析（容错：非法 JSON 返回 null） */
function parseVariables(vars?: string | null): { name: string; label?: string }[] | null {
  if (!vars) return null
  try {
    const parsed = JSON.parse(vars)
    return Array.isArray(parsed) ? parsed : null
  } catch {
    return null
  }
}

const varNames = computed(() =>
  tableData.value.map((row) => parseVariables(row.variables)?.map((v) => v.name) ?? null)
)

// ---------------- 新增/编辑弹窗 ----------------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

function defaultForm(): MessageTemplate {
  return {
    templateCode: '',
    templateName: '',
    bizType: '',
    channelType: 1,
    title: '',
    content: '',
    variables: '',
    fallbackChannelType: undefined,
    channelCode: '',
    status: 1,
    sortOrder: 0,
    remark: ''
  }
}

const form = reactive<MessageTemplate>(defaultForm())

/** 站内信/推送/邮件渠道必须携带标题 */
const titleRequired = computed(() => [2, 3, 6].includes(form.channelType ?? 0))

const rules: FormRules = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  bizType: [{ required: true, message: '请选择或输入业务类型', trigger: 'change' }],
  channelType: [{ required: true, message: '请选择渠道类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入模板正文', trigger: 'blur' }],
  title: [
    {
      validator: (_rule, value: string, callback) => {
        if (titleRequired.value && !value?.trim()) {
          callback(new Error('站内信/APP推送/邮件渠道的消息标题必填'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  variables: [
    {
      validator: (_rule, value: string, callback) => {
        if (!value?.trim()) {
          callback()
          return
        }
        try {
          const parsed = JSON.parse(value)
          Array.isArray(parsed) ? callback() : callback(new Error('必须是 JSON 数组'))
        } catch {
          callback(new Error('不是合法的 JSON'))
        }
      },
      trigger: 'blur'
    }
  ]
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEdit(row: MessageTemplate) {
  dialogMode.value = 'edit'
  editingId.value = row.id ?? null
  Object.assign(form, defaultForm(), row)
  dialogVisible.value = true
}

async function handleSubmit() {
  const inst = formRef.value
  if (!inst) return
  await inst.validate()
  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createMessageTemplate({ ...form })
      ElMessage.success('新增成功')
    } else {
      await updateMessageTemplate(editingId.value!, { ...form })
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (err) {
    void err
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: MessageTemplate) {
  try {
    await ElMessageBox.confirm(
      `确定删除模板「${row.templateName}」（${row.templateCode}）吗？`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteMessageTemplate(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch (err) {
    void err
  }
}

onMounted(loadData)
</script>

<template>
  <div class="msg-template-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.templateCode"
          placeholder="模板编码"
          clearable
          style="width: 170px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.templateName"
          placeholder="模板名称"
          clearable
          style="width: 150px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.bizType" placeholder="业务类型" clearable filterable style="width: 140px">
          <el-option v-for="o in BIZ_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.channelType" placeholder="渠道" clearable style="width: 120px">
          <el-option v-for="o in CHANNEL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 100px">
          <el-option v-for="o in TEMPLATE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <div class="toolbar-actions">
          <el-button v-permission="'system:msg-tpl:create'" type="primary" @click="openCreate">新增模板</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="templateCode" label="模板编码" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono">{{ row.templateCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="templateName" label="模板名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="bizType" label="业务类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.bizType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="channelType" label="渠道" width="115">
          <template #default="{ row }">
            <el-tag size="small" :type="row.channelType === 1 ? 'success' : row.channelType === 2 ? 'primary' : 'warning'">
              {{ channelTypeLabel(row.channelType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.title || '—' }}</template>
        </el-table-column>
        <el-table-column prop="content" label="正文" min-width="220" show-overflow-tooltip />
        <el-table-column label="变量" width="130">
          <template #default="{ $index }">
            <template v-if="varNames[$index]?.length">
              <el-tooltip placement="top" :content="varNames[$index]!.join('、')">
                <span>
                  <el-tag v-for="n in varNames[$index]!.slice(0, 2)" :key="n" size="small" type="info" class="var-tag">
                    $&#123;{{ n }}&#125;
                  </el-tag>
                  <span v-if="varNames[$index]!.length > 2" class="text-muted">+{{ varNames[$index]!.length - 2 }}</span>
                </span>
              </el-tooltip>
            </template>
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column prop="updatedAt" label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:msg-tpl:update'" link type="primary" size="small" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-permission="'system:msg-tpl:delete'" link type="danger" size="small" @click="handleDeleteRow(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          :current-page="query.current"
          :page-size="query.size"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增消息模板' : '编辑消息模板'"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input
                v-model="form.templateCode"
                :disabled="dialogMode === 'edit'"
                placeholder="全局唯一，如 sms_login_code"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="如 登录验证码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务类型" prop="bizType">
              <el-select v-model="form.bizType" filterable allow-create default-first-option style="width: 100%">
                <el-option v-for="o in BIZ_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="渠道类型" prop="channelType">
              <el-select v-model="form.channelType" style="width: 100%">
                <el-option v-for="o in CHANNEL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="消息标题" prop="title">
              <el-input v-model="form.title" :placeholder="titleRequired ? '必填' : '短信/企微/微信模板可不填'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="降级渠道">
              <el-select v-model="form.fallbackChannelType" clearable placeholder="发送失败时备选" style="width: 100%">
                <el-option v-for="o in CHANNEL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="模板正文" prop="content">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="5"
                placeholder="支持变量占位符，如：您的验证码为${code}，${expireMinutes}分钟内有效"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="变量定义" prop="variables">
              <el-input
                v-model="form.variables"
                type="textarea"
                :rows="3"
                placeholder='JSON 数组，如 [{"name":"code","label":"验证码"},{"name":"expireMinutes","label":"有效分钟数"}]'
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in TEMPLATE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.msg-template-page {
  .toolbar {
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

  .pager {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .mono {
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 12px;
  }

  .var-tag {
    margin-right: 4px;
  }

  .text-muted {
    color: #8a919f;
    font-size: 12px;
  }
}
</style>
