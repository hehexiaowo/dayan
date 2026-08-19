# 知识仓库上传、编辑与管理能力完善 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 admin/channel 知识仓库补齐类目管理与选择、切分方式配置、检索增强配置、标签与解析器选择能力（规格：`docs/superpowers/specs/2026-08-19-knowledge-repo-management-design.md`）。

**架构：** 百炼 SDK 升级 2.0.8→2.14.3；类目/标签/文件接口走官方 SDK，索引接口（CreateIndex/UpdateIndex）继续走已实测的 ROA 直连并扩展参数；切分/检索配置存本地 `system_knowledge_repo.config_json`（JSON 字符串，Jackson/hutool 序列化），懒建库时建库应用、已建库仅检索参数（denseTopK/sparseTopK/rerankMinScore）可改；类目/标签实时代理百炼不落库。

**技术栈：** Java 21 / Spring Boot / MyBatis-Plus / hutool-json / 阿里云 bailian20231229 SDK / Vue3 + Element Plus（dayan-admin、dayan-channel）

---

## 文件结构

**后端（dayan-server）：**
- 修改 `dayan-server/pom.xml:49` — SDK 版本 2.0.8 → 2.14.3
- 创建 `db/migration/90_system_knowledge_repo_index_config.sql` — config_json 列
- 修改 `dayan-common/dayan-common-aliyun/src/main/java/com/dayan/common/aliyun/bailian/BailianKnowledgeClient.java` — 常量/类目/标签/addFile/createIndex/updateIndex/DescribeFile 扩展
- 创建 `dayan-modules/dayan-module-system/src/main/java/com/dayan/system/dto/SystemKnowledgeIndexConfig.java` — 索引配置模型 + 校验
- 修改 `dayan-modules/dayan-module-system/src/main/java/com/dayan/system/entity/SystemKnowledgeRepo.java` — + configJson
- 修改 `dayan-modules/dayan-module-system/src/main/java/com/dayan/system/dto/SystemKnowledgeRepoCreateDTO.java` / `SystemKnowledgeRepoUpdateDTO.java` — + indexConfig
- 修改 `dayan-modules/dayan-module-system/src/main/java/com/dayan/system/vo/SystemKnowledgeRepoVO.java` — + indexConfig
- 修改 `dayan-modules/dayan-module-system/src/main/java/com/dayan/system/vo/SystemKnowledgeDocVO.java` — + categoryId/tags/parser
- 创建 `dayan-modules/dayan-module-system/src/main/java/com/dayan/system/vo/SystemCategoryVO.java`、`SystemCategoryAddDTO.java`
- 创建 `dayan-modules/dayan-module-system/src/main/java/com/dayan/system/dto/SystemDocTagsDTO.java`
- 修改 `dayan-modules/dayan-module-system/src/main/java/com/dayan/system/service/SystemKnowledgeRepoService.java` 与 `impl/SystemKnowledgeRepoServiceImpl.java`
- 修改 `dayan-modules/dayan-module-system/src/main/java/com/dayan/system/controller/admin/SystemKnowledgeRepoAdminController.java`
- 修改 `dayan-modules/dayan-module-system/pom.xml` — + junit-jupiter test 依赖
- 创建 `dayan-modules/dayan-module-system/src/test/java/com/dayan/system/dto/SystemKnowledgeIndexConfigTest.java`
- 创建 `dayan-modules/dayan-module-system/src/test/java/com/dayan/system/service/impl/SystemKnowledgeRepoServiceImplTest.java`

**前端 admin（dayan-admin/src）：**
- 修改 `api/knowledge.ts`、`types/knowledge.ts`
- 创建 `components/KnowledgeCategoryDialog/index.vue`
- 修改 `views/system/knowledge/index.vue`（类目管理按钮 + 创建弹窗配置表单）
- 修改 `views/system/knowledge/detail/index.vue`（索引配置展示 + 检索参数编辑）
- 修改 `views/system/knowledge/detail/DocTab.vue`（上传设置对话框/标签列/详情/编辑标签）

**前端 channel（dayan-channel/src）：** 同步 `api/knowledge.ts`、`types/knowledge.ts`、`components/ChunkDialog` 目录同级新建 `components/KnowledgeCategoryDialog/index.vue`、`views/system/knowledge/index.vue`（admin 文件复制后适配差异，见任务 14）

---

### 任务 1：SDK 升级 2.0.8 → 2.14.3

**文件：** 修改 `dayan-server/pom.xml:49`

- [ ] **步骤 1：改版本号**

```xml
<bailian.version>2.14.3</bailian.version>
```

- [ ] **步骤 2：编译验证（关键：确认现有 addFile/describeFile/listChunks 调用在新 SDK 兼容）**

运行：`cd F:\code\dayan\dayan-server && mvn -q -pl dayan-common/dayan-common-aliyun -am compile`
预期：BUILD SUCCESS（若报错，说明 SDK 签名变化，先核对调用点再继续）

- [ ] **步骤 3：Commit**

```bash
git add dayan-server/pom.xml
git commit -m "chore(server): 百炼 SDK 2.0.8 → 2.14.3（类目/标签/切分配置能力前置）"
```

### 任务 2：迁移 90 — config_json 列

**文件：** 创建 `db/migration/90_system_knowledge_repo_index_config.sql`

- [ ] **步骤 1：写迁移文件**

```sql
SET NAMES utf8mb4;
-- =====================================================================
-- 90_system_knowledge_repo_index_config.sql  知识仓库索引配置外置
--
-- 切分方式/向量模型/重排/改写/召回参数存 config_json（JSON 字符串），
-- 懒建库建库时应用；已建库仅检索参数（denseTopK/sparseTopK/rerankMinScore）可改。
-- 存量仓库 config_json 为空 = 使用百炼默认（智能切分）。
-- =====================================================================
ALTER TABLE `system_knowledge_repo`
  ADD COLUMN `config_json` VARCHAR(2000) NULL
  COMMENT '索引配置 JSON（切分方式/向量模型/重排/改写/召回参数；懒建库建库时应用，已建库仅检索参数可改）'
  AFTER `description`;
```

- [ ] **步骤 2：Commit**

```bash
git add db/migration/90_system_knowledge_repo_index_config.sql
git commit -m "feat(db): 迁移 90 知识仓库索引配置 config_json 列"
```

### 任务 3：BailianKnowledgeClient 扩展

**文件：** 修改 `dayan-common/dayan-common-aliyun/src/main/java/com/dayan/common/aliyun/bailian/BailianKnowledgeClient.java`

- [ ] **步骤 1：新增解析器常量（L38 附近）**

```java
    /** 文件解析器：阿里云智能文档解析（默认，已验证兼容） */
    public static final String PARSER_DOCMIND = "DASHSCOPE_DOCMIND";
    /** 文件解析器：电子文档解析 */
    public static final String PARSER_DOCMIND_DIGITAL = "DOCMIND_DIGITAL";
    /** 文件解析器：大模型文档解析 */
    public static final String PARSER_DOCMIND_LLM_VERSION = "DOCMIND_LLM_VERSION";
    /** 文件解析器：自动选择 */
    public static final String PARSER_AUTO_SELECT = "AUTO_SELECT";
```

- [ ] **步骤 2：createIndex 增加配置重载（保留原 3 参方法委托）**

```java
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
```

- [ ] **步骤 3：updateIndex 增加检索参数（保留原 4 参方法委托）**

```java
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
```

