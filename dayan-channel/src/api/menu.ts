import { request } from '@/utils/request'
import type { Menu } from '@/types/menu'

/**
 * Channel 端菜单接口封装。
 *
 * 对应后端 ChannelMenuController（/channel-api/menus/*）。
 */

/**
 * 当前账号可见菜单树：GET /channel-api/menus/mine
 *
 * 后端写死 domainType=channel（见 ChannelMenuController），前端不传该参数。
 * channel 端不做按角色过滤菜单（登录即见 domain_type=channel 全量菜单，见 spec §3.7）。
 */
export function getMyMenuTree(): Promise<Menu[]> {
  return request<Menu[]>({
    url: '/channel-api/menus/mine',
    method: 'get'
  })
}

/**
 * 当前账号按钮级权限码：GET /channel-api/menus/mine/permissions
 *
 * 供 v-permission 指令消费。超管返回 ['*']（通配，指令侧视为放行一切）。
 */
export function getMyPermissions(): Promise<string[]> {
  return request<string[]>({
    url: '/channel-api/menus/mine/permissions',
    method: 'get'
  })
}
