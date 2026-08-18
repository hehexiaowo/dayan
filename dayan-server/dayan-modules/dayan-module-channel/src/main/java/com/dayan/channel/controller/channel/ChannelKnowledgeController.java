package com.dayan.channel.controller.channel;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.aliyun.bailian.BailianKnowledgeClient;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.knowledge.dto.KnowledgeChatDTO;
import com.dayan.knowledge.dto.KnowledgeDocImportDTO;
import com.dayan.knowledge.dto.KnowledgeRepoCreateDTO;
import com.dayan.knowledge.dto.KnowledgeRepoUpdateDTO;
import com.dayan.knowledge.service.KnowledgeRepoService;
import com.dayan.knowledge.vo.KnowledgeChatVO;
import com.dayan.knowledge.vo.KnowledgeDocVO;
import com.dayan.knowledge.vo.KnowledgeRepoTreeNodeVO;
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
 * Channel 渠道端知识仓库接口（本渠道知识库管理 + 树形继承视图）。
 *
 * <p>路径 {@code /channel-api/knowledge/repos/...}（由 dayan-channel 启动模块 context-path 拼接）。
 *
 * <p>渠道隔离双保险：
 * <ol>
 *   <li>channelCode 一律从 {@link ContextHolder} 强制注入（创建/查当前仓库），不接收前端参数；</li>
 *   <li>knowledge_repo 不在租户忽略清单，MyBatis-Plus TenantLineInnerInterceptor
 *       会对本端所有查询自动追加 {@code channel_code = 本渠道} 条件——按 id 操作跨渠道仓库
 *       直接查不到（NOT_FOUND），平台库（channel_code=NULL）天然不可见。</li>
 * </ol>
 *
 * <p>树形继承（{@code /tree}）：跳过租户拦截批量查「本渠道 + 后代」仓库，业务层以渠道树范围
 * 校验；chat/retrieve 走 {@code requireRepoVisible}（当前渠道 ∪ 祖先 ∪ 后代），
 * 子渠道可对继承的祖先库问答，但管理操作（update/delete/upload 等）仍受租户拦截限制在本渠道。
 */
@Tag(name = "Channel 知识仓库")
@RestController
@RequestMapping("/knowledge/repos")
@RequiredArgsConstructor
public class ChannelKnowledgeController {

    private final KnowledgeRepoService knowledgeRepoService;

    @Operation(summary = "本渠道知识仓库（未创建返回 null）")
    @SaCheckPermission("channel:knowledge:view")
    @GetMapping("/current")
    public R<KnowledgeRepoVO> current() {
        return R.ok(knowledgeRepoService.getByChannelCode(ContextHolder.getChannelCode()));
    }

    @Operation(summary = "渠道树形知识库（本渠道 + 全部后代；每节点含独立库/继承来源/实际可用库）")
    @SaCheckPermission("channel:knowledge:view")
    @GetMapping("/tree")
    public R<List<KnowledgeRepoTreeNodeVO>> tree() {
        return R.ok(knowledgeRepoService.getRepoTree(ContextHolder.getChannelCode()));
    }

    @Operation(summary = "创建本渠道知识仓库（懒建库，上传首个文档后自动在百炼建库）")
    @SaCheckPermission("channel:knowledge:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid KnowledgeRepoCreateDTO dto) {
        dto.setRepoType(2);
        dto.setChannelCode(ContextHolder.getChannelCode());
        return R.ok(knowledgeRepoService.create(dto));
    }

