# 知识仓库上传、编辑与管理能力完善设计

日期：2026-08-19
状态：已批准（用户确认功能范围：类目管理与选择、切分方式配置、检索增强配置、标签与解析器选择；切分配置时机=创建仓库时；类目管理入口=列表页按钮+弹窗；不做列表按类目筛选）

## 1. 背景与目标

admin 知识仓库当前能力：仓库 CRUD（懒建库）、文档上传→解析→自动建库/导入索引、切片查看、RAG 问答。相比百炼平台缺失：类目管理/选择、切分方式配置、检索增强配置（Embedding/重排）、文件标签与解析器选择。

目标：补齐上述能力，让 admin 知识仓库具备百炼平台同等的"数据管理"能力，并保持"实时代理百炼、本地仅缓存元数据"的既有架构。

## 2. 现状与能力差距（调研结论）

项目当前使用百炼 SDK `bailian20231229 2.0.8`，最新为 2.14.3（2026-08 发布）。差距：

| 能力 | 百炼 API | 项目现状 |
|---|---|---|
| 类目管理（多级树） | AddCategory / ListCategory / DeleteCategory | 无，固定 `default` 类目 |
| 上传选类目 | AddFile(CategoryId 必填) | 无 |
| 文件标签 | AddFile(Tags ≤10) / UpdateFileTag / BatchUpdateFileTag | 无 |
| 切分方式 | CreateIndex: chunkMode + Separator + ChunkSize + OverlapSize | 无（默认智能） |
| 检索配置 | CreateIndex/UpdateIndex: EmbeddingModelName、RerankModelName、RerankMode、RerankMinScore、EnableRewrite、Dense/SparseSimilarityTopK | 无 |
| 解析器选择 | AddFile(Parser)：DOCMIND 系列 / AUTO_SELECT | 固定 DASHSCOPE_DOCMIND |
| 文件详情 | DescribeFile：CategoryId/Tags/Parser/Status/CreateTime | 仅解析状态 |

关键事实：
1. **切分方式是索引级配置**（CreateIndex 时定），AddFile 无切分参数；**UpdateIndex 不能改切分**（仅 DenseSimilarityTopK/SparseSimilarityTopK/RerankMinScore/名称/描述）。
2. **类目是业务空间级**（所有知识库共享），非仓库级；类目支持多级（ParentCategoryId）。
3. 无"移动文件类目"API（改类目只能删除重传）；切片编辑 AddChunk/UpdateChunk/DeleteChunk 依赖 PipelineId（pipeline 场景），对当前业务无价值。

## 3. 方案决策

- **SDK 升级 2.0.8 → 2.14.3**（`dayan-server/pom.xml:49` `<bailian.version>`）。类目/标签/文件列表接口走官方 SDK；索引接口（CreateIndex/UpdateIndex）继续走已验证的 ROA 直连并扩展参数，不换通道。
- 切分/检索配置存本地 `system_knowledge_repo.config_json`（JSON 列，与近期 83-89 迁移"配置外置 JSON"方向一致）；类目/标签实时代理百炼，不落库。
- 解析器：默认保持 `DASHSCOPE_DOCMIND`（现状已验证，零回归），新增选项用新版枚举（DOCMIND_DIGITAL / DOCMIND_LLM_VERSION / AUTO_SELECT）。

## 4. 详细设计

### 4.1 数据模型（迁移 90_system_knowledge_repo_index_config.sql）

```sql
ALTER TABLE `system_knowledge_repo`
  ADD COLUMN `config_json` VARCHAR(2000) NULL
  COMMENT '索引配置 JSON（切分方式/向量模型/重排/改写/召回参数；懒建库建库时应用，已建库仅检索参数可改）'
  AFTER `description`;
```

config_json 结构（IndexConfig，Jackson 序列化，字段驼峰）：

```json
{
  "chunkMode": "regex",            // null=智能切分；"regex"=自定义（分隔符切分）
  "separator": "(?<=。)",          // 正则分隔符，仅 chunkMode=regex 生效
  "chunkSize": 500,                // 1-6000，默认 500
  "overlapSize": 100,              // 0-1024，默认 100，必须 < chunkSize（百炼 API：重叠仅在 length 切分模式生效，本项目自定义切分为 regex 模式，该参数不生效，前端创建表单不展示）
  "embeddingModel": "text-embedding-v3",  // v3 / v4，空=服务端默认
  "rerankModel": "qwen3-rerank",   // qwen3-rerank / qwen3-rerank-hybrid，空=服务端默认
  "rerankMode": "qa",              // qa / similar / custom，默认 qa
  "rerankMinScore": 0.01,          // 0.01-1.00，默认 0.01
  "enableRewrite": true,           // 多轮改写，默认 true
  "denseTopK": 4,                  // 仅已建库 UpdateIndex 用
  "sparseTopK": 4                  // 仅已建库 UpdateIndex 用
}
```

