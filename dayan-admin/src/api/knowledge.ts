/**
 * 知识仓库 API（对齐后端 KnowledgeRepoAdminController，RESTful /admin-api/knowledge/repos）。
 * 主键：仓库用自增 id；文档用百炼 FileId（UUID）。
 */
import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  KnowledgeRepo,
  KnowledgeRepoQuery,
  KnowledgeRepoCreatePayload,
  KnowledgeDoc,
  KnowledgeChatResult
} from '@/types/knowledge'

/** 仓库分页列表 */
export function pageKnowledgeRepos(params: KnowledgeRepoQuery): Promise<PageResult<KnowledgeRepo>> {
  return request<PageResult<KnowledgeRepo>>({ url: '/admin-api/knowledge/repos/page', method: 'get', params })
}

/** 仓库详情 */
export function getKnowledgeRepo(id: number): Promise<KnowledgeRepo> {
  return request<KnowledgeRepo>({ url: `/admin-api/knowledge/repos/${id}`, method: 'get' })
}

/** 创建仓库（mode=create 新建远端索引 / mode=bind 绑定已有 IndexId） */
export function createKnowledgeRepo(data: KnowledgeRepoCreatePayload): Promise<number> {
  return request<number>({ url: '/admin-api/knowledge/repos', method: 'post', data })
}

/** 更新仓库（名称/描述/排序） */
export function updateKnowledgeRepo(id: number, data: Partial<KnowledgeRepo>): Promise<void> {
  return request<void>({ url: `/admin-api/knowledge/repos/${id}`, method: 'put', data })
}

/** 删除仓库（同时删除百炼远端索引） */
export function deleteKnowledgeRepo(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/knowledge/repos/${id}`, method: 'delete' })
}

/** 同步远端（刷新文档数与状态） */
export function syncKnowledgeRepo(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/knowledge/repos/${id}/sync`, method: 'post' })
}

/** 懒建库：用已解析文件在百炼创建知识库（返回构建任务 JobId） */
export function initKnowledgeRepo(id: number, fileIds: string[]): Promise<string> {
  return request<string>({ url: `/admin-api/knowledge/repos/${id}/init-index`, method: 'post', data: { fileIds } })
}

/** 建库索引构建任务状态（RUNNING/FINISH/FAILED/UNBOUND） */
export function getKnowledgeRepoBuildStatus(id: number): Promise<string> {
  return request<string>({ url: `/admin-api/knowledge/repos/${id}/build-status`, method: 'get' })
}

// ---------- 文档管理 ----------

/** 文档列表（实时代理百炼） */
export function listKnowledgeDocs(
  id: number,
  params: { pageNumber?: number; pageSize?: number; documentName?: string; documentStatus?: string }
): Promise<KnowledgeDoc[]> {
  return request<KnowledgeDoc[]>({ url: `/admin-api/knowledge/repos/${id}/documents`, method: 'get', params })
}

/** 上传文档（multipart，返回百炼 FileId，解析异步） */
export function uploadKnowledgeDoc(id: number, file: File): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  return request<string>({
    url: `/admin-api/knowledge/repos/${id}/documents`,
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 文件解析状态 */
export function getKnowledgeDocParseStatus(id: number, fileId: string): Promise<KnowledgeDoc> {
  return request<KnowledgeDoc>({ url: `/admin-api/knowledge/repos/${id}/documents/${fileId}`, method: 'get' })
}

/** 已解析文档导入索引（返回任务 JobId） */
export function importKnowledgeDocs(id: number, fileIds: string[]): Promise<string> {
  return request<string>({ url: `/admin-api/knowledge/repos/${id}/documents/import`, method: 'post', data: { fileIds } })
}

/** 文档导入索引任务状态（RUNNING/FINISH/FAILED） */
export function getKnowledgeImportStatus(id: number, jobId: string): Promise<string> {
  return request<string>({ url: `/admin-api/knowledge/repos/${id}/import-status/${jobId}`, method: 'get' })
}

/** 删除索引内文档（远端永久删除） */
export function deleteKnowledgeDoc(id: number, fileId: string): Promise<void> {
  return request<void>({ url: `/admin-api/knowledge/repos/${id}/documents/${fileId}`, method: 'delete' })
}

// ---------- 问答 / 检索 ----------

/** 知识库问答（RAG） */
export function chatKnowledgeRepo(id: number, data: { question: string; topK?: number }): Promise<KnowledgeChatResult> {
  return request<KnowledgeChatResult>({ url: `/admin-api/knowledge/repos/${id}/chat`, method: 'post', data })
}

/** 检索测试（仅召回片段） */
export function retrieveKnowledgeRepo(
  id: number,
  params: { query: string; topK?: number }
): Promise<KnowledgeChatResult['citations']> {
  return request<KnowledgeChatResult['citations']>({
    url: `/admin-api/knowledge/repos/${id}/retrieve`,
    method: 'get',
    params
  })
}
