import type { Directive, DirectiveBinding } from 'vue'
import { usePermissionStore } from '@/stores/permission'

/**
 * v-permission 按钮级权限指令。
 *
 * 用法：
 *   <el-button v-permission="'goods:info:delete'">删除</el-button>
 *   <el-button v-permission="['goods:info:update', 'goods:info:create']">保存</el-button>
 *
 * 无权限时直接移除元素（不是禁用），权限判定统一走 permission store 的 hasPerm：
 * 超管 '*' 通配放行，数组任一命中即放行。
 *
 * 注意：本指令只做 UI 收敛，接口鉴权仍以后端 @SaCheckPermission 为准（双保险）。
 */
export const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const store = usePermissionStore()
    if (!binding.value || !store.hasPerm(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  }
}
