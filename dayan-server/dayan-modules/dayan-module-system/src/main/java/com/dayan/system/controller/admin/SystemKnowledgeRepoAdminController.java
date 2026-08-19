package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.aliyun.bailian.BailianKnowledgeClient;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.system.dto.SystemDocTagsDTO;
import com.dayan.system.dto.SystemKnowledgeChatDTO;
import com.dayan.system.dto.SystemKnowledgeDocImportDTO;
import com.dayan.system.dto.SystemKnowledgeRepoCreateDTO;
import com.dayan.system.dto.SystemKnowledgeRepoQueryDTO;
import com.dayan.system.dto.SystemKnowledgeRepoUpdateDTO;
import com.dayan.system.service.SystemKnowledgeRepoService;
import com.dayan.system.vo.SystemKnowledgeChatVO;
import com.dayan.system.vo.SystemKnowledgeDocVO;
import com.dayan.system.vo.SystemKnowledgeRepoTreeNodeVO;
import com.dayan.system.vo.SystemKnowledgeRepoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Admin 端知识仓库接口（百炼知识库：平台 + 每渠道一个）。
 *
 * <p>路径前缀 {@code /system/knowledge/repos}。文档与任务状态实时代理百炼远端。
 */
@Tag(name = "知识仓库管理")
@RestController
@RequestMapping("/system/knowledge/repos")
@RequiredArgsConstructor
public class SystemKnowledgeRepoAdminController {

    private final SystemKnowledgeRepoService knowledgeRepoService;

    @Operation(summary = "知识仓库分页列表")
    @SaCheckPermission("system:knowledge:repo:list")
    @GetMapping("/page")
    public R<PageResult<SystemKnowledgeRepoVO>> page(SystemKnowledgeRepoQueryDTO query) {
        return R.ok(knowledgeRepoService.page(query));
    }

    @Operation(summary = "知识仓库详情")
    @SaCheckPermission("system:knowledge:repo:query")
    @GetMapping("/{id}")
    public R<SystemKnowledgeRepoVO> getDetail(@PathVariable Long id) {
        return R.ok(knowledgeRepoService.getDetail(id));
    }

    @Operation(summary = "渠道树形知识库（root 渠道 + 全部后代；每节点含独立库/继承来源/实际可用库）")
    @SaCheckPermission("system:knowledge:repo:list")
    @GetMapping("/tree")
    public R<List<SystemKnowledgeRepoTreeNodeVO>> tree(@RequestParam(required = false) String channelCode) {
        return R.ok(knowledgeRepoService.getRepoTree(channelCode));
    }

    @Operation(summary = "创建知识仓库（新建远端索引或绑定已有）")
    @SaCheckPermission("system:knowledge:repo:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SystemKnowledgeRepoCreateDTO dto) {
        return R.ok(knowledgeRepoService.create(dto));
    }

    @Operation(summary = "更新知识仓库（名称/描述/排序）")
    @SaCheckPermission("system:knowledge:repo:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SystemKnowledgeRepoUpdateDTO dto) {
        knowledgeRepoService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除知识仓库（同时删除百炼远端索引）")
    @SaCheckPermission("system:knowledge:repo:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        knowledgeRepoService.delete(id);
        return R.ok();
    }

    @Operation(summary = "同步远端（刷新文档数与状态）")
    @SaCheckPermission("system:knowledge:repo:sync")
    @PostMapping("/{id}/sync")
    public R<Void> sync(@PathVariable Long id) {
        knowledgeRepoService.sync(id);
        return R.ok();
    }

    @Operation(summary = "懒建库：用已解析文件在百炼创建知识库（返回构建任务 JobId）")
    @SaCheckPermission("system:knowledge:repo:create")
    @PostMapping("/{id}/init-index")
    public R<String> initIndex(@PathVariable Long id, @RequestBody @Valid SystemKnowledgeDocImportDTO dto) {
        return R.ok(knowledgeRepoService.initIndex(id, dto.getFileIds()));
    }

    @Operation(summary = "建库索引构建任务状态")
    @SaCheckPermission("system:knowledge:repo:query")
    @GetMapping("/{id}/build-status")
    public R<String> getBuildStatus(@PathVariable Long id) {
        return R.ok(knowledgeRepoService.getBuildStatus(id));
    }

    // ---------- 文档管理 ----------

    @Operation(summary = "文档列表（实时代理百炼）")
    @SaCheckPermission("system:knowledge:doc:list")
    @GetMapping("/{id}/documents")
    public R<List<SystemKnowledgeDocVO>> listDocuments(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "1") int pageNumber,
                                                 @RequestParam(defaultValue = "50") int pageSize,
                                                 @RequestParam(required = false) String documentName,
                                                 @RequestParam(required = false) String documentStatus) {
        return R.ok(knowledgeRepoService.listDocuments(id, pageNumber, pageSize, documentName, documentStatus));
    }

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

