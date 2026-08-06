<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getMenuTree, listMenus, createMenu, updateMenu, deleteMenu } from '@/api/menu'
import {
  MenuType,
  MENU_TYPE_OPTIONS,
  DOMAIN_TYPE_OPTIONS,
  buildMenuTree,
  type Menu,
  type DomainType
} from '@/types/menu'

/**
 * 菜单管理页（树形表格）。
 *
 * - 顶部 domainType 筛选（admin/channel/agent/client），切换后重新拉树；
 * - 数据源优先用 /menus/tree（后端组树），并并行拉 /menus 平铺列表供父级选择；
 * - parentCode 用 el-tree-select 选择（编辑态排除自身及其子孙）。
 */

const domainType = ref<DomainType>('admin')
const loading = ref(false)
const treeData = ref<Menu[]>([])
const flatList = ref<Menu[]>([])

async function loadTree() {
  loading.value = true
  try {
    const [tree, list] = await Promise.all([getMenuTree(domainType.value), listMenus(domainType.value)])
    treeData.value = tree
    flatList.value = list
  } finally {
    loading.value = false
  }
}

function handleDomainChange() {
  loadTree()
}

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Menu>({
  menuCode: '',
  menuName: '',
  parentCode: null,
  menuType: MenuType.MENU,
  path: '',
  component: '',
  permissionCode: '',
  icon: '',
  sortOrder: 0,
  isVisible: 1,
  isExternal: 0,
  isCache: 1,
  domainType: 'admin',
  status: 1,
  remark: ''
})

const rules: FormRules = {
  menuCode: [
    { required: true, message: '请输入菜单编码', trigger: 'blur' },
    { max: 100, message: '菜单编码长度不能超过 100', trigger: 'blur' }
  ],
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  domainType: [{ required: true, message: '请选择所属端', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

/** 供父级选择用的树（基于平铺列表组树） */
const parentTreeOptions = ref<Menu[]>([])

function buildParentOptions(excludeCode?: string): Menu[] {
  const tree = buildMenuTree(flatList.value)
  if (excludeCode) {
    const filterNode = (nodes: Menu[]): Menu[] => {
      const result: Menu[] = []
      for (const n of nodes) {
        if (n.menuCode === excludeCode) continue
        const children = n.children ? filterNode(n.children) : undefined
        result.push(children ? { ...n, children } : { ...n, children: undefined })
      }
      return result
    }
    return filterNode(tree)
  }
  return tree
}

function resetForm() {
  Object.assign(form, {
    menuCode: '',
    menuName: '',
    parentCode: null,
    menuType: MenuType.MENU,
    path: '',
    component: '',
    permissionCode: '',
    icon: '',
    sortOrder: 0,
    isVisible: 1,
    isExternal: 0,
    isCache: 1,
    domainType: domainType.value,
    status: 1,
    remark: ''
  })
}

function openCreate(parent?: Menu) {
  dialogType.value = 'create'
  resetForm()
  if (parent) {
    form.parentCode = parent.menuCode
    form.menuType = parent.menuType
  }
  parentTreeOptions.value = buildParentOptions()
  dialogVisible.value = true
}

function openEdit(row: Menu) {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, {
    menuCode: row.menuCode,
    menuName: row.menuName,
    parentCode: row.parentCode,
    menuType: row.menuType,
    path: row.path,
    component: row.component,
    permissionCode: row.permissionCode,
    icon: row.icon,
    sortOrder: row.sortOrder,
    isVisible: row.isVisible,
    isExternal: row.isExternal,
    isCache: row.isCache,
    domainType: row.domainType,
    status: row.status,
    remark: row.remark
  })
  parentTreeOptions.value = buildParentOptions(row.menuCode)
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
    if (dialogType.value === 'create') {
      await createMenu(form)
      ElMessage.success('新增成功')
    } else {
      await updateMenu(form.menuCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadTree()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: Menu) {
  await ElMessageBox.confirm(`确定删除菜单「${row.menuName}」吗？若存在子菜单将一并影响。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteMenu(row.menuCode)
  ElMessage.success('删除成功')
  loadTree()
}

function typeText(t: number) {
  return MENU_TYPE_OPTIONS.find((o) => o.value === t)?.label ?? t
}

function typeTagType(t: number): 'primary' | 'success' | 'warning' {
  if (t === MenuType.DIRECTORY) return 'primary'
  if (t === MenuType.MENU) return 'success'
  return 'warning'
}

// 初始化加载
loadTree()
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="header-title">菜单列表</span>
            <el-select
              v-model="domainType"
              placeholder="所属端"
              style="width: 140px; margin-left: 16px"
              @change="handleDomainChange"
            >
              <el-option v-for="o in DOMAIN_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </div>
          <div>
            <el-button :icon="'Refresh'" @click="loadTree">刷新</el-button>
            <el-button type="primary" :icon="'Plus'" @click="openCreate()">新增菜单</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="treeData"
        border
        stripe
        row-key="menuCode"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column prop="menuCode" label="菜单编码" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.menuType)">{{ typeText(row.menuType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="90" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="160" show-overflow-tooltip />
        <el-table-column prop="component" label="组件" min-width="180" show-overflow-tooltip />
        <el-table-column prop="permissionCode" label="权限标识" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column label="可见" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isVisible === 1 ? 'success' : 'info'">
              {{ row.isVisible === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openCreate(row)">新增子级</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增菜单' : '编辑菜单'"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="菜单编码" prop="menuCode">
              <el-input
                v-model="form.menuCode"
                placeholder="菜单编码"
                :disabled="dialogType === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="form.menuName" placeholder="菜单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父级菜单">
              <el-tree-select
                v-model="form.parentCode"
                :data="parentTreeOptions"
                :props="{ label: 'menuName', value: 'menuCode', children: 'children' }"
                check-strictly
                clearable
                placeholder="顶级（不选）"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单类型" prop="menuType">
              <el-select v-model="form.menuType" style="width: 100%">
                <el-option v-for="o in MENU_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属端" prop="domainType">
              <el-select v-model="form.domainType" style="width: 100%">
                <el-option v-for="o in DOMAIN_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标">
              <el-input v-model="form.icon" placeholder="Element Plus 图标名（如 User）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="路由路径">
              <el-input v-model="form.path" placeholder="/system/user" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="组件路径">
              <el-input v-model="form.component" placeholder="system/user/index（相对 src/views）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限标识">
              <el-input v-model="form.permissionCode" placeholder="organ:account:list（按钮类用）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否可见">
              <el-switch :model-value="form.isVisible === 1" @change="(v: boolean) => (form.isVisible = v ? 1 : 0)" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否外链">
              <el-switch :model-value="form.isExternal === 1" @change="(v: boolean) => (form.isExternal = v ? 1 : 0)" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否缓存">
              <el-switch :model-value="form.isCache === 1" @change="(v: boolean) => (form.isCache = v ? 1 : 0)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
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

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .header-left {
    display: flex;
    align-items: center;

    .header-title {
      font-size: 16px;
      font-weight: 500;
    }
  }
}
</style>
