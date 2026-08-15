package com.dayan.knowledge.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.knowledge.dto.KnowledgeChatDTO;
import com.dayan.knowledge.dto.KnowledgeDocImportDTO;
import com.dayan.knowledge.dto.KnowledgeRepoCreateDTO;
import com.dayan.knowledge.dto.KnowledgeRepoQueryDTO;
import com.dayan.knowledge.dto.KnowledgeRepoUpdateDTO;
import com.dayan.knowledge.service.KnowledgeRepoService;
import com.dayan.knowledge.vo.KnowledgeChatVO;
import com.dayan.knowledge.vo.KnowledgeDocVO;
import com.dayan.knowledge.vo.KnowledgeRepoVO;
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
 * <p>路径前缀 {@code /knowledge/repos}。文档与任务状态实时代理百炼远端。
 */
@Tag(name = "知识仓库管理")
@RestController
@RequestMapping("/knowledge/repos")
@RequiredArgsConstructor
public class KnowledgeRepoAdminController {

    private final KnowledgeRepoService knowledgeRepoService;

    @Operation(summary = "知识仓库分页列表")
    @SaCheckPermission("knowledge:repo:list")
    @GetMapping("/page")
    public R<PageResult<KnowledgeRepoVO>> page(KnowledgeRepoQueryDTO query) {
        return R.ok(knowledgeRepoService.page(query));
    }

    @Operation(summary = "知识仓库详情")
    @SaCheckPermission("knowledge:repo:query")
    @GetMapping("/{id}")
    public R<KnowledgeRepoVO> getDetail(@PathVariable Long id) {
        return R.ok(knowledgeRepoService.getDetail(id));
    }

    @Operation(summary = "创建知识仓库（新建远端索引或绑定已有）")
    @SaCheckPermission("knowledge:repo:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid KnowledgeRepoCreateDTO dto) {
        return R.ok(knowledgeRepoService.create(dto));
    }

    @Operation(summary = "更新知识仓库（名称/描述/排序）")
    @SaCheckPermission("knowledge:repo:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody KnowledgeRepoUpdateDTO dto) {
        knowledgeRepoService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除知识仓库（同时删除百炼远端索引）")
    @SaCheckPermission("knowledge:repo:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        knowledgeRepoService.delete(id);
        return R.ok();
    }

    @Operation(summary = "同步远端（刷新文档数与状态）")
    @SaCheckPermission("knowledge:repo:sync")
    @PostMapping("/{id}/sync")
    public R<Void> sync(@PathVariable Long id) {
        knowledgeRepoService.sync(id);
        return R.ok();
    }

    @Operation(summary = "懒建库：用已解析文件在百炼创建知识库（返回构建任务 JobId）")
    @SaCheckPermission("knowledge:repo:create")
    @PostMapping("/{id}/init-index")
    public R<String> initIndex(@PathVariable Long id, @RequestBody @Valid KnowledgeDocImportDTO dto) {
        return R.ok(knowledgeRepoService.initIndex(id, dto.getFileIds()));
    }

    @Operation(summary = "建库索引构建任务状态")
    @SaCheckPermission("knowledge:repo:query")
    @GetMapping("/{id}/build-status")
    public R<String> getBuildStatus(@PathVariable Long id) {
        return R.ok(knowledgeRepoService.getBuildStatus(id));
    }

    // ---------- 文档管理 ----------

    @Operation(summary = "文档列表（实时代理百炼）")
    @SaCheckPermission("knowledge:doc:list")
    @GetMapping("/{id}/documents")
    public R<List<KnowledgeDocVO>> listDocuments(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "1") int pageNumber,
                                                 @RequestParam(defaultValue = "50") int pageSize,
                                                 @RequestParam(required = false) String documentName,
                                                 @RequestParam(required = false) String documentStatus) {
        return R.ok(knowledgeRepoService.listDocuments(id, pageNumber, pageSize, documentName, documentStatus));
    }

    @Operation(summary = "上传文档（返回 FileId，解析异步进行）")
    @SaCheckPermission("knowledge:doc:upload")
    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<String> uploadDocument(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return R.ok(knowledgeRepoService.uploadDocument(id, file));
    }

    @Operation(summary = "文件解析状态")
    @SaCheckPermission("knowledge:doc:list")
    @GetMapping("/{id}/documents/{fileId}")
    public R<KnowledgeDocVO> getDocumentParseStatus(@PathVariable Long id, @PathVariable String fileId) {
        return R.ok(knowledgeRepoService.getDocumentParseStatus(id, fileId));
    }

    @Operation(summary = "已解析文档导入索引（返回任务 JobId）")
    @SaCheckPermission("knowledge:doc:import")
    @PostMapping("/{id}/documents/import")
    public R<String> importDocuments(@PathVariable Long id, @RequestBody @Valid KnowledgeDocImportDTO dto) {
        return R.ok(knowledgeRepoService.importDocuments(id, dto));
    }

    @Operation(summary = "文档导入索引任务状态")
    @SaCheckPermission("knowledge:doc:list")
    @GetMapping("/{id}/import-status/{jobId}")
    public R<String> getImportStatus(@PathVariable Long id, @PathVariable String jobId) {
        return R.ok(knowledgeRepoService.getImportStatus(id, jobId));
    }

    @Operation(summary = "删除索引内文档（远端永久删除）")
    @SaCheckPermission("knowledge:doc:delete")
    @DeleteMapping("/{id}/documents/{fileId}")
    public R<Void> deleteDocument(@PathVariable Long id, @PathVariable String fileId) {
        knowledgeRepoService.deleteDocument(id, fileId);
        return R.ok();
    }

    // ---------- 问答 / 检索 ----------

    @Operation(summary = "知识库问答（RAG）")
    @SaCheckPermission("knowledge:chat")
    @PostMapping("/{id}/chat")
    public R<KnowledgeChatVO> chat(@PathVariable Long id, @RequestBody @Valid KnowledgeChatDTO dto) {
        return R.ok(knowledgeRepoService.chat(id, dto));
    }

    @Operation(summary = "检索测试（仅召回片段）")
    @SaCheckPermission("knowledge:repo:query")
    @GetMapping("/{id}/retrieve")
    public R<List<KnowledgeChatVO.Citation>> retrieve(@PathVariable Long id,
                                                      @RequestParam String query,
                                                      @RequestParam(required = false) Integer topK) {
        return R.ok(knowledgeRepoService.retrieve(id, query, topK));
    }
}
