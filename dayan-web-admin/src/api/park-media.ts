import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ParkMediaImage,
  ParkMediaImageQuery,
  ParkMediaVideo,
  ParkMediaVideoQuery,
  ParkMediaFile,
  ParkMediaFileQuery,
  ParkMediaVr,
  ParkMediaVrQuery
} from '@/types/park'

/**
 * 机构媒体库接口封装（图片 / 视频 / 文档 / VR 四表）。
 *
 * 对应后端 /admin-api/park/media-image|media-video|media-file|media-vr。
 *
 * 注意：
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - 各 url 字段（imageUrl/videoUrl/fileUrl/vrUrl）后端 @NotBlank 必填。
 * - /list 只接 parkCode 一参，返回数组非分页。
 */

// ---------------- 媒体-图片（media-image）----------------

/** 图片分页：GET /admin-api/park/media-image/page */
export function pageMediaImages(query: ParkMediaImageQuery): Promise<PageResult<ParkMediaImage>> {
  return request<PageResult<ParkMediaImage>>({
    url: '/admin-api/park/media-image/page',
    method: 'get',
    params: query
  })
}

/** 图片列表（全量，按 parkCode 过滤）：GET /admin-api/park/media-image/list?parkCode=xxx */
export function listMediaImages(parkCode: string): Promise<ParkMediaImage[]> {
  return request<ParkMediaImage[]>({
    url: '/admin-api/park/media-image/list',
    method: 'get',
    params: { parkCode }
  })
}

/** 图片详情：GET /admin-api/park/media-image/{id} */
export function getMediaImage(id: number): Promise<ParkMediaImage> {
  return request<ParkMediaImage>({
    url: `/admin-api/park/media-image/${id}`,
    method: 'get'
  })
}

/** 新增图片：POST /admin-api/park/media-image（返回新 id） */
export function createMediaImage(data: Partial<ParkMediaImage>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/media-image',
    method: 'post',
    data
  })
}

/** 修改图片：PUT /admin-api/park/media-image/{id} */
export function updateMediaImage(id: number, data: Partial<ParkMediaImage>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/media-image/${id}`,
    method: 'put',
    data
  })
}

/** 删除图片：DELETE /admin-api/park/media-image/{id} */
export function deleteMediaImage(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/media-image/${id}`,
    method: 'delete'
  })
}

// ---------------- 媒体-视频（media-video）----------------

/** 视频分页：GET /admin-api/park/media-video/page */
export function pageMediaVideos(query: ParkMediaVideoQuery): Promise<PageResult<ParkMediaVideo>> {
  return request<PageResult<ParkMediaVideo>>({
    url: '/admin-api/park/media-video/page',
    method: 'get',
    params: query
  })
}

/** 视频列表（全量，按 parkCode 过滤）：GET /admin-api/park/media-video/list?parkCode=xxx */
export function listMediaVideos(parkCode: string): Promise<ParkMediaVideo[]> {
  return request<ParkMediaVideo[]>({
    url: '/admin-api/park/media-video/list',
    method: 'get',
    params: { parkCode }
  })
}

/** 视频详情：GET /admin-api/park/media-video/{id} */
export function getMediaVideo(id: number): Promise<ParkMediaVideo> {
  return request<ParkMediaVideo>({
    url: `/admin-api/park/media-video/${id}`,
    method: 'get'
  })
}

/** 新增视频：POST /admin-api/park/media-video */
export function createMediaVideo(data: Partial<ParkMediaVideo>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/media-video',
    method: 'post',
    data
  })
}

/** 修改视频：PUT /admin-api/park/media-video/{id} */
export function updateMediaVideo(id: number, data: Partial<ParkMediaVideo>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/media-video/${id}`,
    method: 'put',
    data
  })
}

/** 删除视频：DELETE /admin-api/park/media-video/{id} */
export function deleteMediaVideo(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/media-video/${id}`,
    method: 'delete'
  })
}

// ---------------- 媒体-文档（media-file）----------------

/** 文档分页：GET /admin-api/park/media-file/page */
export function pageMediaFiles(query: ParkMediaFileQuery): Promise<PageResult<ParkMediaFile>> {
  return request<PageResult<ParkMediaFile>>({
    url: '/admin-api/park/media-file/page',
    method: 'get',
    params: query
  })
}

/** 文档列表（全量，按 parkCode 过滤）：GET /admin-api/park/media-file/list?parkCode=xxx */
export function listMediaFiles(parkCode: string): Promise<ParkMediaFile[]> {
  return request<ParkMediaFile[]>({
    url: '/admin-api/park/media-file/list',
    method: 'get',
    params: { parkCode }
  })
}

/** 文档详情：GET /admin-api/park/media-file/{id} */
export function getMediaFile(id: number): Promise<ParkMediaFile> {
  return request<ParkMediaFile>({
    url: `/admin-api/park/media-file/${id}`,
    method: 'get'
  })
}

/** 新增文档：POST /admin-api/park/media-file */
export function createMediaFile(data: Partial<ParkMediaFile>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/media-file',
    method: 'post',
    data
  })
}

/** 修改文档：PUT /admin-api/park/media-file/{id} */
export function updateMediaFile(id: number, data: Partial<ParkMediaFile>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/media-file/${id}`,
    method: 'put',
    data
  })
}

/** 删除文档：DELETE /admin-api/park/media-file/{id} */
export function deleteMediaFile(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/media-file/${id}`,
    method: 'delete'
  })
}

// ---------------- 媒体-VR（media-vr）----------------

/** VR 分页：GET /admin-api/park/media-vr/page */
export function pageMediaVrs(query: ParkMediaVrQuery): Promise<PageResult<ParkMediaVr>> {
  return request<PageResult<ParkMediaVr>>({
    url: '/admin-api/park/media-vr/page',
    method: 'get',
    params: query
  })
}

/** VR 列表（全量，按 parkCode 过滤）：GET /admin-api/park/media-vr/list?parkCode=xxx */
export function listMediaVrs(parkCode: string): Promise<ParkMediaVr[]> {
  return request<ParkMediaVr[]>({
    url: '/admin-api/park/media-vr/list',
    method: 'get',
    params: { parkCode }
  })
}

/** VR 详情：GET /admin-api/park/media-vr/{id} */
export function getMediaVr(id: number): Promise<ParkMediaVr> {
  return request<ParkMediaVr>({
    url: `/admin-api/park/media-vr/${id}`,
    method: 'get'
  })
}

/** 新增 VR：POST /admin-api/park/media-vr */
export function createMediaVr(data: Partial<ParkMediaVr>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/media-vr',
    method: 'post',
    data
  })
}

/** 修改 VR：PUT /admin-api/park/media-vr/{id} */
export function updateMediaVr(id: number, data: Partial<ParkMediaVr>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/media-vr/${id}`,
    method: 'put',
    data
  })
}

/** 删除 VR：DELETE /admin-api/park/media-vr/{id} */
export function deleteMediaVr(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/media-vr/${id}`,
    method: 'delete'
  })
}