- [ ] **步骤 4：addFile 扩展标签（签名改为 4 参，原调用点任务 6 同步改）**

```java
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
```

- [ ] **步骤 5：类目与标签方法（新增，位于"数据连接"区后）**

```java
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
            while (true) {
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
                    break;
                }
                nextToken = data.getNextToken();
            }
            return all;
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
```

注：AddCategoryResponseBody.Data 字段为 CategoryId（`getData().getCategoryId()`），DeleteCategoryResponseBody/UpdateFileTagResponseBody 与 ListCategory 同模式（Code/Message/Success/Status），实施时若 getter 名不同按 `javap` 或 IDE 自动完成核对调整。

- [ ] **步骤 6：FileStatusInfo 增加字段（DescribeFile 全量映射）**

```java
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
```

同步修改 `describeFile`（L265-278）映射：补 `data.getCategoryId()`、`data.getFileType()`、`data.getCreateTime()`、`data.getTags()`（SDK 2.14.3 DescribeFileResponseBodyData 含这些字段，见调研）。

- [ ] **步骤 7：编译验证**

运行：`cd F:\code\dayan\dayan-server && mvn -q -pl dayan-common/dayan-common-aliyun -am compile`
预期：BUILD SUCCESS

- [ ] **步骤 8：Commit**

```bash
git add dayan-server/dayan-common/dayan-common-aliyun
git commit -m "feat(server): BailianKnowledgeClient 扩展——类目管理/文件标签/上传选类目解析器/addFile 标签/createIndex 切分检索配置/DescribeFile 全量映射"
```

### 任务 4：SystemKnowledgeIndexConfig 模型 + 校验（TDD）

**文件：**
- 创建 `dayan-modules/dayan-module-system/src/main/java/com/dayan/system/dto/SystemKnowledgeIndexConfig.java`
- 修改 `dayan-modules/dayan-module-system/pom.xml`（+ junit-jupiter test）
- 创建 `dayan-modules/dayan-module-system/src/test/java/com/dayan/system/dto/SystemKnowledgeIndexConfigTest.java`

- [ ] **步骤 1：加测试依赖（对齐 common-core 的 junit-jupiter 用法，pom 的 dependencies 区）**

```xml
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **步骤 2：编写失败的测试**

```java
package com.dayan.system.dto;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemKnowledgeIndexConfigTest {

    @Test
    void jsonRoundTrip() {
        SystemKnowledgeIndexConfig cfg = new SystemKnowledgeIndexConfig();
        cfg.setChunkMode("regex");
        cfg.setSeparator("(?<=。)");
        cfg.setChunkSize(500);
        cfg.setOverlapSize(100);
        cfg.setEmbeddingModel("text-embedding-v3");
        cfg.setRerankModel("qwen3-rerank");
        cfg.setRerankMode("qa");
        cfg.setRerankMinScore(0.2);
        cfg.setEnableRewrite(true);
        cfg.setDenseTopK(8);
        cfg.setSparseTopK(8);
        SystemKnowledgeIndexConfig parsed = JSONUtil.toBean(JSONUtil.toJsonStr(cfg), SystemKnowledgeIndexConfig.class);
        assertEquals("regex", parsed.getChunkMode());
        assertEquals("(?<=。)", parsed.getSeparator());
        assertEquals(500, parsed.getChunkSize());
        assertEquals(100, parsed.getOverlapSize());
        assertEquals("text-embedding-v3", parsed.getEmbeddingModel());
        assertEquals("qwen3-rerank", parsed.getRerankModel());
        assertEquals("qa", parsed.getRerankMode());
        assertEquals(0.2, parsed.getRerankMinScore());
        assertEquals(Boolean.TRUE, parsed.getEnableRewrite());
        assertEquals(8, parsed.getDenseTopK());
        assertEquals(8, parsed.getSparseTopK());
    }

    @Test
    void validateRejectsInvalidRanges() {
        SystemKnowledgeIndexConfig cfg = new SystemKnowledgeIndexConfig();
        cfg.setChunkSize(6001);
        assertThrows(IllegalArgumentException.class, cfg::validate);
        cfg.setChunkSize(200);
        cfg.setOverlapSize(200);
        assertThrows(IllegalArgumentException.class, cfg::validate, "overlap >= chunk 应报错");
        cfg.setOverlapSize(100);
        cfg.setRerankMinScore(2.0);
        assertThrows(IllegalArgumentException.class, cfg::validate, "rerankMinScore 超界应报错");
        cfg.setRerankMinScore(0.5);
        cfg.setChunkMode("unknown");
        assertThrows(IllegalArgumentException.class, cfg::validate, "chunkMode 非法值应报错");
    }

    @Test
    void validateAcceptsNullAsDefaults() {
        SystemKnowledgeIndexConfig cfg = new SystemKnowledgeIndexConfig();
        assertDoesNotThrow(cfg::validate);
    }

    @Test
    void toQueryMapContainsOnlySetFields() {
        SystemKnowledgeIndexConfig cfg = new SystemKnowledgeIndexConfig();
        cfg.setChunkMode("regex");
        cfg.setChunkSize(300);
        var map = cfg.toQueryMap();
        assertEquals("regex", map.get("chunkMode"));
        assertEquals("300", map.get("ChunkSize"));
        assertFalse(map.containsKey("Separator"), "未设置字段不应进 map");
        assertFalse(map.containsKey("denseTopK"), "TopK 不进 CreateIndex 参数");
    }
}
```

- [ ] **步骤 3：运行确认失败**

运行：`cd F:\code\dayan\dayan-server && mvn -pl dayan-modules/dayan-module-system test -Dtest=SystemKnowledgeIndexConfigTest`
预期：编译失败（SystemKnowledgeIndexConfig 不存在）

- [ ] **步骤 4：实现模型**

```java
package com.dayan.system.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 知识仓库索引配置（存 system_knowledge_repo.config_json）。
 *
 * <p>切分/向量/重排/改写参数仅在创建（懒建库 initIndex）时应用；已建库仅
 * denseTopK/sparseTopK/rerankMinScore 可更新（UpdateIndex 支持面）。
 */
@Data
public class SystemKnowledgeIndexConfig {

    /** 切分方式：null=智能切分；"regex"=自定义（分隔符切分） */
    private String chunkMode;

    /** 正则分隔符（仅 chunkMode=regex 生效） */
    private String separator;

    /** 切块长度 1-6000（默认 500） */
    private Integer chunkSize;

    /** 重叠 0-1024（默认 100，必须 < chunkSize） */
    private Integer overlapSize;

    /** 向量模型：text-embedding-v3 / text-embedding-v4（空=服务端默认） */
    private String embeddingModel;

    /** 重排模型：qwen3-rerank / qwen3-rerank-hybrid（空=服务端默认） */
    private String rerankModel;

    /** 重排模式：qa / similar / custom（默认 qa） */
    private String rerankMode;

    /** 相似度阈值 0.01-1.00（默认 0.01） */
    private Double rerankMinScore;

    /** 多轮改写（默认 true） */
    private Boolean enableRewrite;

    /** 稠密召回数（仅已建库 UpdateIndex 用） */
    private Integer denseTopK;

    /** 稀疏召回数（仅已建库 UpdateIndex 用） */
    private Integer sparseTopK;

