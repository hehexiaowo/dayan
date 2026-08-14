import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ToolInfo, ToolInfoQuery } from '@/types/tool'

/**
 * 工具域接口封装。
 *
 * 对应后端 ToolInfoAdminController（/admin-api/tool/info/*）。
 * 主键 toolCode（TL 前缀，服务端生成），update/delete 用 path toolCode。
 */

/** 工具分页：GET /admin-api/tool/info/page */
export function pageTools(query: ToolInfoQuery): Promise<PageResult<ToolInfo>> {
  return request<PageResult<ToolInfo>>({
    url: '/admin-api/tool/info/page',
    method: 'get',
    params: query
  })
}

/** 工具列表（全量）：GET /admin-api/tool/info/list */
export function listTools(query?: Partial<ToolInfoQuery>): Promise<ToolInfo[]> {
  return request<ToolInfo[]>({
    url: '/admin-api/tool/info/list',
    method: 'get',
    params: query
  })
}

/** 工具详情：GET /admin-api/tool/info/{toolCode} */
export function getTool(toolCode: string): Promise<ToolInfo> {
  return request<ToolInfo>({
    url: `/admin-api/tool/info/${toolCode}`,
    method: 'get'
  })
}

/** 新增工具：POST /admin-api/tool/info（返回 toolCode） */
export function createTool(data: Partial<ToolInfo>): Promise<string> {
  return request<string>({
    url: '/admin-api/tool/info',
    method: 'post',
    data
  })
}

/** 修改工具：PUT /admin-api/tool/info/{toolCode}（toolCode 不可改） */
export function updateTool(toolCode: string, data: Partial<ToolInfo>): Promise<void> {
  return request<void>({
    url: `/admin-api/tool/info/${toolCode}`,
    method: 'put',
    data
  })
}

/** 删除工具：DELETE /admin-api/tool/info/{toolCode} */
export function deleteTool(toolCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/tool/info/${toolCode}`,
    method: 'delete'
  })
}
