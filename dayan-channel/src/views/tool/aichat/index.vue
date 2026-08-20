<script setup lang="ts">
/**
 * Channel 端问答人物落地页（系统管理 → 问答人物）。
 *
 * 你问我答人物知识库两层模型：
 * - admin 全局库（config_json.repoIds）：只读展示，不可改；
 * - 本渠道补充库（tool_channel_repo_bind）：多选编辑，并集生效；
 *   可选项 = 本渠道 + 后代渠道名下的渠道库（不含平台库）。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getChannelAichatPersonas,
  getChannelAichatRepoOptions,
  saveChannelPersonaRepos
} from '@/api/toolAichat'
import type { ToolChannelPersona, ToolChannelRepoOption } from '@/types/toolAichat'

const loading = ref(false)
const personas = ref<ToolChannelPersona[]>([])
const repoOptions = ref<ToolChannelRepoOption[]>([])

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<{ toolCode: string; personaName: string; channelRepoIds: number[] }>({
  toolCode: '',
  personaName: '',
  channelRepoIds: []
})
const rules: FormRules = {}

/** repoId → 名称映射（全局库/补充库标签展示用） */
const repoNameMap = computed(() => new Map(repoOptions.value.map((r) => [r.id, r.repoName])))

async function loadData() {
  loading.value = true
  try {
    const [p, r] = await Promise.all([getChannelAichatPersonas(), getChannelAichatRepoOptions()])
    personas.value = p
    repoOptions.value = r
  } catch {
    ElMessage.error('加载问答人物失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

function openEdit(row: ToolChannelPersona) {
  form.toolCode = row.toolCode
  form.personaName = row.personaName
  form.channelRepoIds = [...row.channelRepoIds]
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    await saveChannelPersonaRepos(form.toolCode, form.channelRepoIds)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>问答人物</span>
          <span class="card-tip">渠道可为人物的知识库补充本渠道（含后代渠道）的库，与 admin 全局库并集生效</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="personas" stripe>
        <el-table-column prop="personaName" label="人物名称" min-width="140" />
        <el-table-column prop="toolDesc" label="简介" min-width="200" show-overflow-tooltip />
        <el-table-column label="admin 全局知识库（只读）" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="id in row.globalRepoIds"
              :key="id"
              size="small"
              class="repo-tag"
            >{{ repoNameMap.get(id) || `知识库#${id}` }}</el-tag>
            <span v-if="!row.globalRepoIds.length" class="empty-text">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column label="本渠道补充知识库" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="id in row.channelRepoIds"
              :key="id"
              type="success"
              size="small"
              class="repo-tag"
            >{{ repoNameMap.get(id) || `知识库#${id}` }}</el-tag>
            <span v-if="!row.channelRepoIds.length" class="empty-text">未补充（仅用全局库）</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑补充</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="`编辑补充知识库 · ${form.personaName}`" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="补充知识库">
          <el-select v-model="form.channelRepoIds" multiple filterable collapse-tags style="width: 100%">
            <el-option v-for="r in repoOptions" :key="r.id" :label="r.repoName" :value="r.id" />
          </el-select>
          <div class="form-tip">可选项为本渠道及后代渠道名下的知识库；留空 = 不补充（仅用 admin 全局库）</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.card-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.repo-tag {
  margin: 2px 4px 2px 0;
}
.empty-text {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
</style>