    @Operation(summary = "更新本渠道知识仓库（名称/描述/排序）")
    @SaCheckPermission("channel:knowledge:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody KnowledgeRepoUpdateDTO dto) {
        knowledgeRepoService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除本渠道知识仓库（同时删除百炼远端索引）")
    @SaCheckPermission("channel:knowledge:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        knowledgeRepoService.delete(id);
        return R.ok();
    }

    @Operation(summary = "同步远端（刷新文档数与状态）")
    @SaCheckPermission("channel:knowledge:sync")
    @PostMapping("/{id}/sync")
    public R<Void> sync(@PathVariable Long id) {
        knowledgeRepoService.sync(id);
        return R.ok();
    }

    @Operation(summary = "懒建库：用已解析文件在百炼创建知识库（返回构建任务 JobId）")
    @SaCheckPermission("channel:knowledge:create")
    @PostMapping("/{id}/init-index")
    public R<String> initIndex(@PathVariable Long id, @RequestBody @Valid KnowledgeDocImportDTO dto) {
        return R.ok(knowledgeRepoService.initIndex(id, dto.getFileIds()));
    }

    @Operation(summary = "建库索引构建任务状态")
    @SaCheckPermission("channel:knowledge:view")
    @GetMapping("/{id}/build-status")
    public R<String> getBuildStatus(@PathVariable Long id) {
        return R.ok(knowledgeRepoService.getBuildStatus(id));
    }

    // ---------- 文档管理 ----------

    @Operation(summary = "文档列表（实时代理百炼）")
    @SaCheckPermission("channel:knowledge:view")
    @GetMapping("/{id}/documents")
    public R<List<KnowledgeDocVO>> listDocuments(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "1") int pageNumber,
                                                 @RequestParam(defaultValue = "50") int pageSize,
                                                 @RequestParam(required = false) String documentName,
                                                 @RequestParam(required = false) String documentStatus) {
        return R.ok(knowledgeRepoService.listDocuments(id, pageNumber, pageSize, documentName, documentStatus));
    }

    @Operation(summary = "上传文档（返回 FileId，解析异步进行）")
    @SaCheckPermission("channel:knowledge:doc:upload")
    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<String> uploadDocument(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return R.ok(knowledgeRepoService.uploadDocument(id, file));
    }

    @Operation(summary = "文件解析状态")
    @SaCheckPermission("channel:knowledge:view")
    @GetMapping("/{id}/documents/{fileId}")
    public R<KnowledgeDocVO> getDocumentParseStatus(@PathVariable Long id, @PathVariable String fileId) {
        return R.ok(knowledgeRepoService.getDocumentParseStatus(id, fileId));
    }

    @Operation(summary = "已解析文档导入索引（返回任务 JobId）")
    @SaCheckPermission("channel:knowledge:doc:upload")
    @PostMapping("/{id}/documents/import")
    public R<String> importDocuments(@PathVariable Long id, @RequestBody @Valid KnowledgeDocImportDTO dto) {
        return R.ok(knowledgeRepoService.importDocuments(id, dto));
    }

    @Operation(summary = "文档导入索引任务状态")
    @SaCheckPermission("channel:knowledge:view")
    @GetMapping("/{id}/import-status/{jobId}")
    public R<String> getImportStatus(@PathVariable Long id, @PathVariable String jobId) {
        return R.ok(knowledgeRepoService.getImportStatus(id, jobId));
    }

    @Operation(summary = "删除索引内文档（远端永久删除）")
    @SaCheckPermission("channel:knowledge:doc:delete")
    @DeleteMapping("/{id}/documents/{fileId}")
    public R<Void> deleteDocument(@PathVariable Long id, @PathVariable String fileId) {
        knowledgeRepoService.deleteDocument(id, fileId);
        return R.ok();
    }

    @Operation(summary = "文档切片列表（切片管理，分页实时代理百炼）")
    @SaCheckPermission("channel:knowledge:view")
    @GetMapping("/{id}/documents/{fileId}/chunks")
    public R<BailianKnowledgeClient.ChunkPage> listChunks(@PathVariable Long id,
                                                          @PathVariable String fileId,
                                                          @RequestParam(defaultValue = "1") int pageNum,
                                                          @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(knowledgeRepoService.listChunks(id, fileId, pageNum, pageSize));
    }

    // ---------- 问答 / 检索 ----------

    @Operation(summary = "知识库问答（RAG）")
    @SaCheckPermission("channel:knowledge:chat")
    @PostMapping("/{id}/chat")
    public R<KnowledgeChatVO> chat(@PathVariable Long id, @RequestBody @Valid KnowledgeChatDTO dto) {
        return R.ok(knowledgeRepoService.chat(id, dto));
    }

    @Operation(summary = "检索测试（仅召回片段）")
    @SaCheckPermission("channel:knowledge:view")
    @GetMapping("/{id}/retrieve")
    public R<List<KnowledgeChatVO.Citation>> retrieve(@PathVariable Long id,
                                                      @RequestParam String query,
                                                      @RequestParam(required = false) Integer topK) {
        return R.ok(knowledgeRepoService.retrieve(id, query, topK));
    }
}