    @Operation(summary = "文件解析状态")
    @SaCheckPermission("system:knowledge:doc:list")
    @GetMapping("/{id}/documents/{fileId}")
    public R<SystemKnowledgeDocVO> getDocumentParseStatus(@PathVariable Long id, @PathVariable String fileId) {
        return R.ok(knowledgeRepoService.getDocumentParseStatus(id, fileId));
    }

    @Operation(summary = "已解析文档导入索引（返回任务 JobId）")
    @SaCheckPermission("system:knowledge:doc:import")
    @PostMapping("/{id}/documents/import")
    public R<String> importDocuments(@PathVariable Long id, @RequestBody @Valid SystemKnowledgeDocImportDTO dto) {
        return R.ok(knowledgeRepoService.importDocuments(id, dto));
    }

    @Operation(summary = "文档导入索引任务状态")
    @SaCheckPermission("system:knowledge:doc:list")
    @GetMapping("/{id}/import-status/{jobId}")
    public R<String> getImportStatus(@PathVariable Long id, @PathVariable String jobId) {
        return R.ok(knowledgeRepoService.getImportStatus(id, jobId));
    }

    @Operation(summary = "删除索引内文档（远端永久删除）")
    @SaCheckPermission("system:knowledge:doc:delete")
    @DeleteMapping("/{id}/documents/{fileId}")
    public R<Void> deleteDocument(@PathVariable Long id, @PathVariable String fileId) {
        knowledgeRepoService.deleteDocument(id, fileId);
        return R.ok();
    }

    @Operation(summary = "文档切片列表（切片管理，分页实时代理百炼）")
    @SaCheckPermission("system:knowledge:doc:list")
    @GetMapping("/{id}/documents/{fileId}/chunks")
    public R<BailianKnowledgeClient.ChunkPage> listChunks(@PathVariable Long id,
                                                          @PathVariable String fileId,
                                                          @RequestParam(defaultValue = "1") int pageNum,
                                                          @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(knowledgeRepoService.listChunks(id, fileId, pageNum, pageSize));
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

    // ---------- 问答 / 检索 ----------

    @Operation(summary = "知识库问答（RAG）")
    @SaCheckPermission("system:knowledge:chat")
    @PostMapping("/{id}/chat")
    public R<SystemKnowledgeChatVO> chat(@PathVariable Long id, @RequestBody @Valid SystemKnowledgeChatDTO dto) {
        return R.ok(knowledgeRepoService.chat(id, dto));
    }

    @Operation(summary = "检索测试（仅召回片段）")
    @SaCheckPermission("system:knowledge:repo:query")
    @GetMapping("/{id}/retrieve")
    public R<List<SystemKnowledgeChatVO.Citation>> retrieve(@PathVariable Long id,
                                                      @RequestParam String query,
                                                      @RequestParam(required = false) Integer topK) {
        return R.ok(knowledgeRepoService.retrieve(id, query, topK));
    }
}
