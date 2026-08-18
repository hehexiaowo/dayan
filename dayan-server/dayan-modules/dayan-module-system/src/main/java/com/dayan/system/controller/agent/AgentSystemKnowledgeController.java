package com.dayan.system.controller.agent;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.system.dto.SystemKnowledgeRetrieveDTO;
import com.dayan.system.service.SystemKnowledgeRepoService;
import com.dayan.system.vo.SystemKnowledgeChatVO;
import com.dayan.system.vo.SystemKnowledgeDocVO;
import com.dayan.system.vo.SystemKnowledgeRepoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent 代理人端知识库只读接口（AI 内容创作素材选择用）。
 *
 * <p>路径 {@code /agent-api/knowledge/...}（由 dayan-agent 启动模块 context-path 拼接）。
 * 仅返回当前渠道可见仓库（平台库 + 本渠道库）及其文档；channelCode 从
 * {@link ContextHolder} 强制注入，防越权。
 */
@Tag(name = "Agent 知识库")
@RestController
@RequestMapping("/system/knowledge")
@RequiredArgsConstructor
public class AgentSystemKnowledgeController {

    private final SystemKnowledgeRepoService knowledgeRepoService;

    @Operation(summary = "当前渠道可见仓库（平台库 + 本渠道库）")
    @GetMapping("/repos")
    public R<List<SystemKnowledgeRepoVO>> repos() {
        return R.ok(knowledgeRepoService.listForAgent(ContextHolder.getChannelCode()));
    }

    @Operation(summary = "可见仓库文档合并列表（按文件名过滤）")
    @GetMapping("/docs")
    public R<List<SystemKnowledgeDocVO>> docs(@RequestParam(required = false) String keyword) {
        String channelCode = ContextHolder.getChannelCode();
        List<SystemKnowledgeDocVO> result = new java.util.ArrayList<>();
        for (SystemKnowledgeRepoVO repo : knowledgeRepoService.listForAgent(channelCode)) {
            if (repo.getIndexId() == null || repo.getIndexId().isBlank()) {
                continue; // 未建库（懒建库模式）跳过
            }
            List<SystemKnowledgeDocVO> docs = knowledgeRepoService.listDocuments(repo.getId(), 1, 100, null, null);
            for (SystemKnowledgeDocVO doc : docs) {
                doc.setRepoId(repo.getId());
                doc.setRepoName(repo.getRepoName());
                result.add(doc);
            }
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            result.removeIf(d -> d.getFileName() == null || !d.getFileName().contains(kw));
        }
        return R.ok(result);
    }

    @Operation(summary = "知识检索（AI 创作素材取材，可见性同 /repos）")
    @PostMapping("/retrieve")
    public R<List<SystemKnowledgeChatVO.Citation>> retrieve(@RequestBody @Valid SystemKnowledgeRetrieveDTO dto) {
        String channelCode = ContextHolder.getChannelCode();
        boolean visible = knowledgeRepoService.listForAgent(channelCode).stream()
                .anyMatch(r -> dto.getRepoId().equals(r.getId()));
        if (!visible) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在或不可见");
        }
        int topK = dto.getTopK() == null ? 6 : Math.min(Math.max(dto.getTopK(), 1), 10);
        List<SystemKnowledgeChatVO.Citation> cites = (dto.getDocFileIds() != null && !dto.getDocFileIds().isEmpty())
                ? knowledgeRepoService.retrieveByDocuments(dto.getRepoId(), dto.getQuery(), topK, dto.getDocFileIds())
                : knowledgeRepoService.retrieve(dto.getRepoId(), dto.getQuery(), topK);
        return R.ok(cites);
    }
}
