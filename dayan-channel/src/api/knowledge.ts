/**
 * 知识仓库 API（Channel 端本渠道知识库管理）。
 *
 * 对齐后端 ChannelKnowledgeController（/channel-api/system/knowledge/repos）。
 * 本渠道由后端 ContextHolder 强制注入，前端不传 channelCode；
 * 创建接口 repoType=2 由后端固定，前端提交名称/描述/索引配置。
 */
import { request } from '@/utils/request'
import type {
  KnowledgeRepo,
  KnowledgeRepoTreeNode,
  KnowledgeDoc,
  KnowledgeChunk,
  KnowledgeChatResult,
  KnowledgeIndexConfig,
  KnowledgeCategory
} from '@/types/knowledge'

/** 本渠道仓库详情（未创建返回 null） */
export function getCurrentKnowledgeRepo(): Promise<KnowledgeRepo | null> {
  return request<KnowledgeRepo | null>({ url: '/channel-api/system/knowledge/repos/current', method: 'get' })
}

/** 渠道树形知识库（本渠道 + 全部后代；每节点含独立库/继承来源/实际可用库） */
export function getKnowledgeRepoTree(): Promise<KnowledgeRepoTreeNode[]> {
  return request<KnowledgeRepoTreeNode[]>({ url: '/channel-api/system/knowledge/repos/tree', method: 'get' })
}

/** 创建本渠道仓库（懒建库，上传首个文档解析成功后自动在百炼建库） */
export function createKnowledgeRepo(data: {
  repoName: string
  description?: string
  /** 索引配置（切分方式/检索参数；建库后不可修改） */
  indexConfig?: KnowledgeIndexConfig
}): Promise<number> {
  return request<number>({ url: '/channel-api/system/knowledge/repos', method: 'post', data })
}

/** 更新仓库（名称/描述/排序） */
export function updateKnowledgeRepo(id: number, data: Partial<KnowledgeRepo>): Promise<void> {
  return request<void>({ url: `/channel-api/system/knowledge/repos/${id}`, method: 'put', data })
}

/** 删除仓库（同时删除百炼远端索引） */
export function deleteKnowledgeRepo(id: number): Promise<void> {
  return request<void>({ url: `/channel-api/system/knowledge/repos/${id}`, method: 'delete' })
}

/** 同步远端（刷新文档数与状态） */
export function syncKnowledgeRepo(id: number): Promise<void> {
  return request<void>({ url: `/channel-api/system/knowledge/repos/${id}/sync`, method: 'post' })
}

/** 懒建库：用已解析文件在百炼创建知识库（返回构建任务 JobId） */
export function initKnowledgeRepo(id: number, fileIds: string[]): Promise<string> {
  return request<string>({ url: `/channel-api/system/knowledge/repos/${id}/init-index`, method: 'post', data: { fileIds } })
}

/** 建库索引构建任务状态（RUNNING/FINISH/FAILED/UNBOUND） */
export function getKnowledgeRepoBuildStatus(id: number): Promise<string> {
  return request<string>({ url: `/channel-api/system/knowledge/repos/${id}/build-status`, method: 'get' })
}

// ---------- 文档管理 ----------

/** 文档列表（实时代理百炼） */
export function listKnowledgeDocs(
  id: number,
  params: { pageNumber?: number; pageSize?: number; documentName?: string; documentStatus?: string }
): Promise<KnowledgeDoc[]> {
  return request<KnowledgeDoc[]>({ url: `/channel-api/system/knowledge/repos/${id}/documents`, method: 'get', params })
}

/** 上传文档（multipart；可指定类目/解析器/标签，返回百炼 FileId，解析异步） */
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
    url: `/channel-api/system/knowledge/repos/${id}/documents`,
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' },
    silent
  })
}

/** 类目列表（全量平铺） */
export function listKnowledgeCategories(): Promise<KnowledgeCategory[]> {
  return request<KnowledgeCategory[]>({ url: '/channel-api/system/knowledge/categories', method: 'get' })
}

/** 新增类目 */
export function addKnowledgeCategory(data: { categoryName: string; parentCategoryId?: string }): Promise<string> {
  return request<string>({ url: '/channel-api/system/knowledge/categories', method: 'post', data })
}

/** 删除类目 */
export function deleteKnowledgeCategory(categoryId: string): Promise<void> {
  return request<void>({ url: `/channel-api/system/knowledge/categories/${categoryId}`, method: 'delete' })
}

/** 更新文件标签（≤10，空=清空） */
export function updateKnowledgeDocTags(id: number, fileId: string, tags: string[]): Promise<void> {
  return request<void>({
    url: `/channel-api/system/knowledge/repos/${id}/documents/${fileId}/tags`,
    method: 'put',
    data: { tags }
  })
}

/** 文件解析状态 */
export function getKnowledgeDocParseStatus(id: number, fileId: string): Promise<KnowledgeDoc> {
  return request<KnowledgeDoc>({ url: `/channel-api/system/knowledge/repos/${id}/documents/${fileId}`, method: 'get' })
}

/** 已解析文档导入索引（返回任务 JobId） */
export function importKnowledgeDocs(id: number, fileIds: string[]): Promise<string> {
  return request<string>({ url: `/channel-api/system/knowledge/repos/${id}/documents/import`, method: 'post', data: { fileIds } })
}

/** 文档导入索引任务状态（RUNNING/FINISH/FAILED） */
export function getKnowledgeImportStatus(id: number, jobId: string): Promise<string> {
  return request<string>({ url: `/channel-api/system/knowledge/repos/${id}/import-status/${jobId}`, method: 'get' })
}

/** 删除索引内文档（远端永久删除） */
export function deleteKnowledgeDoc(id: number, fileId: string): Promise<void> {
  return request<void>({ url: `/channel-api/system/knowledge/repos/${id}/documents/${fileId}`, method: 'delete' })
}

/** 文档切片列表（切片管理，分页实时代理百炼） */
export function listKnowledgeDocChunks(
  id: number,
  fileId: string,
  params: { pageNum?: number; pageSize?: number }
): Promise<{ total: number; chunks: KnowledgeChunk[] }> {
  return request<{ total: number; chunks: KnowledgeChunk[] }>({
    url: `/channel-api/system/knowledge/repos/${id}/documents/${fileId}/chunks`,
    method: 'get',
    params
  })
}

// ---------- 问答 / 检索 ----------

/** 知识库问答（RAG） */
export function chatKnowledgeRepo(id: number, data: { question: string; topK?: number }): Promise<KnowledgeChatResult> {
  return request<KnowledgeChatResult>({ url: `/channel-api/system/knowledge/repos/${id}/chat`, method: 'post', data })
}

/** 检索测试（仅召回片段） */
export function retrieveKnowledgeRepo(
  id: number,
  params: { query: string; topK?: number }
): Promise<KnowledgeChatResult['citations']> {
  return request<KnowledgeChatResult['citations']>({
    url: `/channel-api/system/knowledge/repos/${id}/retrieve`,
    method: 'get',
    params
  })
}
