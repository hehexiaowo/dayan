<script setup lang="ts">
/**
 * 管家详情页 - 技能 tab。
 *
 * 分页模式：useCrud（主键 id 自增 number，idKey:'id'，fixedParams:{butlerCode}）。
 *
 * 关键约束：
 * - skillCode 编辑时不可改（update 只改 skillName/proficiency/isCertified/certificateNo/obtainDate/sortOrder）。
 * - sortOrder 空默认 0。
 * - proficiency：4 态（1了解/2熟悉/3熟练/4精通），用 el-select。
 * - isCertified：0否/1是，用 el-select。
 * - skillCode/skillName 字典来源未确认（DDL 注释提"字典：butler_skill"），用 input 兜底 + TODO。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageButlerSkills,
  createButlerSkill,
  updateButlerSkill,
  deleteButlerSkill
} from '@/api/service'
import {
  SKILL_PROFICIENCY_OPTIONS,
  IS_CERTIFIED_OPTIONS,
  skillProficiencyLabel,
  isCertifiedLabel
} from '@/types/service'
import type { ButlerSkill, ButlerSkillQuery } from '@/types/service'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  /** 管家编码（路由参数） */
  butlerCode: string
}>()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<ButlerSkill, ButlerSkillQuery, number>(
    {
      page: pageButlerSkills,
      create: createButlerSkill,
      update: (id, data) => updateButlerSkill(id, data),
      remove: (id) => deleteButlerSkill(id)
    },
    {
      initialQuery: { skillName: '', proficiency: undefined, isCertified: undefined },
      idKey: 'id',
      fixedParams: { butlerCode: props.butlerCode }
    }
  )

loadPage()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ButlerSkill>({
  id: undefined,
  butlerCode: '',
  skillCode: '',
  skillName: '',
  proficiency: 1,
  isCertified: 0,
  certificateNo: '',
  obtainDate: '',
  sortOrder: 0
})

const rules: FormRules<ButlerSkill> = {
  skillCode: [{ required: true, message: '请输入技能编码', trigger: 'blur' }],
  skillName: [{ required: true, message: '请输入技能名称', trigger: 'blur' }],
  proficiency: [{ required: true, message: '请选择熟练度', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    butlerCode: '',
    skillCode: '',
    skillName: '',
    proficiency: 1,
    isCertified: 0,
    certificateNo: '',
    obtainDate: '',
    sortOrder: 0
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.butlerCode = props.butlerCode
  dialogVisible.value = true
}

function openEdit(row: ButlerSkill) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: row.id,
    butlerCode: row.butlerCode,
    skillCode: row.skillCode ?? '',
    skillName: row.skillName ?? '',
    proficiency: row.proficiency ?? 1,
    isCertified: row.isCertified ?? 0,
    certificateNo: row.certificateNo ?? '',
    obtainDate: row.obtainDate ?? '',
    sortOrder: row.sortOrder ?? 0
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
  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createButlerSkill(form)
      ElMessage.success('新增成功')
    } else if (form.id != null) {
      await updateButlerSkill(form.id, {
        // skillCode 不可改，update 不提交 skillCode
        skillName: form.skillName,
        proficiency: form.proficiency,
        isCertified: form.isCertified,
        certificateNo: form.certificateNo,
        obtainDate: form.obtainDate,
        sortOrder: form.sortOrder ?? 0
      })
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ButlerSkill) {
  if (row.id == null) return
  await ElMessageBox.confirm(`确定删除技能「${row.skillName || row.skillCode}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteButlerSkill(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

defineExpose({ loadPage })
</script>

<template>
  <div class="skill-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="技能名称">
        <el-input v-model="query.skillName" placeholder="技能名称" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="熟练度">
        <el-select v-model="query.proficiency" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="o in SKILL_PROFICIENCY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否持证">
        <el-select v-model="query.isCertified" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="o in IS_CERTIFIED_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增技能</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="skillCode" label="技能编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="skillName" label="技能名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="proficiency" label="熟练度" width="100" align="center">
        <template #default="{ row }">{{ skillProficiencyLabel(row.proficiency) }}</template>
      </el-table-column>
      <el-table-column prop="isCertified" label="是否持证" width="100" align="center">
        <template #default="{ row }">{{ isCertifiedLabel(row.isCertified) }}</template>
      </el-table-column>
      <el-table-column prop="certificateNo" label="证书编号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="obtainDate" label="取得日期" width="120" align="center" />
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
      <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        :current-page="query.current"
        :page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增技能' : '编辑技能'"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <!-- TODO: 可接入 butler_skill 字典（字典是否存在未确认，暂用 input 兜底） -->
            <el-form-item label="技能编码" prop="skillCode">
              <el-input
                v-model="form.skillCode"
                :disabled="dialogMode === 'edit'"
                placeholder="技能编码"
                maxlength="64"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <!-- TODO: 可接入 butler_skill 字典联动填充 skillName -->
            <el-form-item label="技能名称" prop="skillName">
              <el-input v-model="form.skillName" placeholder="技能名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="熟练度" prop="proficiency">
              <el-select v-model="form.proficiency" style="width: 100%">
                <el-option v-for="o in SKILL_PROFICIENCY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否持证">
              <el-select v-model="form.isCertified" style="width: 100%">
                <el-option v-for="o in IS_CERTIFIED_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证书编号">
              <el-input v-model="form.certificateNo" placeholder="证书编号（可选）" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="取得日期">
              <el-date-picker
                v-model="form.obtainDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'edit'" :span="24">
            <el-alert
              type="info"
              :closable="false"
              title="技能编码创建后不可修改。"
              show-icon
            />
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

<style scoped lang="scss">
.skill-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
