/**
 * 知识仓库 API（对齐后端 KnowledgeRepoAdminController，RESTful /admin-api/system/knowledge/repos）。
 * 主键：仓库用自增 id；文档用百炼 FileId（UUID）。
 */
import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  KnowledgeRepo,
  KnowledgeRepoTreeNode,
  KnowledgeRepoQuery,
  KnowledgeRepoCreatePayload,
  KnowledgeDoc,
  KnowledgeChunk,
  KnowledgeChatResult,
  KnowledgeCategory
} from '@/types/knowledge'

/** 仓库分页列表 */
export function pageKnowledgeRepos(params: KnowledgeRepoQuery): Promise<PageResult<KnowledgeRepo>> {
  return request<PageResult<KnowledgeRepo>>({ url: '/admin-api/system/knowledge/repos/page', method: 'get', params })
}

/** 渠道树形知识库（root 渠道 + 全部后代；channelCode 为空 = 全渠道树） */
export function getKnowledgeRepoTree(channelCode?: string): Promise<KnowledgeRepoTreeNode[]> {
  return request<KnowledgeRepoTreeNode[]>({
    url: '/admin-api/system/knowledge/repos/tree',
    method: 'get',
    params: channelCode ? { channelCode } : {}
  })
}

/** 仓库详情 */
export function getKnowledgeRepo(id: number): Promise<KnowledgeRepo> {
  return request<KnowledgeRepo>({ url: `/admin-api/system/knowledge/repos/${id}`, method: 'get' })
}

/** 创建仓库（mode=create 新建远端索引 / mode=bind 绑定已有 IndexId） */
export function createKnowledgeRepo(data: KnowledgeRepoCreatePayload): Promise<number> {
  return request<number>({ url: '/admin-api/system/knowledge/repos', method: 'post', data })
}

/** 更新仓库（名称/描述/排序） */
export function updateKnowledgeRepo(id: number, data: Partial<KnowledgeRepo>): Promise<void> {
  return request<void>({ url: `/admin-api/system/knowledge/repos/${id}`, method: 'put', data })
}

/** 删除仓库（同时删除百炼远端索引） */
export function deleteKnowledgeRepo(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/system/knowledge/repos/${id}`, method: 'delete' })
}

/** 同步远端（刷新文档数与状态） */
export function syncKnowledgeRepo(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/system/knowledge/repos/${id}/sync`, method: 'post' })
}

/** 懒建库：用已解析文件在百炼创建知识库（返回构建任务 JobId） */
export function initKnowledgeRepo(id: number, fileIds: string[]): Promise<string> {
  return request<string>({ url: `/admin-api/system/knowledge/repos/${id}/init-index`, method: 'post', data: { fileIds } })
}

/** 建库索引构建任务状态（RUNNING/FINISH/FAILED/UNBOUND） */
export function getKnowledgeRepoBuildStatus(id: number): Promise<string> {
  return request<string>({ url: `/admin-api/system/knowledge/repos/${id}/build-status`, method: 'get' })
}

// ---------- 文档管理 ----------

/** 文档列表（实时代理百炼） */
export function listKnowledgeDocs(
  id: number,
  params: { pageNumber?: number; pageSize?: number; documentName?: string; documentStatus?: string }
): Promise<KnowledgeDoc[]> {
  return request<KnowledgeDoc[]>({ url: `/admin-api/system/knowledge/repos/${id}/documents`, method: 'get', params })
}

/** 上传文档（multipart；可指定类目/解析器/标签，返回百炼 FileId） */
export function uploadKnowledgeDoc(
  id: number,
  file: File,
  options?: { categoryId?: string; parser?: string; tags?: string[] },
  silent = false
): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  if (options?.categoryId) form.append('categoryId', options.categoryId)
  if (options?.parser) form.append('parser', options.parser)
  options?.tags?.forEach((t) => form.append('tags', t))
  return request<string>({
    url: `/admin-api/system/knowledge/repos/${id}/documents`,
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' },
    silent
  })
}

/** 类目列表（全量平铺） */
export function listKnowledgeCategories(): Promise<KnowledgeCategory[]> {
  return request<KnowledgeCategory[]>({ url: '/admin-api/system/knowledge/categories', method: 'get' })
}

/** 新增类目 */
export function addKnowledgeCategory(data: { categoryName: string; parentCategoryId?: string }): Promise<string> {
  return request<string>({ url: '/admin-api/system/knowledge/categories', method: 'post', data })
}

/** 删除类目 */
export function deleteKnowledgeCategory(categoryId: string): Promise<void> {
  return request<void>({ url: `/admin-api/system/knowledge/categories/${categoryId}`, method: 'delete' })
}

/** 更新文件标签（≤10，空=清空） */
export function updateKnowledgeDocTags(id: number, fileId: string, tags: string[]): Promise<void> {
  return request<void>({
    url: `/admin-api/system/knowledge/repos/${id}/documents/${fileId}/tags`,
    method: 'put',
    data: { tags }
  })
}

/** 文件解析状态 */
export function getKnowledgeDocParseStatus(id: number, fileId: string): Promise<KnowledgeDoc> {
  return request<KnowledgeDoc>({ url: `/admin-api/system/knowledge/repos/${id}/documents/${fileId}`, method: 'get' })
}

/** 已解析文档导入索引（返回任务 JobId） */
export function importKnowledgeDocs(id: number, fileIds: string[]): Promise<string> {
  return request<string>({ url: `/admin-api/system/knowledge/repos/${id}/documents/import`, method: 'post', data: { fileIds } })
}

/** 文档导入索引任务状态（RUNNING/FINISH/FAILED） */
export function getKnowledgeImportStatus(id: number, jobId: string): Promise<string> {
  return request<string>({ url: `/admin-api/system/knowledge/repos/${id}/import-status/${jobId}`, method: 'get' })
}

/** 删除索引内文档（远端永久删除） */
export function deleteKnowledgeDoc(id: number, fileId: string): Promise<void> {
  return request<void>({ url: `/admin-api/system/knowledge/repos/${id}/documents/${fileId}`, method: 'delete' })
}

/** 文档切片列表（切片管理，分页实时代理百炼） */
export function listKnowledgeDocChunks(
  id: number,
  fileId: string,
  params: { pageNum?: number; pageSize?: number }
): Promise<{ total: number; chunks: KnowledgeChunk[] }> {
  return request<{ total: number; chunks: KnowledgeChunk[] }>({
    url: `/admin-api/system/knowledge/repos/${id}/documents/${fileId}/chunks`,
    method: 'get',
    params
  })
}

// ---------- 问答 / 检索 ----------

/** 知识库问答（RAG） */
export function chatKnowledgeRepo(id: number, data: { question: string; topK?: number }): Promise<KnowledgeChatResult> {
  return request<KnowledgeChatResult>({ url: `/admin-api/system/knowledge/repos/${id}/chat`, method: 'post', data })
}

/** 检索测试（仅召回片段） */
export function retrieveKnowledgeRepo(
  id: number,
  params: { query: string; topK?: number }
): Promise<KnowledgeChatResult['citations']> {
  return request<KnowledgeChatResult['citations']>({
    url: `/admin-api/system/knowledge/repos/${id}/retrieve`,
    method: 'get',
    params
  })
}
