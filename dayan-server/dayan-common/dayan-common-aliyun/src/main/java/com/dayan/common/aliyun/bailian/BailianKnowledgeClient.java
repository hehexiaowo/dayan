package com.dayan.common.aliyun.bailian;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 百炼知识库管理客户端。
 *
 * <p>两类接口（签名与参数要求不同，分别处理）：
 * <ul>
 *   <li>索引（知识库）接口 /{ws}/index/*（CreateIndex / SubmitIndexJob / GetIndexJobStatus /
 *       SubmitIndexAddDocumentsJob / ListIndexDocuments / DeleteIndexDocument / DeleteIndex / Retrieve）：
 *       ROA 风格 POST，文档列表参数为驼峰 <b>DocumentIds</b>（JSON 数组；服务端错误文案中的
 *       "file_ids" 为误导性模板文案）——统一走 {@link BailianRoaClient} 手写签名直连；</li>
 *   <li>数据连接（文件上传链路）/{ws}/datacenter/*（ApplyFileUploadLease → OSS PUT 直传 →
 *       AddFile → DescribeFile）：ROA 风格，参数与服务端一致——走官方 SDK。</li>
 * </ul>
 *
 * <p>所有方法抛 {@link BusinessException}，业务层无需感知签名/协议细节。
 */
public class BailianKnowledgeClient {

    /** 默认类目（百炼控制台「default」类目） */
    public static final String CATEGORY_DEFAULT = "default";
    /** 文件解析器：阿里云智能文档解析（默认，已验证兼容） */
    public static final String PARSER_DOCMIND = "DASHSCOPE_DOCMIND";
    /** 文件解析器：电子文档解析 */
    public static final String PARSER_DOCMIND_DIGITAL = "DOCMIND_DIGITAL";
    /** 文件解析器：大模型文档解析 */
    public static final String PARSER_DOCMIND_LLM_VERSION = "DOCMIND_LLM_VERSION";
    /** 文件解析器：自动选择 */
    public static final String PARSER_AUTO_SELECT = "AUTO_SELECT";
    /** 知识库数据源：数据中心-文件 */
    public static final String SOURCE_TYPE_DATA_CENTER_FILE = "DATA_CENTER_FILE";
    /** 非结构化文档知识库 */
    public static final String STRUCTURE_TYPE_UNSTRUCTURED = "unstructured";
    /** 百炼托管存储 */
    public static final String SINK_TYPE_DEFAULT = "DEFAULT";
    /** API 版本 */
    private static final String API_VERSION = "2023-12-29";
    /** 成功状态码（百炼 ROA 返回 Status 字段） */
    private static final String STATUS_OK = "200";

    private final com.aliyun.bailian20231229.Client sdkClient;
    private final BailianRoaClient roaClient;
    private final String workspaceId;

