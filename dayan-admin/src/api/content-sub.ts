import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ContentMedia,
  ContentMediaQuery,
  ContentRecordRead,
  ContentRecordReadQuery,
  ContentRecordShare,
  ContentRecordShareQuery,
  ContentReadStats
} from '@/types/content'

/**
 * 内容子表接口封装（媒体/阅读记录/分享记录）。
 *
 * 对应后端：
 * - ContentMediaAdminController（/admin-api/content/media/*）
 * - ContentRecordReadAdminController（/admin-api/content/record-read/*）
 * - ContentRecordShareAdminController（/admin-api/content/record-share/*）
 *
 * 注意：content 子表的 update 一律用 query `?id=` 传 id（非 path）。
 */

// ==================== 内容多媒体 ====================

export function pageContentMedia(query: ContentMediaQuery): Promise<PageResult<ContentMedia>> {
  return request<PageResult<ContentMedia>>({
    url: '/admin-api/content/media/page',
    method: 'get',
    params: query
  })
}

/** 按 contentCode 列出媒体 */
export function listContentMediaByCode(contentCode: string): Promise<ContentMedia[]> {
  return request<ContentMedia[]>({
    url: '/admin-api/content/media/list',
    method: 'get',
    params: { contentCode }
  })
}

export function getContentMedia(id: number): Promise<ContentMedia> {
  return request<ContentMedia>({ url: `/admin-api/content/media/${id}`, method: 'get' })
}

export function createContentMedia(data: Partial<ContentMedia>): Promise<number> {
  return request<number>({ url: '/admin-api/content/media', method: 'post', data })
}

/** 修改媒体：PUT /content/media?id=（id 走 query） */
export function updateContentMedia(id: number, data: Partial<ContentMedia>): Promise<void> {
  return request<void>({ url: '/admin-api/content/media', method: 'put', params: { id }, data })
}

export function deleteContentMedia(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/content/media/${id}`, method: 'delete' })
}

/** 按 contentCode 批量删除媒体 */
export function deleteContentMediaByCode(contentCode: string): Promise<void> {
  return request<void>({ url: '/admin-api/content/media/by-code', method: 'delete', params: { contentCode } })
}

// ==================== 阅读记录 + 统计 ====================

export function pageContentRecordRead(query: ContentRecordReadQuery): Promise<PageResult<ContentRecordRead>> {
  return request<PageResult<ContentRecordRead>>({
    url: '/admin-api/content/record-read/page',
    method: 'get',
    params: query
  })
}

export function createContentRecordRead(data: Partial<ContentRecordRead>): Promise<number> {
  return request<number>({ url: '/admin-api/content/record-read', method: 'post', data })
}

export function deleteContentRecordRead(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/content/record-read/${id}`, method: 'delete' })
}

/** 阅读统计（UV/PV）：GET /content/record-read/stats?contentCode= */
export function getContentReadStats(contentCode: string): Promise<ContentReadStats> {
  return request<ContentReadStats>({
    url: '/admin-api/content/record-read/stats',
    method: 'get',
    params: { contentCode }
  })
}

// ==================== 分享记录 ====================

export function pageContentRecordShare(query: ContentRecordShareQuery): Promise<PageResult<ContentRecordShare>> {
  return request<PageResult<ContentRecordShare>>({
    url: '/admin-api/content/record-share/page',
    method: 'get',
    params: query
  })
}

export function getContentRecordShare(id: number): Promise<ContentRecordShare> {
  return request<ContentRecordShare>({ url: `/admin-api/content/record-share/${id}`, method: 'get' })
}

export function createContentRecordShare(data: Partial<ContentRecordShare>): Promise<number> {
  return request<number>({ url: '/admin-api/content/record-share', method: 'post', data })
}

/** 修改分享记录：PUT /content/record-share?id=（id 走 query） */
export function updateContentRecordShare(id: number, data: Partial<ContentRecordShare>): Promise<void> {
  return request<void>({ url: '/admin-api/content/record-share', method: 'put', params: { id }, data })
}

export function deleteContentRecordShare(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/content/record-share/${id}`, method: 'delete' })
}