    /** 校验配置合法性；非法抛 IllegalArgumentException */
    public void validate() {
        if (chunkSize != null && (chunkSize < 1 || chunkSize > 6000)) {
            throw new IllegalArgumentException("切块长度需在 1-6000 之间");
        }
        if (overlapSize != null && (overlapSize < 0 || overlapSize > 1024)) {
            throw new IllegalArgumentException("重叠长度需在 0-1024 之间");
        }
        if (chunkSize != null && overlapSize != null && overlapSize >= chunkSize) {
            throw new IllegalArgumentException("重叠长度必须小于切块长度");
        }
        if (rerankMinScore != null && (rerankMinScore < 0.01 || rerankMinScore > 1.0)) {
            throw new IllegalArgumentException("相似度阈值需在 0.01-1.00 之间");
        }
        if (chunkMode != null && !"regex".equals(chunkMode)) {
            throw new IllegalArgumentException("切分方式仅支持智能切分（不传）或 regex（自定义）");
        }
        if ("regex".equals(chunkMode) && (separator == null || separator.isBlank())) {
            throw new IllegalArgumentException("自定义切分必须填写分隔符");
        }
    }

    /** 转 CreateIndex 扩展参数（仅已设置且建库相关的字段；TopK 不在此列） */
    public Map<String, String> toQueryMap() {
        Map<String, String> map = new HashMap<>();
        if (chunkMode != null) map.put("chunkMode", chunkMode);
        if (separator != null && !separator.isBlank()) map.put("Separator", separator);
        if (chunkSize != null) map.put("ChunkSize", String.valueOf(chunkSize));
        if (overlapSize != null) map.put("OverlapSize", String.valueOf(overlapSize));
        if (embeddingModel != null && !embeddingModel.isBlank()) map.put("EmbeddingModelName", embeddingModel);
        if (rerankModel != null && !rerankModel.isBlank()) map.put("RerankModelName", rerankModel);
        if (rerankMode != null && !rerankMode.isBlank()) map.put("RerankMode", rerankMode);
        if (rerankMinScore != null) map.put("RerankMinScore", String.valueOf(rerankMinScore));
        if (enableRewrite != null) map.put("EnableRewrite", String.valueOf(enableRewrite));
        return map;
    }
}
```

- [ ] **步骤 5：运行测试确认通过**

运行：`cd F:\code\dayan\dayan-server && mvn -pl dayan-modules/dayan-module-system test -Dtest=SystemKnowledgeIndexConfigTest`
预期：Tests run: 4, Failures: 0（hutool-json 若不在 module-system 传递依赖，在 pom 加 `cn.hutool:hutool-json` test 或 main 依赖；JSONUtil 已是 common-aliyun 使用同款，版本由父 pom 管理）

- [ ] **步骤 6：Commit**

```bash
git add dayan-server/dayan-modules/dayan-module-system
git commit -m "feat(server): SystemKnowledgeIndexConfig 索引配置模型与校验（切分/向量/重排/改写/召回参数）"
```

### 任务 5：Entity / DTO / VO 扩展

**文件：**
- 修改 `entity/SystemKnowledgeRepo.java`
- 修改 `dto/SystemKnowledgeRepoCreateDTO.java`、`dto/SystemKnowledgeRepoUpdateDTO.java`
- 修改 `vo/SystemKnowledgeRepoVO.java`、`vo/SystemKnowledgeDocVO.java`
- 创建 `vo/SystemCategoryVO.java`、`vo/SystemCategoryAddDTO.java`（SystemDocTagsDTO 在 dto 包，见任务 5 步骤 4）

- [ ] **步骤 1：Entity 加字段（description 字段后）**

```java
    /** 索引配置 JSON（切分/向量/重排/改写/召回参数；空=百炼默认） */
    private String configJson;
```

- [ ] **步骤 2：DTO 加 indexConfig**

```java
// SystemKnowledgeRepoCreateDTO 追加：
    /** 索引配置（切分方式/检索参数；mode=bind 时忽略） */
    private SystemKnowledgeIndexConfig indexConfig;
```

```java
// SystemKnowledgeRepoUpdateDTO 追加：
    /** 索引配置（整体替换；已建库仅检索参数可改，切分等不可变字段变更会被拒绝） */
    private SystemKnowledgeIndexConfig indexConfig;
```

- [ ] **步骤 3：VO 加字段**

```java
// SystemKnowledgeRepoVO 追加：
    /** 索引配置（configJson 解析；未配置为 null） */
    private SystemKnowledgeIndexConfig indexConfig;
```

```java
// SystemKnowledgeDocVO 追加：
    /** 所属类目 ID（DescribeFile 返回） */
    private String categoryId;

    /** 文件标签（DescribeFile 返回，≤10） */
    private List<String> tags;

    /** 解析器（DASHSCOPE_DOCMIND/DOCMIND_DIGITAL/DOCMIND_LLM_VERSION/AUTO_SELECT） */
    private String parser;
```

（SystemKnowledgeDocVO 需加 `import java.util.List;`）

- [ ] **步骤 4：新建三个小类型**

```java
package com.dayan.system.vo;

import lombok.Data;

/** 百炼类目（业务空间级，多级树） */
@Data
public class SystemCategoryVO {
    private String categoryId;
    private String categoryName;
    private String parentCategoryId;
    /** 百炼内置默认类目（不可删除） */
    private Boolean isDefault;
}
```

```java
package com.dayan.system.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增类目 DTO */
@Data
public class SystemCategoryAddDTO {

    @NotBlank(message = "类目名称不能为空")
    @Size(max = 100, message = "类目名称最长 100 字")
    private String categoryName;

    /** 父类目 ID（空=顶级类目） */
    private String parentCategoryId;
}
```

```java
package com.dayan.system.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 文件标签更新 DTO（≤10 个，空列表=清空） */
@Data
public class SystemDocTagsDTO {

    @Size(max = 10, message = "标签最多 10 个")
    private List<String> tags;
}
```

- [ ] **步骤 5：编译验证**

运行：`cd F:\code\dayan\dayan-server && mvn -q -pl dayan-modules/dayan-module-system -am compile`
预期：BUILD SUCCESS

- [ ] **步骤 6：Commit**

```bash
git add dayan-server/dayan-modules/dayan-module-system
git commit -m "feat(server): 知识仓库 Entity/DTO/VO 扩展——configJson/indexConfig/类目 VO/标签 DTO"
```

### 任务 6：Service 层扩展

**文件：** 修改 `service/SystemKnowledgeRepoService.java`、`service/impl/SystemKnowledgeRepoServiceImpl.java`
创建 `dayan-modules/dayan-module-system/src/test/java/com/dayan/system/service/impl/SystemKnowledgeRepoServiceImplTest.java`

- [ ] **步骤 1：Service 接口新增方法**

```java
    /** 类目列表（业务空间级全量平铺） */
    List<SystemCategoryVO> listCategories();

    /** 新增类目，返回 CategoryId */
    String addCategory(String categoryName, String parentCategoryId);

    /** 删除类目（类目下有文件时百炼拒绝） */
    void deleteCategory(String categoryId);

    /** 更新文件标签（≤10，空=清空） */
    void updateDocTags(Long id, String fileId, SystemDocTagsDTO dto);

    /** 上传文档（可选类目/解析器/标签；categoryId 空=default，parser 空=智能解析） */
    String uploadDocument(Long id, MultipartFile file, String categoryId, String parser, List<String> tags);