    public BailianKnowledgeClient(String accessKeyId, String accessKeySecret, String region, String workspaceId) {
        this.workspaceId = workspaceId;
        this.roaClient = new BailianRoaClient(accessKeyId, accessKeySecret, region);
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint("bailian." + (region == null || region.isBlank() ? "cn-beijing" : region) + ".aliyuncs.com");
        try {
            this.sdkClient = new com.aliyun.bailian20231229.Client(config);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "百炼客户端初始化失败: " + e.getMessage());
        }
    }

    // ==================== 索引（知识库）管理 ====================

    /**
     * 创建文档知识库并提交索引构建任务。
     * 百炼要求创建时至少导入一个已解析文件（file_ids 非空）。
     *
     * @return indexId（CreateIndex 返回 Data.Id）与 jobId（SubmitIndexJob 返回 Data.Id）
     */
    public CreateIndexResult createIndex(String name, String description, List<String> fileIds) {
        return createIndex(name, description, fileIds, null);
    }

    /** 创建文档知识库（可带切分/检索配置；config 为 null 时用百炼默认） */
    public CreateIndexResult createIndex(String name, String description, List<String> fileIds,
                                         Map<String, String> indexConfig) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "创建百炼知识库必须携带至少一个已解析文件（file_ids）");
        }
        Map<String, String> query = new HashMap<>();
        query.put("Name", name);
        if (description != null && !description.isBlank()) {
            query.put("Description", description);
        }
        query.put("SourceType", SOURCE_TYPE_DATA_CENTER_FILE);
        query.put("StructureType", STRUCTURE_TYPE_UNSTRUCTURED);
        query.put("SinkType", SINK_TYPE_DEFAULT);
        query.put("DocumentIds", JSONUtil.toJsonStr(fileIds));
        if (indexConfig != null) {
            query.putAll(indexConfig);
        }
        JSONObject resp = callIndex("CreateIndex", "/index/create", query);
        String indexId = resp.getJSONObject("Data").getStr("Id");
        String jobId = submitIndexJob(indexId);
        return new CreateIndexResult(indexId, jobId);
    }

    /** 提交索引构建任务，返回 JobId */
    public String submitIndexJob(String indexId) {
        Map<String, String> query = new HashMap<>();
        query.put("IndexId", indexId);
        JSONObject resp = callIndex("SubmitIndexJob", "/index/submit_index_job", query);
        return resp.getJSONObject("Data").getStr("Id");
    }

    /** 向已有知识库追加导入文档（文档须已解析成功），返回 JobId */
    public String submitAddDocumentsJob(String indexId, List<String> documentIds) {
        Map<String, String> query = new HashMap<>();
        query.put("IndexId", indexId);
        query.put("SourceType", SOURCE_TYPE_DATA_CENTER_FILE);
        query.put("DocumentIds", JSONUtil.toJsonStr(documentIds));
        JSONObject resp = callIndex("SubmitIndexAddDocumentsJob", "/index/add_documents_to_index", query);
        return resp.getJSONObject("Data").getStr("Id");
    }

    /** 查询索引任务状态（RUNNING/FINISH/FAILED 等），附文档级明细 */
    public IndexJobStatus getIndexJobStatus(String indexId, String jobId) {
        Map<String, String> query = new HashMap<>();
        query.put("IndexId", indexId);
        query.put("JobId", jobId);
        query.put("PageNumber", "1");
        query.put("PageSize", "100");
        JSONObject resp = callGet("GetIndexJobStatus", "/index/job/status", query);
        JSONObject data = resp.getJSONObject("Data");
        List<DocStatus> docs = new ArrayList<>();
        JSONArray arr = data.getJSONArray("Documents");
        if (arr != null) {
            for (Object o : arr) {
                JSONObject d = (JSONObject) o;
                docs.add(new DocStatus(d.getStr("DocId"), d.getStr("DocName"),
                        d.getStr("Status"), d.getStr("Message")));
            }
        }
        return new IndexJobStatus(data.getStr("Status"), data.getStr("JobId"), docs);
    }

    /** 分页查询知识库文档（含解析/导入状态，以百炼远端为准） */
    public DocumentPage listDocuments(String indexId, int pageNumber, int pageSize,
                                      String documentName, String documentStatus) {
        Map<String, String> query = new HashMap<>();
        query.put("IndexId", indexId);
        query.put("PageNumber", String.valueOf(pageNumber));
        query.put("PageSize", String.valueOf(pageSize));
        if (documentName != null && !documentName.isBlank()) {
            query.put("DocumentName", documentName);
        }
        if (documentStatus != null && !documentStatus.isBlank()) {
            query.put("DocumentStatus", documentStatus);
        }
        JSONObject resp = callGet("ListIndexDocuments", "/index/list_index_documents", query);
        JSONObject data = resp.getJSONObject("Data");
        List<DocItem> docs = new ArrayList<>();
        JSONArray arr = data.getJSONArray("Documents");
        if (arr != null) {
            for (Object o : arr) {
                JSONObject d = (JSONObject) o;
                docs.add(new DocItem(d.getStr("Id"), d.getStr("Name"), d.getStr("Status"),
                        d.getInt("Size"), d.getLong("GmtModified"),
                        d.getStr("DocumentType"), d.getStr("SourceId")));
            }
        }
        return new DocumentPage(data.getLong("TotalCount"), docs);
    }

    /** 删除知识库中的文档（远端永久删除） */
    public void deleteDocuments(String indexId, List<String> documentIds) {
        Map<String, String> query = new HashMap<>();
        query.put("IndexId", indexId);
        query.put("DocumentIds", JSONUtil.toJsonStr(documentIds));
        callIndex("DeleteIndexDocument", "/index/delete_index_document", query);
    }

    /** 删除知识库（远端，谨慎调用） */
    public void deleteIndex(String indexId) {
        Map<String, String> query = new HashMap<>();
        query.put("IndexId", indexId);
        callIndex("DeleteIndex", "/index/delete", query);
    }

    /** 知识库检索（RAG 召回），返回命中片段 */
    public List<RetrieveNode> retrieve(String indexId, String queryText, int topK, boolean enableReranking) {
        return retrieve(indexId, queryText, topK, enableReranking, null);
    }

    /**
     * 知识库检索（按文档 ID 过滤，勾选文档精准召回）。
     *
     * <p>SearchFilters 须为 JSON 数组（子分组）形式，按切片元数据字段 doc_id 多值 IN 过滤，
     * 即 {@code [{"doc_id":["<documentId>"]}]}}（实测：对象形式 {"documentIds":[...]} 报
     * InvalidSearchFilters "JSON Array parsing error"）。
     *
     * @param documentIds 限定检索的文档 ID（null/空 = 不过滤）
     */
    public List<RetrieveNode> retrieve(String indexId, String queryText, int topK,
                                       boolean enableReranking, List<String> documentIds) {
        Map<String, String> query = new HashMap<>();
        query.put("IndexId", indexId);
        query.put("Query", queryText);
        query.put("DenseSimilarityTopK", String.valueOf(topK));
        query.put("SparseSimilarityTopK", String.valueOf(topK));
        query.put("EnableReranking", String.valueOf(enableReranking));
        query.put("RerankTopN", String.valueOf(topK));
        if (documentIds != null && !documentIds.isEmpty()) {
            JSONObject condition = new JSONObject();
            condition.set("doc_id", documentIds);
            JSONArray filters = new JSONArray();
            filters.add(condition);
            query.put("SearchFilters", filters.toString());
        }
        JSONObject resp = callIndex("Retrieve", "/index/retrieve", query);
        JSONObject data = resp.getJSONObject("Data");
        List<RetrieveNode> nodes = new ArrayList<>();
        JSONArray arr = data.getJSONArray("Nodes");
        if (arr != null) {
            for (Object o : arr) {
                JSONObject n = (JSONObject) o;
                nodes.add(new RetrieveNode(n.getStr("Text"), n.getDouble("Score"), n.get("Metadata")));
            }
        }
        return nodes;
    }

    // ==================== 数据连接（文件上传链路） ====================

    /**
     * 申请上传租约（当前服务端要求 Md5 必填，官方 SDK 无该字段，走 ROA form 直连）。
     * 返回 OSS 预签名 URL（有效期短，须尽快上传）。
     */
    public UploadLease applyUploadLease(String fileName, byte[] content) {
        return applyUploadLease(fileName, content, null);
    }

    /**
     * 申请上传租约（指定目标类目；租约类目必须与后续 AddFile 的 CategoryId 一致，
     * 否则百炼返回 "Category is mismatched"）。
     */
    public UploadLease applyUploadLease(String fileName, byte[] content, String categoryId) {
        Map<String, String> form = new HashMap<>();
        form.put("FileName", fileName);
        form.put("SizeInBytes", String.valueOf(content.length));
        form.put("Md5", md5Hex(content));
        form.put("CategoryType", "UNSTRUCTURED");
        String cat = categoryId == null || categoryId.isBlank() ? CATEGORY_DEFAULT : categoryId;
        String body = roaClient.postForm("ApplyFileUploadLease", API_VERSION,
                "/" + workspaceId + "/datacenter/category/" + cat, form);
        JSONObject resp = parseResp(body, "ApplyFileUploadLease");
        JSONObject data = resp.getJSONObject("Data");
        JSONObject param = data.getJSONObject("Param");
        return new UploadLease(data.getStr("FileUploadLeaseId"), param.getStr("Url"),
                param.getStr("Method"), param.get("Headers"));
    }

    /** 将文档导入应用数据并触发解析（解析完成前不可建库/导入索引），返回 FileId */
    public String addFile(String leaseId, String categoryId, String parser, List<String> tags) {
        try {
            com.aliyun.bailian20231229.models.AddFileRequest req =
                    new com.aliyun.bailian20231229.models.AddFileRequest()
                            .setLeaseId(leaseId)
                            .setCategoryId(categoryId == null || categoryId.isBlank() ? CATEGORY_DEFAULT : categoryId)
                            .setParser(parser == null || parser.isBlank() ? PARSER_DOCMIND : parser);
            if (tags != null && !tags.isEmpty()) {
                req.setTags(tags);
            }
            com.aliyun.bailian20231229.models.AddFileResponse resp = sdkClient.addFile(workspaceId, req);
            // 注意：AddFile 的 success 字段类型为 String（"true"/"false"），与其他接口的 Boolean 不同
            if (!"true".equalsIgnoreCase(resp.getBody().getSuccess())) {
                throw new BusinessException(ErrorCode.BUSINESS, "导入文件失败"
                        + (resp.getBody().getMessage() == null || resp.getBody().getMessage().isBlank()
                        ? "" : "：" + resp.getBody().getMessage()));
            }
            return resp.getBody().getData().getFileId();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw wrap(e, "导入文件");
        }
    }

    /** 查询文件解析状态（INIT/PARSING/PARSE_SUCCESS/PARSE_FAILED） */
    public FileStatusInfo describeFile(String fileId) {
        try {
            com.aliyun.bailian20231229.models.DescribeFileResponse resp = sdkClient.describeFile(workspaceId, fileId,
                    // 空请求体：DescribeFile 仅按 FileId 查询，无其他参数
                    new com.aliyun.bailian20231229.models.DescribeFileRequest());
            checkSdk(resp.getBody().getSuccess(), resp.getBody().getMessage(), "查询文件状态");
            com.aliyun.bailian20231229.models.DescribeFileResponseBody.DescribeFileResponseBodyData data =
                    resp.getBody().getData();
            return new FileStatusInfo(data.getFileId(), data.getFileName(), data.getStatus(),
                    data.getSizeInBytes(), data.getParser(), data.getCategoryId(),
                    data.getFileType(), data.getCreateTime(), data.getTags());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw wrap(e, "查询文件状态");
        }
    }

    // ==================== 类目管理（业务空间级，多级树） ====================

    @Data
    @AllArgsConstructor
    public static class CategoryItem {
        private String categoryId;
        private String categoryName;
        private String parentCategoryId;
        private Boolean isDefault;
    }

    /** 全量类目列表（ListCategory 循环翻页聚合；类目量级小，一次拉全） */
    public List<CategoryItem> listCategories() {
        List<CategoryItem> all = new ArrayList<>();
        String nextToken = null;
        try {
            // 翻页迭代上限 100 轮（每轮 100 条 ≈ 10000 条上限），防服务端 nextToken 异常导致死循环
            for (int round = 0; round < 100; round++) {
                com.aliyun.bailian20231229.models.ListCategoryRequest req =
                        new com.aliyun.bailian20231229.models.ListCategoryRequest()
                                .setCategoryType("UNSTRUCTURED")
                                .setMaxResults(100)
                                .setNextToken(nextToken);
                com.aliyun.bailian20231229.models.ListCategoryResponse resp =
                        sdkClient.listCategory(workspaceId, req);
                checkSdk(resp.getBody().getSuccess(), resp.getBody().getMessage(), "查询类目");
                var data = resp.getBody().getData();
                if (data != null && data.getCategoryList() != null) {
                    for (var c : data.getCategoryList()) {
                        all.add(new CategoryItem(c.getCategoryId(), c.getCategoryName(),
                                c.getParentCategoryId(), c.getIsDefault()));
                    }
                }
                if (data == null || !Boolean.TRUE.equals(data.getHasNext())
                        || data.getNextToken() == null || data.getNextToken().isBlank()) {
                    return all;
                }
                nextToken = data.getNextToken();
            }
            throw new BusinessException(ErrorCode.BUSINESS,
                    "查询类目失败：翻页超过 100 轮上限（疑似服务端 nextToken 异常）");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw wrap(e, "查询类目");
        }
    }

    /** 新增类目（多级：parentCategoryId 为空 = 顶级类目） */
    public String addCategory(String categoryName, String parentCategoryId) {
        try {
            com.aliyun.bailian20231229.models.AddCategoryRequest req =
                    new com.aliyun.bailian20231229.models.AddCategoryRequest()
                            .setCategoryName(categoryName)
                            .setCategoryType("UNSTRUCTURED")
                            .setParentCategoryId(parentCategoryId);
            com.aliyun.bailian20231229.models.AddCategoryResponse resp =
                    sdkClient.addCategory(workspaceId, req);
            checkSdk(resp.getBody().getSuccess(), resp.getBody().getMessage(), "新增类目");
            return resp.getBody().getData().getCategoryId();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw wrap(e, "新增类目");
        }
    }

    /** 删除类目（类目下有文件时百炼返回错误，透传） */
    public void deleteCategory(String categoryId) {
        try {
            com.aliyun.bailian20231229.models.DeleteCategoryRequest req =
                    new com.aliyun.bailian20231229.models.DeleteCategoryRequest();
            // 注意：SDK 方法签名为 deleteCategory(CategoryId, WorkspaceId)，但路径模板为
            // /{workspaceId}/datacenter/category/{categoryId}/——传参 (categoryId, workspaceId) 顺序正确，
            // 不能按直觉传 (workspaceId, categoryId)（实测反向传参 404）。
            com.aliyun.bailian20231229.models.DeleteCategoryResponse resp =
                    sdkClient.deleteCategory(categoryId, workspaceId, req);
            checkSdk(resp.getBody().getSuccess(), resp.getBody().getMessage(), "删除类目");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw wrap(e, "删除类目");
        }
    }

    /** 更新文件标签（tags ≤ 10，空列表 = 清空标签） */
    public void updateFileTags(String fileId, List<String> tags) {
        try {
            com.aliyun.bailian20231229.models.UpdateFileTagRequest req =
                    new com.aliyun.bailian20231229.models.UpdateFileTagRequest()
                            .setTags(tags == null ? List.of() : tags);
            com.aliyun.bailian20231229.models.UpdateFileTagResponse resp =
                    sdkClient.updateFileTag(workspaceId, fileId, req);
            checkSdk(resp.getBody().getSuccess(), resp.getBody().getMessage(), "更新文件标签");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw wrap(e, "更新文件标签");
        }
    }

    // ==================== 切片管理 ====================

    /**
     * 分页查询知识库文档的切片列表（切片管理用）。
     *
     * @param fileId 文档 ID（必须，切片按文档维度展示）
     * @return 切片列表（text/score）与总数
     */
    public ChunkPage listChunks(String indexId, String fileId, int pageNum, int pageSize) {
        try {
            com.aliyun.bailian20231229.models.ListChunksRequest req =
                    new com.aliyun.bailian20231229.models.ListChunksRequest()
                            .setIndexId(indexId)
                            .setFileId(fileId)
                            .setPageNum(pageNum)
                            .setPageSize(pageSize);
            com.aliyun.bailian20231229.models.ListChunksResponse resp =
                    sdkClient.listChunks(workspaceId, req);
            checkSdk(resp.getBody().getSuccess(), resp.getBody().getMessage(), "查询切片列表");
            com.aliyun.bailian20231229.models.ListChunksResponseBody.ListChunksResponseBodyData data =
                    resp.getBody().getData();
            List<ChunkItem> chunks = new ArrayList<>();
            if (data != null && data.getNodes() != null) {
                for (var n : data.getNodes()) {
                    chunks.add(new ChunkItem(n.getText(), n.getScore(), n.getMetadata()));
                }
            }
            return new ChunkPage(data == null || data.getTotal() == null ? 0 : data.getTotal(), chunks);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw wrap(e, "查询切片列表");
        }
    }

    // ==================== 索引配置更新 ====================

    /**
     * 更新知识库配置（名称/描述；ROA POST /index/update，Id+Name 至少一项）。
     * 用于本地知识仓库改名时与百炼远端索引保持同步。
     */
    public void updateIndex(String indexId, String name, String description) {
        updateIndex(indexId, name, description, null, null, null);
    }

    /** 更新知识库（检索参数；denseTopK/sparseTopK/rerankMinScore 为 null 时不更新） */
    public void updateIndex(String indexId, String name, String description,
                            Integer denseTopK, Integer sparseTopK, Double rerankMinScore) {
        Map<String, String> query = new HashMap<>();
        query.put("Id", indexId);
        if (name != null && !name.isBlank()) {
            query.put("Name", name);
        }
        if (description != null && !description.isBlank()) {
            query.put("Description", description);
        }
        if (denseTopK != null) {
            query.put("DenseSimilarityTopK", String.valueOf(denseTopK));
        }
        if (sparseTopK != null) {
            query.put("SparseSimilarityTopK", String.valueOf(sparseTopK));
        }
        if (rerankMinScore != null) {
            query.put("RerankMinScore", String.valueOf(rerankMinScore));
        }
        callIndex("UpdateIndex", "/index/update", query);
    }

    /**
     * 向预签名 URL 上传文件二进制（ApplyFileUploadLease 返回的 URL，短时有效，须尽快上传）。
     * 注意：预签名 URL 自带签名，本方法不涉及 AccessKey。
     */
    public static void uploadBinary(UploadLease lease, byte[] content) {
        try {
            java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(15))
                    .build();
            java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(lease.getUrl()))
                    .timeout(java.time.Duration.ofMinutes(10))
                    .header("Content-Type", "application/octet-stream");
            // 预签名 URL 要求的附加请求头（K-V 均为字符串）
            if (lease.getHeaders() instanceof java.util.Map<?, ?> headers) {
                for (java.util.Map.Entry<?, ?> e : headers.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        builder.header(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                    }
                }
            }
            java.net.http.HttpRequest request = builder
                    .method(lease.getMethod() == null ? "PUT" : lease.getMethod().toUpperCase(),
                            java.net.http.HttpRequest.BodyPublishers.ofByteArray(content))
                    .build();
            java.net.http.HttpResponse<String> resp = http.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new BusinessException(ErrorCode.BUSINESS, "文件上传失败（HTTP " + resp.statusCode() + "）："
                        + resp.body());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS, "文件上传异常: " + e.getMessage(), e);
        }
    }

    // ==================== 内部工具 ====================

    /** ROA 调用索引接口并校验返回（Status==200），返回 Data 所在层级响应体 */
    private JSONObject callIndex(String action, String pathSuffix, Map<String, String> query) {
        String body = roaClient.post(action, API_VERSION, "/" + workspaceId + pathSuffix, query);
        return parseResp(body, action);
    }

    /** ROA GET 调用索引接口并校验返回 */
    private JSONObject callGet(String action, String pathSuffix, Map<String, String> query) {
        String body = roaClient.get(action, API_VERSION, "/" + workspaceId + pathSuffix, query);
        return parseResp(body, action);
    }

    /** 解析 ROA 响应并校验 Status==200 */
    private JSONObject parseResp(String body, String action) {
        JSONObject resp;
        try {
            resp = JSONUtil.parseObj(body);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS, "百炼响应解析失败: " + body);
        }
        String status = resp.getStr("Status");
        if (!STATUS_OK.equals(status)) {
            throw new BusinessException(ErrorCode.BUSINESS, action + "失败：" + resp.getStr("Message"));
        }
        return resp;
    }

    /** 文件内容 MD5（小写 hex，百炼上传租约必填） */
    private static String md5Hex(byte[] content) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "MD5 计算失败: " + e.getMessage());
        }
    }

    private void checkSdk(Boolean success, String message, String action) {
        if (success == null || !success) {
            throw new BusinessException(ErrorCode.BUSINESS, action + "失败"
                    + (message == null || message.isBlank() ? "" : "：" + message));
        }
    }

    private BusinessException wrap(Exception e, String action) {
        return new BusinessException(ErrorCode.BUSINESS, action + "异常: " + e.getMessage(), e);
    }

    // ==================== 结果模型 ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateIndexResult {
        private String indexId;
        private String jobId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndexJobStatus {
        /** 任务状态：RUNNING / FINISH / COMPLETED / FAILED 等 */
        private String jobStatus;
        private String jobId;
        private List<DocStatus> documents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocStatus {
        private String docId;
        private String docName;
        private String status;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentPage {
        private Long total;
        private List<DocItem> documents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocItem {
        private String id;
        private String name;
        /** 文档状态（导入索引后：INSERT_ERROR/RUNNING/FINISH 等） */
        private String status;
        private Integer size;
        private Long gmtModified;
        private String documentType;
        private String sourceId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrieveNode {
        private String text;
        private Double score;
        private Object metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadLease {
        private String leaseId;
        /** OSS 预签名 URL（短时有效） */
        private String url;
        private String method;
        /** 上传时须携带的请求头（K-V 均为字符串） */
        private Object headers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileStatusInfo {
        private String fileId;
        private String fileName;
        /** INIT / PARSING / PARSE_SUCCESS / PARSE_FAILED */
        private String status;
        private Long sizeInBytes;
        private String parser;
        private String categoryId;
        private String fileType;
        private String createTime;
        private List<String> tags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkPage {
        private long total;
        private List<ChunkItem> chunks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkItem {
        /** 切片文本内容 */
        private String text;
        /** 相关度（0-1，未评分可能为 null） */
        private Double score;
        /** 切片元数据（doc_id 等，JSON 对象或 null） */
        private Object metadata;
    }
}
