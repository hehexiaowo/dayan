package com.dayan.knowledge.controller.agent;

import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.knowledge.service.KnowledgeRepoService;
import com.dayan.knowledge.vo.KnowledgeDocVO;
import com.dayan.knowledge.vo.KnowledgeRepoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class AgentKnowledgeController {

    private final KnowledgeRepoService knowledgeRepoService;

    @Operation(summary = "当前渠道可见仓库（平台库 + 本渠道库）")
    @GetMapping("/repos")
    public R<List<KnowledgeRepoVO>> repos() {
        return R.ok(knowledgeRepoService.listForAgent(ContextHolder.getChannelCode()));
    }

    @Operation(summary = "可见仓库文档合并列表（按文件名过滤）")
    @GetMapping("/docs")
    public R<List<KnowledgeDocVO>> docs(@RequestParam(required = false) String keyword) {
        String channelCode = ContextHolder.getChannelCode();
        List<KnowledgeDocVO> result = new java.util.ArrayList<>();
        for (KnowledgeRepoVO repo : knowledgeRepoService.listForAgent(channelCode)) {
            if (repo.getIndexId() == null || repo.getIndexId().isBlank()) {
                continue; // 未建库（懒建库模式）跳过
            }
            List<KnowledgeDocVO> docs = knowledgeRepoService.listDocuments(repo.getId(), 1, 100, null, null);
            for (KnowledgeDocVO doc : docs) {
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
}