```

- [ ] **步骤 2：写失败测试（update 校验逻辑抽静态包可见方法 assertUpdatableConfig）**

```java
package com.dayan.system.service.impl;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.system.dto.SystemKnowledgeIndexConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemKnowledgeRepoServiceImplTest {

    private static SystemKnowledgeIndexConfig base() {
        SystemKnowledgeIndexConfig cfg = new SystemKnowledgeIndexConfig();
        cfg.setChunkMode(null);
        cfg.setChunkSize(500);
        cfg.setOverlapSize(100);
        cfg.setDenseTopK(4);
        cfg.setSparseTopK(4);
        cfg.setRerankMinScore(0.01);
        return cfg;
    }

    @Test
    void updatableFieldsPass() {
        SystemKnowledgeIndexConfig incoming = base();
        incoming.setDenseTopK(8);
        incoming.setSparseTopK(8);
        incoming.setRerankMinScore(0.3);
        assertDoesNotThrow(() -> SystemKnowledgeRepoServiceImpl.assertUpdatableConfig(base(), incoming));
    }

    @Test
    void chunkChangeRejected() {
        SystemKnowledgeIndexConfig incoming = base();
        incoming.setChunkMode("regex");
        incoming.setSeparator("(?<=。)");
        BusinessException e = assertThrows(BusinessException.class,
                () -> SystemKnowledgeRepoServiceImpl.assertUpdatableConfig(base(), incoming));
        assertTrue(e.getMessage().contains("建库后不可修改"));
    }

    @Test
    void embeddingModelChangeRejected() {
        SystemKnowledgeIndexConfig incoming = base();
        incoming.setEmbeddingModel("text-embedding-v4");
        assertThrows(BusinessException.class,
                () -> SystemKnowledgeRepoServiceImpl.assertUpdatableConfig(base(), incoming));
    }

    @Test
    void nullIncomingPasses() {
        assertDoesNotThrow(() -> SystemKnowledgeRepoServiceImpl.assertUpdatableConfig(base(), null));
        assertDoesNotThrow(() -> SystemKnowledgeRepoServiceImpl.assertUpdatableConfig(null, base()));
    }
}
```

- [ ] **步骤 3：运行确认失败**

运行：`cd F:\code\dayan\dayan-server && mvn -pl dayan-modules/dayan-module-system test -Dtest=SystemKnowledgeRepoServiceImplTest`
预期：编译失败（assertUpdatableConfig 不存在）

- [ ] **步骤 4：实现 ServiceImpl 改造（逐段替换）**

导入追加：
```java
import cn.hutool.json.JSONUtil;
import com.dayan.system.vo.SystemCategoryAddDTO;
import com.dayan.system.vo.SystemCategoryVO;
import com.dayan.system.dto.SystemDocTagsDTO;
```

新增工具方法（"内部工具"区）：
```java
    /** 已建库配置可更新校验：仅 denseTopK/sparseTopK/rerankMinScore 可变，其余报错 */
    static void assertUpdatableConfig(SystemKnowledgeIndexConfig existing, SystemKnowledgeIndexConfig incoming) {
        if (existing == null || incoming == null) {
            return;
        }
        if (!Objects.equals(existing.getChunkMode(), incoming.getChunkMode())
                || !Objects.equals(existing.getSeparator(), incoming.getSeparator())
                || !Objects.equals(existing.getChunkSize(), incoming.getChunkSize())
                || !Objects.equals(existing.getOverlapSize(), incoming.getOverlapSize())
                || !Objects.equals(existing.getEmbeddingModel(), incoming.getEmbeddingModel())
                || !Objects.equals(existing.getRerankModel(), incoming.getRerankModel())
                || !Objects.equals(existing.getRerankMode(), incoming.getRerankMode())
                || !Objects.equals(existing.getEnableRewrite(), incoming.getEnableRewrite())) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "切分方式、向量模型、重排模型等配置在建库后不可修改（如需调整请删除仓库重建）");
        }
    }

    private SystemKnowledgeIndexConfig parseConfig(SystemKnowledgeRepo repo) {
        if (StrUtil.isBlank(repo.getConfigJson())) {
            return null;
        }
        try {
            return JSONUtil.toBean(repo.getConfigJson(), SystemKnowledgeIndexConfig.class);
        } catch (Exception e) {
            log.warn("索引配置 JSON 解析失败 repoId={}: {}", repo.getId(), e.getMessage());
            return null;
        }
    }
```

`create()`（L282 之后 indexId 赋值处，bind 分支后加）：
```java
        if (!bind && dto.getIndexConfig() != null) {
            dto.getIndexConfig().validate();
            repo.setConfigJson(JSONUtil.toJsonStr(dto.getIndexConfig()));
        }
```

`initIndex()`（L313-328）：
```java
        SystemKnowledgeIndexConfig config = parseConfig(repo);
        BailianKnowledgeClient.CreateIndexResult result =
                requireClient().createIndex(repo.getRepoName(), repo.getDescription(), fileIds,
                        config == null ? null : config.toQueryMap());
```

`update()`（L332-353，dto.getIndexConfig() != null 分支插在 sortOrder 之前）：
```java
        if (dto.getIndexConfig() != null) {
            SystemKnowledgeIndexConfig config = dto.getIndexConfig();
            config.validate();
            if (StrUtil.isBlank(repo.getIndexId())) {
                // 未建库（懒建库）：全量保存，initIndex 时应用
                repo.setConfigJson(JSONUtil.toJsonStr(config));
            } else {
                // 已建库：仅检索参数可改并同步百炼
                SystemKnowledgeIndexConfig existing = parseConfig(repo);
                assertUpdatableConfig(existing, config);
                boolean syncNeeded = (config.getDenseTopK() != null && !config.getDenseTopK().equals(existing == null ? null : existing.getDenseTopK()))
                        || (config.getSparseTopK() != null && !config.getSparseTopK().equals(existing == null ? null : existing.getSparseTopK()))
                        || (config.getRerankMinScore() != null && !config.getRerankMinScore().equals(existing == null ? null : existing.getRerankMinScore()));
                if (syncNeeded) {
                    requireClient().updateIndex(repo.getIndexId(), repo.getRepoName(), repo.getDescription(),
                            config.getDenseTopK(), config.getSparseTopK(), config.getRerankMinScore());
                }
                // 合并：保留未提交的不可变字段，覆盖可更新字段
                SystemKnowledgeIndexConfig merged = existing == null ? new SystemKnowledgeIndexConfig() : existing;
                if (config.getDenseTopK() != null) merged.setDenseTopK(config.getDenseTopK());
                if (config.getSparseTopK() != null) merged.setSparseTopK(config.getSparseTopK());
                if (config.getRerankMinScore() != null) merged.setRerankMinScore(config.getRerankMinScore());
                repo.setConfigJson(JSONUtil.toJsonStr(merged));
            }
        }
```

`uploadDocument()`（L434-458，签名与调用）：
```java
    public String uploadDocument(Long id, MultipartFile file, String categoryId, String parser, List<String> tags) {
        requireRepo(id);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名必须带扩展名（如 .pdf/.docx/.md）");
        }
        if (tags != null && tags.size() > 10) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标签最多 10 个");
        }
        try {
            BailianKnowledgeClient client = requireClient();
            byte[] content = file.getBytes();
            BailianKnowledgeClient.UploadLease lease = client.applyUploadLease(fileName, content);
            BailianKnowledgeClient.uploadBinary(lease, content);
            String fileId = client.addFile(lease.getLeaseId(), categoryId, parser, tags);
            log.info("知识库文档上传成功 repoId={} fileName={} categoryId={} fileId={}", id, fileName,
                    categoryId == null ? "default" : categoryId, fileId);
            return fileId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS, "文件读取失败: " + e.getMessage(), e);
        }
    }