bind 模式（绑定已有索引）不写 config_json。

### 4.2 SDK 升级

- `dayan-server/pom.xml:49`：`<bailian.version>2.0.8</bailian.version>` → `2.14.3`。
- 兼容性核对（实施时构建验证）：现有 SDK 调用 addFile/describeFile/listChunks 方法签名在新版存在（已核对 2.14.3 Client 方法清单）。

### 4.3 BailianKnowledgeClient 扩展（dayan-common-aliyun）

新增常量：
- `PARSER_DOCMIND_DIGITAL = "DOCMIND_DIGITAL"`、`PARSER_DOCMIND_LLM_VERSION = "DOCMIND_LLM_VERSION"`、`PARSER_AUTO_SELECT = "AUTO_SELECT"`（保留现有 PARSER_DOCMIND = "DASHSCOPE_DOCMIND" 作为默认）

新增方法：
- `List<CategoryItem> listCategories()`：ListCategory 循环翻页（MaxResults=100）聚合全量平铺返回（含 CategoryId/CategoryName/ParentCategoryId/IsDefault）
- `String addCategory(String name, String parentCategoryId)`：AddCategory(CategoryType=UNSTRUCTURED)，返回 CategoryId
- `void deleteCategory(String categoryId)`：DeleteCategory
- `void updateFileTags(String fileId, List<String> tags)`：UpdateFileTag（tags ≤10）
- `addFile` 扩展签名：`addFile(String leaseId, String categoryId, String parser, List<String> tags)`（保留默认值兜底）
- `FileStatusInfo` 增加 categoryId/fileName/fileType/tags/createTime 字段（DescribeFile 全量映射）

扩展方法（ROA 直连，query Map 增加字段）：
- `createIndex(name, description, fileIds, IndexConfig config)`：config 非空时附加 `chunkMode`/`Separator`/`ChunkSize`/`OverlapSize`/`EmbeddingModelName`/`RerankModelName`/`RerankMode`/`RerankMinScore`/`EnableRewrite`（参数名与 SDK NameInMap 一致；EnableRewrite 传 "true"/"false"）
- `updateIndex(indexId, name, description, denseTopK, sparseTopK, rerankMinScore)`：附加 `DenseSimilarityTopK`/`SparseSimilarityTopK`/`RerankMinScore`

### 4.4 Service 层（SystemKnowledgeRepoServiceImpl）

- `create(dto)`：dto.indexConfig 非空时序列化写入 repo.configJson（bind 模式忽略并告警日志）
- `initIndex(id, fileIds)`：读 repo.configJson → 反序列化 IndexConfig → 传入 createIndex
- `update(id, dto)`：扩展 indexConfig 处理：
  - 未建库（indexId 空）：全量写入 configJson
  - 已建库：仅 denseTopK/sparseTopK/rerankMinScore 变更 → UpdateIndex 同步并更新本地 configJson；chunkMode/separator/chunkSize/overlapSize/embeddingModel/rerankModel/rerankMode/enableRewrite 变更 → 抛 BusinessException "切分方式、向量模型、重排模型等配置在建库后不可修改"
  - 名称/描述同步逻辑保留
- `uploadDocument(id, file, categoryId, parser, tags)`：applyUploadLease → uploadBinary → addFile(leaseId, categoryId, parser, tags)；categoryId 空默认 `default`，parser 空默认 PARSER_DOCMIND，tags 空不传
- `getDocumentParseStatus`：VO 增加 categoryId/tags/parser（categoryName 由前端用已加载类目树映射，后端不查）
- 新增 `listCategories()/addCategory(name, parentId)/deleteCategory(categoryId)/updateDocTags(id, fileId, tags)`（透传客户端，仓库存在性校验）
- IndexConfig 序列化/校验抽为独立工具方法（可单测）：chunkSize 1-6000、overlapSize 0-1024 且 < chunkSize、rerankMinScore 0.01-1、tags ≤10

### 4.5 DTO / VO / Controller

- `SystemKnowledgeRepoCreateDTO`：+ `IndexConfig indexConfig`
- `SystemKnowledgeRepoUpdateDTO`：+ `IndexConfig indexConfig`
- `SystemKnowledgeRepoVO`：+ `IndexConfig indexConfig`（configJson 解析）
- `SystemKnowledgeDocVO`：+ categoryId/tags/parser（详情接口用）
- 新增 `SystemCategoryVO`（categoryId/categoryName/parentCategoryId/isDefault）
- `SystemKnowledgeRepoAdminController` 新增：
  - `GET /system/knowledge/categories`（权限 system:knowledge:repo:list）
  - `POST /system/knowledge/categories`（权限 system:knowledge:repo:create）
  - `DELETE /system/knowledge/categories/{categoryId}`（权限 system:knowledge:repo:delete）
  - `PUT /repos/{id}/documents/{fileId}/tags`（权限 system:knowledge:doc:upload）
  - `POST /repos/{id}/documents` 增加 `@RequestParam categoryId/parser/tags(List<String>)` 可选参数
