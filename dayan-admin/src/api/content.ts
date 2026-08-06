import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ContentInfo, ContentInfoQuery } from '@/types/content'

/**
 * 内容信息接口封装。
 *
 * 对应后端 ContentInfoAdminController（/admin-api/content/info/*）。
 * 审核流：submit（草稿→待审）、audit（待审→通过/拒绝）、publish（通过→发布）、offline（通过→下线）。
 */

/** 内容分页：GET /admin-api/content/info/page */
export function pageContents(query: ContentInfoQuery): Promise<PageResult<ContentInfo>> {
  return request<PageResult<ContentInfo>>({
    url: '/admin-api/content/info/page',
    method: 'get',
    params: query
  })
}

/** 内容详情：GET /admin-api/content/info/{contentCode} */
export function getContent(contentCode: string): Promise<ContentInfo> {
  return request<ContentInfo>({
    url: `/admin-api/content/info/${contentCode}`,
    method: 'get'
  })
}

/** 新增内容：POST /admin-api/content/info（返回 contentCode） */
export function createContent(data: Partial<ContentInfo>): Promise<string> {
  return request<string>({
    url: '/admin-api/content/info',
    method: 'post',
    data
  })
}

/**
 * 修改内容：PUT /admin-api/content/info?contentCode=（contentCode 走 query string，非 path）。
 */
export function updateContent(contentCode: string, data: Partial<ContentInfo>): Promise<void> {
  return request<void>({
    url: '/admin-api/content/info',
    method: 'put',
    params: { contentCode },
    data
  })
}

/** 删除内容：DELETE /admin-api/content/info/{contentCode} */
export function deleteContent(contentCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/content/info/${contentCode}`,
    method: 'delete'
  })
}

/** 提交审核（草稿→待审）：POST /admin-api/content/info/submit?contentCode= */
export function submitContent(contentCode: string): Promise<void> {
  return request<void>({
    url: '/admin-api/content/info/submit',
    method: 'post',
    params: { contentCode }
  })
}

/**
 * 审核内容（待审→通过/拒绝）：POST /admin-api/content/info/audit
 *
 * auditStatus: 2=通过 / 3=拒绝（对齐后端 ContentInfoAuditDTO 约定）。
 */
export function auditContent(data: {
  contentCode: string
  auditStatus: number
  auditRemark?: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/content/info/audit',
    method: 'post',
    data
  })
}

/** 发布内容（通过→发布）：POST /admin-api/content/info/publish?contentCode= */
export function publishContent(contentCode: string): Promise<void> {
  return request<void>({
    url: '/admin-api/content/info/publish',
    method: 'post',
    params: { contentCode }
  })
}

/** 下线内容（通过→下线）：POST /admin-api/content/info/offline?contentCode= */
export function offlineContent(contentCode: string): Promise<void> {
  return request<void>({
    url: '/admin-api/content/info/offline',
    method: 'post',
    params: { contentCode }
  })
}
