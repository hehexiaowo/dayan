<script setup lang="ts">
/**
 * 类目管理弹窗（百炼业务空间级类目树）。
 * 支持多级：节点「新增子类目」；默认类目（isDefault）只读不可删。
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listKnowledgeCategories,
  addKnowledgeCategory,
  deleteKnowledgeCategory
} from '@/api/knowledge'
import type { KnowledgeCategory } from '@/types/knowledge'

const visible = defineModel<boolean>({ default: false })
const loading = ref(false)
const categories = ref<KnowledgeCategory[]>([])

interface TreeNode extends KnowledgeCategory {
  children: TreeNode[]
}

/** 平铺 → 树（parentCategoryId 挂接；顶层含百炼 default 类目） */
function buildTree(flat: KnowledgeCategory[]): TreeNode[] {
  const map = new Map<string, TreeNode>()
  flat.forEach((c) => map.set(c.categoryId, { ...c, children: [] }))
  const roots: TreeNode[] = []
  map.forEach((node) => {
    if (node.parentCategoryId && map.has(node.parentCategoryId)) {
      map.get(node.parentCategoryId)!.children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

async function load() {
  loading.value = true
  try {
    categories.value = await listKnowledgeCategories()
  } catch {
    categories.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

/** 新增子类目 */
async function handleAdd(parent?: TreeNode) {
  let value: string | undefined
  try {
    value = (
      await ElMessageBox.prompt(
        parent ? `在「${parent.categoryName}」下新增子类目名称：` : '新增顶级类目名称：',
        '新增类目',
        { confirmButtonText: '确定', cancelButtonText: '取消', inputPattern: /\S+/, inputErrorMessage: '类目名称不能为空' }
      )
    ).value
  } catch {
    return // 用户取消，吞掉拒绝
  }
  if (!value) return
  await addKnowledgeCategory({ categoryName: value.trim(), parentCategoryId: parent?.categoryId })
  ElMessage.success('类目创建成功')
  load()
}

async function handleDelete(node: TreeNode) {
  if (node.isDefault) return
  try {
    await ElMessageBox.confirm(
      `确定删除类目「${node.categoryName}」？若类目下有文件，百炼将拒绝删除。`,
      '删除类目',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // 用户取消，吞掉拒绝
  }
  await deleteKnowledgeCategory(node.categoryId)
  ElMessage.success('删除成功')
  load()
}
</script>

<template>
  <el-dialog v-model="visible" title="类目管理" width="560px" @open="load">
    <div class="category-toolbar">
      <el-button type="primary" size="small" @click="handleAdd()">新增顶级类目</el-button>
      <span class="tip">类目为业务空间级（所有知识库共享），用于上传文件时归类</span>
    </div>
    <el-tree
      v-loading="loading"
      :data="buildTree(categories)"
      node-key="categoryId"
      default-expand-all
      :expand-on-click-node="false"
      class="category-tree"
    >
      <template #default="{ data }">
        <div class="tree-node">
          <span class="node-name">
            {{ data.categoryName }}
            <el-tag v-if="data.isDefault" size="small" type="info">默认</el-tag>
          </span>
          <span class="node-actions">
            <el-button link type="primary" size="small" @click.stop="handleAdd(data)">新增子类目</el-button>
            <el-button
              v-if="!data.isDefault"
              link
              type="danger"
              size="small"
              @click.stop="handleDelete(data)"
            >删除</el-button>
          </span>
        </div>
      </template>
    </el-tree>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.category-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  .tip {
    font-size: 12px;
    color: #909399;
  }
}
.category-tree {
  max-height: 420px;
  overflow: auto;
  .tree-node {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-right: 8px;
    .node-name {
      font-size: 13px;
    }
  }
}
</style>