- 复用现有 system:knowledge:* 权限码，不新增权限定义（迁移只加列，不动权限表）

### 4.6 前端（dayan-admin）

- `api/knowledge.ts`：+ listCategories/addCategory/deleteCategory/updateDocTags；uploadKnowledgeDoc 增加 categoryId/parser/tags 参数
- `types/knowledge.ts`：+ `KnowledgeCategory`、`IndexConfig` 类型
- 新增 `src/components/KnowledgeCategoryDialog/index.vue`（列表页使用）：
  - el-tree 展示多级类目（平铺数据组树），根节点含百炼内置 `default`（只读标记"默认"）
  - 节点操作：新增子类目（弹输入框）、删除（confirm，透传百炼错误如"类目下有文件不可删"）
- `system/knowledge/index.vue`：工具栏加"类目管理"按钮 → 打开弹窗；创建弹窗中"自定义切分"（regex）子区仅展示分隔符/切块长度，**不展示重叠长度**（百炼 API 重叠仅在 length 切分模式生效，regex 模式无效；overlapSize 字段保留，详情页仍展示以兼容旧数据）
- `system/knowledge/detail/index.vue` 基本信息区：
  - 展示索引配置（切分方式/分隔符/切块长度/重叠/Embedding/重排/改写/召回参数），未配置显示"使用百炼默认"；重叠长度仅展示不编辑（regex 模式不生效）
  - 已建库时"检索参数"（denseTopK/sparseTopK/rerankMinScore）可编辑（小表单提交）；切分等其余字段只读
- `system/knowledge/detail/DocTab.vue`：
  - 拖入/选择文件 → 弹"上传设置"对话框：文件名列表 + 类目树选择（默认 default）+ 解析器下拉（智能/电子文档/大模型/自动）+ 标签（el-select multiple allow-create filterable，≤10）→ 确认后逐个上传
  - 文件表格加"标签"列（el-tag）；操作加"详情"（类目名/标签/解析器/文件ID/大小/创建时间/解析状态）、"编辑标签"
  - 类目名映射：DocTab 加载时拉一次类目树存 Map<categoryId, name>
- channel 端（dayan-channel 前端）知识库页保持一致改动（同结构复制，文件级同步）

### 4.7 权限

类目 CRUD 与标签编辑复用现有 system:knowledge:* 权限码（见 4.5），不加新码、不改迁移种子。

## 5. 限制与权衡（明确不做）

- 切分方式、Embedding/重排模型建库后不可改（百炼 UpdateIndex 不支持）——UI 只读 + 后端校验报错
- 文件不可移动类目（无 API），需删除重传
- 不做列表按类目筛选（索引维度列表无类目字段，拼接复杂度高）
- 不做切片编辑（AddChunk/UpdateChunk 依赖 pipeline）
- 不做音视频解析（DOCMIND_LLM_VERSION_MEDIA）
- bind 模式不展示/管理索引配置（远端索引配置未知，避免误导）

## 6. 测试计划

- 后端单测（dayan-common-core 或 module-system test）：
  - IndexConfig Jackson 序列化/反序列化往返
  - 校验逻辑：chunkSize/overlapSize/rerankMinScore 边界、overlapSize ≥ chunkSize 报错、tags >10 报错
  - update 校验：已建库改 chunk 字段 → BusinessException（校验逻辑抽静态方法，mock mapper 或直接测方法）
- 前端：vue-tsc 类型检查
- 构建：mvn 编译 dayan-common-aliyun / dayan-module-system / dayan-admin starter
- 联调（需真实百炼凭据，交付后人工验证）：类目增删、上传选类目/解析器/标签、懒建库应用切分配置、已建库检索参数更新、详情展示

## 7. 实施顺序

1. 迁移 90 + SDK 版本升级
2. BailianKnowledgeClient 扩展（常量/方法/模型）
3. IndexConfig 工具（序列化+校验）+ DTO/VO/Service/Controller
4. 后端单测 + 构建验证
5. 前端 api/types + CategoryDialog + 列表页按钮
6. 前端创建弹窗（切分/检索配置）+ DocTab（上传设置/标签列/详情）+ 详情页（配置展示/检索参数编辑）
7. channel 前端同步
8. 前端类型检查 + 全量构建验证
