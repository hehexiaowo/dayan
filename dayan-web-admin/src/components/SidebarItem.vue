<script setup lang="ts">
import type { Menu } from '@/types/menu'
import { MenuType } from '@/types/menu'

/**
 * 递归侧边栏菜单项。
 *
 * - 目录（menuType=1）渲染为 el-sub-menu，递归渲染 children。
 * - 菜单（menuType=2）渲染为 el-menu-item，点击走 Vue Router（el-menu 的 router 模式）。
 * - 按钮（menuType=3）不渲染。
 * - icon 字段为 Element Plus 图标组件名（全局已注册），动态渲染。
 */
defineOptions({ name: 'SidebarItem' })

defineProps<{
  /** 当前菜单项 */
  item: Menu
}>()

/** 是否目录（含子菜单） */
function isDirectory(menu: Menu): boolean {
  return menu.menuType === MenuType.DIRECTORY
}

/** 子菜单（仅目录有） */
function children(menu: Menu): Menu[] {
  return menu.children ?? []
}

/** 渲染图标：menu.icon 为 Element Plus 图标组件名字符串 */
function iconComp(menu: Menu) {
  return menu.icon || 'Menu'
}
</script>

<template>
  <!-- 目录：递归子菜单 -->
  <el-sub-menu v-if="isDirectory(item)" :index="item.path ?? item.menuCode">
    <template #title>
      <el-icon><component :is="iconComp(item)" /></el-icon>
      <span>{{ item.menuName }}</span>
    </template>
    <sidebar-item
      v-for="child in children(item)"
      :key="child.menuCode"
      :item="child"
    />
  </el-sub-menu>

  <!-- 菜单叶子 -->
  <el-menu-item v-else :index="item.path ?? item.menuCode">
    <el-icon><component :is="iconComp(item)" /></el-icon>
    <template #title>{{ item.menuName }}</template>
  </el-menu-item>
</template>