```

`getDocumentParseStatus()`（L461-470）VO 增强：
```java
        vo.setCategoryId(info.getCategoryId());
        vo.setTags(info.getTags());
        vo.setParser(info.getParser());
```

`toVO()`（L628）加：
```java
        vo.setIndexConfig(parseConfig(repo));
```

新方法（"文档管理"区后）：
```java
    // ==================== 类目与文件标签管理（实时代理百炼） ====================

    @Override
    public List<SystemCategoryVO> listCategories() {
        return requireClient().listCategories().stream().map(c -> {
            SystemCategoryVO vo = new SystemCategoryVO();
            vo.setCategoryId(c.getCategoryId());
            vo.setCategoryName(c.getCategoryName());
            vo.setParentCategoryId(c.getParentCategoryId());
            vo.setIsDefault(c.getIsDefault());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public String addCategory(String categoryName, String parentCategoryId) {
        return requireClient().addCategory(categoryName, parentCategoryId);
    }

    @Override
    public void deleteCategory(String categoryId) {
        requireClient().deleteCategory(categoryId);
    }

    @Override
    public void updateDocTags(Long id, String fileId, SystemDocTagsDTO dto) {
        requireRepo(id);
        if (dto == null || dto.getTags() == null || dto.getTags().size() > 10) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标签最多 10 个");
        }
        requireClient().updateFileTags(fileId, dto.getTags());
    }
```

- [ ] **步骤 5：运行测试确认通过**

运行：`cd F:\code\dayan\dayan-server && mvn -pl dayan-modules/dayan-module-system test -Dtest='SystemKnowledgeIndexConfigTest,SystemKnowledgeRepoServiceImplTest'`
预期：Tests run: 8, Failures: 0

- [ ] **步骤 6：Commit**

```bash
git add dayan-server/dayan-modules/dayan-module-system
git commit -m "feat(server): 知识仓库 Service 扩展——类目 CRUD/标签更新/上传选类目解析器标签/建库应用索引配置/已建库仅检索参数可改"
```

### 任务 7：Controller 扩展

**文件：** 修改 `controller/admin/SystemKnowledgeRepoAdminController.java`

- [ ] **步骤 1：新增类目与标签接口（"文档管理"区后）**

```java
    // ---------- 类目管理（业务空间级，多级树） ----------

    @Operation(summary = "类目列表（全量平铺，前端组树）")
    @SaCheckPermission("system:knowledge:repo:list")
    @GetMapping("/categories")
    public R<List<SystemCategoryVO>> listCategories() {
        return R.ok(knowledgeRepoService.listCategories());
    }

    @Operation(summary = "新增类目（parentCategoryId 空=顶级）")
    @SaCheckPermission("system:knowledge:repo:create")
    @PostMapping("/categories")
    public R<String> addCategory(@RequestBody @Valid SystemCategoryAddDTO dto) {
        return R.ok(knowledgeRepoService.addCategory(dto.getCategoryName(), dto.getParentCategoryId()));
    }

    @Operation(summary = "删除类目（类目下有文件时百炼拒绝）")
    @SaCheckPermission("system:knowledge:repo:delete")
    @DeleteMapping("/categories/{categoryId}")
    public R<Void> deleteCategory(@PathVariable String categoryId) {
        knowledgeRepoService.deleteCategory(categoryId);
        return R.ok();
    }

    // ---------- 文件标签 ----------

    @Operation(summary = "更新文件标签（≤10，空=清空）")
    @SaCheckPermission("system:knowledge:doc:upload")
    @PutMapping("/{id}/documents/{fileId}/tags")
    public R<Void> updateDocTags(@PathVariable Long id, @PathVariable String fileId,
                                 @RequestBody @Valid SystemDocTagsDTO dto) {
        knowledgeRepoService.updateDocTags(id, fileId, dto);
        return R.ok();
    }
```

- [ ] **步骤 2：上传接口加可选参数（L119-124 替换）**

```java
    @Operation(summary = "上传文档（可指定类目/解析器/标签，返回 FileId，解析异步）")
    @SaCheckPermission("system:knowledge:doc:upload")
    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<String> uploadDocument(@PathVariable Long id,
                                    @RequestPart("file") MultipartFile file,
                                    @RequestParam(required = false) String categoryId,
                                    @RequestParam(required = false) String parser,
                                    @RequestParam(required = false) List<String> tags) {
        return R.ok(knowledgeRepoService.uploadDocument(id, file, categoryId, parser, tags));
    }
```

- [ ] **步骤 3：编译验证**

运行：`cd F:\code\dayan\dayan-server && mvn -q -pl dayan-modules/dayan-module-system -am compile`
预期：BUILD SUCCESS

- [ ] **步骤 4：Commit**

```bash
git add dayan-server/dayan-modules/dayan-module-system
git commit -m "feat(server): 知识仓库 Controller 扩展——类目 CRUD/文件标签/上传类目解析器标签参数"
```

### 任务 8：后端全量构建 + 测试

- [ ] **步骤 1：全量测试**

运行：`cd F:\code\dayan\dayan-server && mvn -pl dayan-modules/dayan-module-system test`
预期：BUILD SUCCESS，全部测试通过

- [ ] **步骤 2：starter 编译（admin/channel 一起验证 SDK 升级无回归）**

运行：`cd F:\code\dayan\dayan-server && mvn -q -pl dayan-starters/dayan-admin,dayan-starters/dayan-channel -am compile`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit（如有遗留改动）**

### 任务 9：前端 api/types 扩展（admin）

**文件：** 修改 `dayan-admin/src/api/knowledge.ts`、`dayan-admin/src/types/knowledge.ts`

- [ ] **步骤 1：types 增加类型（KnowledgeRepo 后）**

```ts
/** 索引配置（对齐 SystemKnowledgeIndexConfig） */
export interface KnowledgeIndexConfig {
  /** null=智能切分；"regex"=自定义（分隔符切分） */
  chunkMode?: string
  separator?: string
  /** 切块长度 1-6000 */
  chunkSize?: number
  /** 重叠 0-1024（< chunkSize） */
  overlapSize?: number
  /** text-embedding-v3 / text-embedding-v4 */
  embeddingModel?: string
  /** qwen3-rerank / qwen3-rerank-hybrid */
  rerankModel?: string
  /** qa / similar / custom */
  rerankMode?: string
  /** 0.01-1.00 */
  rerankMinScore?: number
  enableRewrite?: boolean
  denseTopK?: number
  sparseTopK?: number
}

/** 百炼类目（业务空间级，多级树） */
export interface KnowledgeCategory {
  categoryId: string
  categoryName: string
  parentCategoryId?: string
  isDefault?: boolean
}

/** 解析器选项 */
export const KNOWLEDGE_PARSER_OPTIONS = [
  { value: 'DASHSCOPE_DOCMIND', label: '智能文档解析' },
  { value: 'DOCMIND_DIGITAL', label: '电子文档解析' },
  { value: 'DOCMIND_LLM_VERSION', label: '大模型文档解析' },
  { value: 'AUTO_SELECT', label: '自动选择' }
] as const
```

`KnowledgeRepo` 加 `indexConfig?: KnowledgeIndexConfig`；`KnowledgeRepoCreatePayload` 加 `indexConfig?: KnowledgeIndexConfig`；`KnowledgeDoc` 加 `categoryId?: string`、`tags?: string[]`、`parser?: string`。

- [ ] **步骤 2：api 增加函数（uploadKnowledgeDoc 改造 + 类目/标签）**

```ts
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
```

- [ ] **步骤 3：类型检查**

运行：`cd F:\code\dayan\dayan-admin && node node_modules/vue-tsc/bin/vue-tsc.js --noEmit`
预期：DocTab.vue 现有调用 `uploadKnowledgeDoc(props.repoId, options.file, true)` 会报类型错误（`true` 不再匹配 options 参数）——这是预期内的过渡态，任务 12 整体改造 DocTab 后消除；其余文件应无错误

- [ ] **步骤 4：Commit**

```bash
git add dayan-admin/src/api/knowledge.ts dayan-admin/src/types/knowledge.ts
git commit -m "feat(admin): 知识仓库 api/types 扩展——类目/标签/索引配置/上传参数"
```

### 任务 10：类目管理弹窗组件 + 列表页入口

**文件：**
- 创建 `dayan-admin/src/components/KnowledgeCategoryDialog/index.vue`
- 修改 `dayan-admin/src/views/system/knowledge/index.vue`

- [ ] **步骤 1：创建组件（类目树 + 新增子类目 + 删除）**

```vue
<script setup lang="ts">
/**
 * 类目管理弹窗（百炼业务空间级类目树）。
 * 支持多级：节点「新增子类目」；默认类目（isDefault）只读不可删。
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listKnowledgeCategories,
  addKnowledgeCategory,
  deleteKnowledgeCategory
} from '@/api/knowledge'
import type { KnowledgeCategory } from '@/types/knowledge'

const visible = defineModel<boolean>({ default: false })
const loading = ref(false)
const categories = ref<KnowledgeCategory[]>([])

interface TreeNode extends KnowledgeCategory {
  children: TreeNode[]
}

/** 平铺 → 树（parentCategoryId 挂接；顶层含百炼 default 类目） */
function buildTree(flat: KnowledgeCategory[]): TreeNode[] {
  const map = new Map<string, TreeNode>()
  flat.forEach((c) => map.set(c.categoryId, { ...c, children: [] }))
  const roots: TreeNode[] = []
  map.forEach((node) => {
    if (node.parentCategoryId && map.has(node.parentCategoryId)) {
      map.get(node.parentCategoryId)!.children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

async function load() {
  loading.value = true
  try {
    categories.value = await listKnowledgeCategories()
  } catch {
    categories.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

/** 新增子类目 */
async function handleAdd(parent?: TreeNode) {
  const { value } = await ElMessageBox.prompt(
    parent ? `在「${parent.categoryName}」下新增子类目名称：` : '新增顶级类目名称：',
    '新增类目',
    { confirmButtonText: '确定', cancelButtonText: '取消', inputPattern: /\S+/, inputErrorMessage: '类目名称不能为空' }
  )
  if (!value) return
  await addKnowledgeCategory({ categoryName: value.trim(), parentCategoryId: parent?.categoryId })
  ElMessage.success('类目创建成功')
  load()
}

async function handleDelete(node: TreeNode) {
  if (node.isDefault) return
  await ElMessageBox.confirm(
    `确定删除类目「${node.categoryName}」？若类目下有文件，百炼将拒绝删除。`,
    '删除类目',
    { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
  )
  await deleteKnowledgeCategory(node.categoryId)
  ElMessage.success('删除成功')
  load()
}
</script>

<template>
  <el-dialog v-model="visible" title="类目管理" width="560px">
    <div class="category-toolbar">
      <el-button type="primary" size="small" @click="handleAdd()">新增顶级类目</el-button>
      <span class="tip">类目为业务空间级（所有知识库共享），用于上传文件时归类</span>
    </div>
    <el-tree
      v-loading="loading"
      :data="buildTree(categories)"
      node-key="categoryId"
      default-expand-all
      :expand-on-click-node="false"
      class="category-tree"
    >
      <template #default="{ data }">
        <div class="tree-node">
          <span class="node-name">
            {{ data.categoryName }}
            <el-tag v-if="data.isDefault" size="small" type="info">默认</el-tag>
          </span>
          <span class="node-actions">
            <el-button link type="primary" size="small" @click.stop="handleAdd(data)">新增子类目</el-button>
            <el-button
              v-if="!data.isDefault"
              link
              type="danger"
              size="small"
              @click.stop="handleDelete(data)"
            >删除</el-button>
          </span>
        </div>
      </template>
    </el-tree>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.category-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  .tip {
    font-size: 12px;
    color: #909399;
  }
}
.category-tree {
  max-height: 420px;
  overflow: auto;
  .tree-node {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-right: 8px;
    .node-name {
      font-size: 13px;
    }
  }
}
</style>
```

- [ ] **步骤 2：列表页接入（index.vue script 区 + 卡片头部按钮）**

script 区追加：
```ts
import KnowledgeCategoryDialog from '@/components/KnowledgeCategoryDialog/index.vue'

const categoryDialogVisible = ref(false)
```

模板卡片头部（`新建知识仓库` 按钮前）加：
```html
          <el-button :icon="'FolderOpened'" v-permission="'system:knowledge:repo:list'" @click="categoryDialogVisible = true">
            类目管理
          </el-button>
```

弹窗层（`新建仓库弹窗` 前）加：
```html
    <KnowledgeCategoryDialog v-model="categoryDialogVisible" />
```

- [ ] **步骤 3：类型检查**

运行：`cd F:\code\dayan\dayan-admin && node node_modules/vue-tsc/bin/vue-tsc.js --noEmit`
预期：无输出

- [ ] **步骤 4：Commit**

```bash
git add dayan-admin/src/components/KnowledgeCategoryDialog dayan-admin/src/views/system/knowledge/index.vue
git commit -m "feat(admin): 类目管理弹窗（多级树/新增子类目/删除）与列表页入口"
```

### 任务 11：创建仓库弹窗——切分与检索配置表单

**文件：** 修改 `dayan-admin/src/views/system/knowledge/index.vue`

- [ ] **步骤 1：form 增加 indexConfig 与类型引用**

```ts
import type { KnowledgeIndexConfig } from '@/types/knowledge'

const emptyIndexConfig = (): KnowledgeIndexConfig => ({
  chunkMode: undefined,
  separator: '',
  chunkSize: 500,
  overlapSize: 100,
  embeddingModel: 'text-embedding-v3',
  rerankModel: 'qwen3-rerank',
  rerankMode: 'qa',
  rerankMinScore: 0.01,
  enableRewrite: true,
  denseTopK: 4,
  sparseTopK: 4
})
```

`resetForm()` 里加 `indexConfig: emptyIndexConfig()`；`handleSubmit()` 提交参数加：
```ts
      indexConfig: form.mode === 'create' ? { ...form.indexConfig } : undefined,
```

- [ ] **步骤 2：模板加配置表单（创建方式区后，`v-if="form.mode === 'create'"`）**

```html
        <el-form-item v-if="form.mode === 'create'" label="切分方式">
          <el-radio-group v-model="form.indexConfig.chunkMode">
            <el-radio :value="undefined">智能切分</el-radio>
            <el-radio value="regex">自定义切分</el-radio>
          </el-radio-group>
          <div class="form-tip">智能切分按语义自动切块；自定义按分隔符 + 长度 + 重叠切块（建库后不可修改）</div>
        </el-form-item>
        <template v-if="form.mode === 'create' && form.indexConfig.chunkMode === 'regex'">
          <el-form-item label="分隔符">
            <el-input v-model="form.indexConfig.separator" placeholder="正则表达式，如 (?<=。)" />
          </el-form-item>
          <el-form-item label="切块长度">
            <el-input-number v-model="form.indexConfig.chunkSize" :min="1" :max="6000" controls-position="right" />
          </el-form-item>
          <el-form-item label="重叠长度">
            <el-input-number v-model="form.indexConfig.overlapSize" :min="0" :max="1024" controls-position="right" />
          </el-form-item>
        </template>
        <el-form-item v-if="form.mode === 'create'" label="向量模型">
          <el-select v-model="form.indexConfig.embeddingModel" style="width: 220px">
            <el-option label="text-embedding-v3" value="text-embedding-v3" />
            <el-option label="text-embedding-v4" value="text-embedding-v4" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.mode === 'create'" label="重排模型">
          <el-select v-model="form.indexConfig.rerankModel" style="width: 220px">
            <el-option label="qwen3-rerank（语义重排）" value="qwen3-rerank" />
            <el-option label="qwen3-rerank-hybrid（语义+文本匹配）" value="qwen3-rerank-hybrid" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.mode === 'create'" label="重排模式">
          <el-select v-model="form.indexConfig.rerankMode" style="width: 220px">
            <el-option label="问答模式（qa）" value="qa" />
            <el-option label="相似模式（similar）" value="similar" />
            <el-option label="自定义（custom）" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.mode === 'create'" label="相似度阈值">
          <el-input-number v-model="form.indexConfig.rerankMinScore" :min="0.01" :max="1" :step="0.01" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="form.mode === 'create'" label="多轮改写">
          <el-switch v-model="form.indexConfig.enableRewrite" />
          <span class="form-tip" style="margin-left: 8px">多轮对话时对问题做改写后检索</span>
        </el-form-item>
```

（`form` 改为含 `indexConfig: emptyIndexConfig()`；弹窗宽度 620→720px 以容纳配置区）

- [ ] **步骤 3：类型检查**

运行：`cd F:\code\dayan\dayan-admin && node node_modules/vue-tsc/bin/vue-tsc.js --noEmit`
预期：无输出

- [ ] **步骤 4：Commit**

```bash
git add dayan-admin/src/views/system/knowledge/index.vue
git commit -m "feat(admin): 新建知识仓库弹窗增加切分方式/向量模型/重排/改写配置（懒建库模式）"
```

### 任务 12：DocTab——上传设置/标签列/文件详情/编辑标签

**文件：** 修改 `dayan-admin/src/views/system/knowledge/detail/DocTab.vue`

- [ ] **步骤 1：上传流程改为"选文件 → 上传设置对话框 → 上传"**

script 区改造：
```ts
import { listKnowledgeCategories, updateKnowledgeDocTags } from '@/api/knowledge'
import type { KnowledgeCategory } from '@/types/knowledge'
import { KNOWLEDGE_PARSER_OPTIONS } from '@/types/knowledge'

/** 待上传文件（设置对话框确认后逐个上传） */
const pendingFiles = ref<File[]>([])
const uploadDialogVisible = ref(false)
const uploadSetting = reactive({
  categoryId: '' as string,   // 空 = 默认类目 default
  parser: 'DASHSCOPE_DOCMIND',
  tags: [] as string[]
})
const categories = ref<KnowledgeCategory[]>([])

/** 类目名映射（展示用） */
const categoryNameMap = ref(new Map<string, string>())

async function loadCategories() {
  try {
    const list = await listKnowledgeCategories()
    categories.value = list
    categoryNameMap.value = new Map(list.map((c) => [c.categoryId, c.categoryName]))
  } catch {
    categories.value = []
  }
}

onMounted(async () => {
  await loadRepoInfo()
  loadDocs()
  loadCategories()
})

/** 拖入/选择文件 → 打开上传设置 */
function handleSelectFile(options: UploadRequestOptions) {
  pendingFiles.value.push(options.file)
  uploadDialogVisible.value = true
  return Promise.resolve() // 阻止 el-upload 直接上传，由确认后统一走 handleUpload
}

/** 确认上传：按设置逐个上传 */
async function confirmUpload() {
  uploadDialogVisible.value = false
  const files = pendingFiles.value
  pendingFiles.value = []
  for (const file of files) {
    await handleUpload(file, {
      categoryId: uploadSetting.categoryId || undefined,
      parser: uploadSetting.parser,
      tags: uploadSetting.tags
    })
  }
}

async function handleUpload(file: File, opts: { categoryId?: string; parser: string; tags: string[] }) {
  const fileName = file.name
  let fileId: string
  try {
    fileId = await uploadKnowledgeDoc(props.repoId, file, opts, true)
  } catch (e) {
    const msg = e instanceof Error && e.message ? e.message : '未知原因'
    ElMessage.error(`「${fileName}」上传失败：${msg}`)
    return
  }
  const task: UploadTask = { fileId, fileName, status: 'parsing' }
  tasks.value.push(task)
  ElMessage.success(`「${fileName}」上传成功，正在解析`)
  pollParse(task)
}
```

el-upload 配置改为：
```html
      <el-upload
        :show-file-list="false"
        :http-request="handleSelectFile"
        :multiple="true"
        :accept="'.pdf,.doc,.docx,.md,.txt,.xls,.xlsx,.ppt,.pptx'"
        drag
      >
```

- [ ] **步骤 2：上传设置对话框（处理中任务卡片前）**

```html
    <el-dialog v-model="uploadDialogVisible" title="上传设置" width="480px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="文件">
          <span class="upload-file-list">
            <el-tag v-for="(f, i) in pendingFiles" :key="i" size="small" closable @close="pendingFiles.splice(i, 1)">
              {{ f.name }}
            </el-tag>
          </span>
        </el-form-item>
        <el-form-item label="所属类目">
          <el-tree-select
            v-model="uploadSetting.categoryId"
            :data="buildCategoryTree(categories)"
            node-key="categoryId"
            :props="{ label: 'categoryName', children: 'children' }"
            check-strictly
            clearable
            placeholder="默认类目"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="解析器">
          <el-select v-model="uploadSetting.parser" style="width: 100%">
            <el-option v-for="o in KNOWLEDGE_PARSER_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="uploadSetting.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入后回车创建，最多 10 个"
            style="width: 100%"
          >
            <el-option v-for="t in uploadSetting.tags" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!pendingFiles.length" @click="confirmUpload">
          上传 {{ pendingFiles.length ? `（${pendingFiles.length} 个文件）` : '' }}
        </el-button>
      </template>
    </el-dialog>
```

`buildCategoryTree` 复用任务 10 的树构造（组件内复制一份小工具函数，输入 categories 输出带 children 的树）。

- [ ] **步骤 3：表格加标签列 + 操作加详情/编辑标签**

表格列（入库状态列后）：
```html
      <el-table-column label="标签" min-width="140">
        <template #default="{ row }">
          <template v-if="row.tags?.length">
            <el-tag v-for="t in row.tags" :key="t" size="small" style="margin-right: 4px">{{ t }}</el-tag>
          </template>
          <span v-else class="no-tags">--</span>
        </template>
      </el-table-column>
```

操作列（切片按钮前）加：
```html
          <el-button link type="info" size="small" @click="openDetail(row)">详情</el-button>
          <el-button link type="warning" size="small" @click="openEditTags(row)">标签</el-button>
```

script 区加：
```ts
// ---------- 文件详情 / 编辑标签 ----------
const detailVisible = ref(false)
const detailRow = ref<KnowledgeDoc | null>(null)
const editTagsVisible = ref(false)
const editingFile = ref<KnowledgeDoc | null>(null)
const editTags = ref<string[]>([])

function openDetail(row: KnowledgeDoc) {
  detailRow.value = row
  detailVisible.value = true
}

async function openEditTags(row: KnowledgeDoc) {
  editingFile.value = row
  editTags.value = [...(row.tags || [])]
  editTagsVisible.value = true
}

async function confirmEditTags() {
  if (!editingFile.value) return
  const tags = editTags.value.slice(0, 10)
  await updateKnowledgeDocTags(props.repoId, editingFile.value.fileId, tags)
  editingFile.value.tags = tags
  editTagsVisible.value = false
  ElMessage.success('标签已更新')
}
```

模板（上传设置对话框后）：
```html
    <!-- 文件详情 -->
    <el-dialog v-model="detailVisible" title="文件详情" width="480px">
      <el-descriptions v-if="detailRow" :column="1" border>
        <el-descriptions-item label="文件名">{{ detailRow.fileName }}</el-descriptions-item>
        <el-descriptions-item label="所属类目">
          {{ detailRow.categoryId ? categoryNameMap.get(detailRow.categoryId) || detailRow.categoryId : '默认类目' }}
        </el-descriptions-item>
        <el-descriptions-item label="解析器">
          {{ KNOWLEDGE_PARSER_OPTIONS.find((o) => o.value === detailRow.parser)?.label || detailRow.parser || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="解析状态">{{ parseStatusLabel(detailRow.parseStatus) }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatSize(detailRow.sizeInBytes) }}</el-descriptions-item>
        <el-descriptions-item label="文件 ID">{{ detailRow.fileId }}</el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>

    <!-- 编辑标签 -->
    <el-dialog v-model="editTagsVisible" title="编辑标签" width="440px">
      <el-select v-model="editTags" multiple filterable allow-create default-first-option style="width: 100%" placeholder="输入后回车创建，最多 10 个">
        <el-option v-for="t in editTags" :key="t" :label="t" :value="t" />
      </el-select>
      <template #footer>
        <el-button @click="editTagsVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEditTags">保存</el-button>
      </template>
    </el-dialog>
```

- [ ] **步骤 4：类型检查**

运行：`cd F:\code\dayan\dayan-admin && node node_modules/vue-tsc/bin/vue-tsc.js --noEmit`
预期：无输出

- [ ] **步骤 5：Commit**

```bash
git add dayan-admin/src/views/system/knowledge/detail/DocTab.vue
git commit -m "feat(admin): DocTab 上传设置（类目/解析器/标签）+ 标签列 + 文件详情 + 编辑标签"
```

### 任务 13：详情页——索引配置展示与检索参数编辑

**文件：** 修改 `dayan-admin/src/views/system/knowledge/detail/index.vue`

- [ ] **步骤 1：读文件确认基本信息区结构后，在基本信息表单后追加"索引配置"展示卡**

实现要点：
- `getKnowledgeRepo` 返回的 `indexConfig` 渲染只读描述列表：
  - 切分方式：`indexConfig?.chunkMode === 'regex' ? '自定义切分' : '智能切分'`（自定义时附分隔符/切块长度/重叠）
  - 向量模型 / 重排模型 / 重排模式 / 相似度阈值 / 多轮改写 / 召回 TopK
  - 无配置显示"使用百炼默认（智能切分）"
- 已建库（`repo.indexId` 非空）时"检索参数"（denseTopK/sparseTopK/rerankMinScore）用 `el-input-number` 行内可编辑 + 保存按钮 → `updateKnowledgeRepo(id, { indexConfig })`（提交完整 indexConfig：原值 + 修改后的三个字段）
- 未建库时整卡只读提示"上传首个文档并建库后，此处仅检索参数可编辑"

- [ ] **步骤 2：类型检查**

运行：`cd F:\code\dayan\dayan-admin && node node_modules/vue-tsc/bin/vue-tsc.js --noEmit`
预期：无输出

- [ ] **步骤 3：Commit**

```bash
git add dayan-admin/src/views/system/knowledge/detail/index.vue
git commit -m "feat(admin): 知识仓库详情页索引配置展示与检索参数编辑（已建库）"
```

### 任务 14：channel 前端同步

**文件：** `dayan-channel/src` 下与 admin 对应文件同步（`api/knowledge.ts`、`types/knowledge.ts`、`views/system/knowledge/index.vue`，新建 `components/KnowledgeCategoryDialog/index.vue`）

- [ ] **步骤 1：同步 api/types（将任务 9 的改动按 channel 端文件现状适配复制）**

channel 端 `api/knowledge.ts`、`types/knowledge.ts` 结构同 admin（早期复制），直接应用任务 9 的增量；`uploadKnowledgeDoc` 若已被 channel 端调用需同步调用点（channel 端 knowledge/index.vue 的 el-upload）。

- [ ] **步骤 2：同步 DocTab 逻辑（channel 端是单页 index.vue 内嵌文档 Tab，应用任务 12 的上传设置/标签/详情改动）**

channel 端 `views/system/knowledge/index.vue` 的上传区（el-upload，L545-549 附近）应用任务 12 的对话框与列改动；文件表格列按 channel 现有列结构适配。

- [ ] **步骤 3：同步类目管理入口与创建配置表单（任务 10/11 改动）**

channel 端列表/详情为同一页（渠道树 + 面板），"类目管理"按钮放树面板工具栏；创建/绑定表单加配置区（对齐 admin）。

- [ ] **步骤 4：类型检查**

运行：`cd F:\code\dayan\dayan-channel && node node_modules/vue-tsc/bin/vue-tsc.js --noEmit`
预期：无输出

- [ ] **步骤 5：Commit**

```bash
git add dayan-channel/src
git commit -m "feat(channel): 知识仓库同步 admin——类目管理/上传设置/标签/索引配置"
```

### 任务 15：全量验证

- [ ] **步骤 1：后端全量构建 + 测试**

运行：`cd F:\code\dayan\dayan-server && mvn -pl dayan-modules/dayan-module-system,dayan-common/dayan-common-aliyun -am test`
预期：BUILD SUCCESS

- [ ] **步骤 2：前端双端类型检查**

运行：`cd F:\code\dayan\dayan-admin && node node_modules/vue-tsc/bin/vue-tsc.js --noEmit`
运行：`cd F:\code\dayan\dayan-channel && node node_modules/vue-tsc/bin/vue-tsc.js --noEmit`
预期：均无输出

- [ ] **步骤 3：迁移文件自检（SQL 语法）**

运行：`cd F:\code\dayan && git diff db/migration/90_system_knowledge_repo_index_config.sql`
预期：仅 1 条 ALTER 语句

- [ ] **步骤 4：Commit（如有遗留）**

---

## 交付后人工联调清单（需真实百炼凭据，无法自动化）

1. 类目管理：新增顶级/子类目 → 树刷新；删除空类目成功；删除含文件类目透传百炼错误
2. 上传：选类目/解析器/标签上传 PDF → 解析成功 → 懒建库应用配置（百炼控制台核对切分方式/Embedding/重排）
3. 已建库：详情页改检索参数 → 百炼侧生效；改切分方式被拒
4. 标签：上传带标签 → 列表展示 → 编辑/清空标签生效
5. 绑定模式：不显示索引配置区，正常绑定
