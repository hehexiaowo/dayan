package com.dayan.tool.controller.channel;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.system.service.SystemKnowledgeRepoService;
import com.dayan.system.vo.SystemKnowledgeRepoVO;
import com.dayan.tool.dto.ToolChannelRepoBindDTO;
import com.dayan.tool.service.ToolChannelRepoBindService;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.vo.ToolChannelPersonaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Channel 渠道端 AI 问答人物接口（渠道补充知识库）。
 *
 * <p>路径 {@code /tools/aichat}（dayan-channel starter context-path 拼接为 {@code /channel-api/tools/aichat/*}）。
 *
 * <p>渠道隔离：channelCode 一律从 {@link ContextHolder} 强制注入，不接收前端参数；
 * 补充范围仅限本渠道 + 后代渠道名下的渠道库（{@code listChannelScopeRepos} 校验）。</p>
 */
@Tag(name = "Channel AI 问答人物")
@RestController
@RequestMapping("/tools/aichat")
@RequiredArgsConstructor
public class ChannelToolAichatController {

    private final ToolInfoService toolInfoService;
    private final ToolChannelRepoBindService bindService;
    private final SystemKnowledgeRepoService knowledgeRepoService;

    @Operation(summary = "启用中问答人物列表（含 admin 全局库与本渠道补充库）")
    @SaCheckPermission("channel:tool:aichat:view")
    @GetMapping("/personas")
    public R<List<ToolChannelPersonaVO>> personas() {
        return R.ok(toolInfoService.listChannelPersonas(ContextHolder.getChannelCode()));
    }

    @Operation(summary = "可补充知识库（本渠道 + 后代渠道名下，不含平台库）")
    @SaCheckPermission("channel:tool:aichat:view")
    @GetMapping("/repos/options")
    public R<List<SystemKnowledgeRepoVO>> repoOptions() {
        return R.ok(knowledgeRepoService.listChannelScopeRepos(ContextHolder.getChannelCode()));
    }

    @Operation(summary = "保存人物补充知识库（全量替换；空数组 = 清空补充）")
    @SaCheckPermission("channel:tool:aichat:update")
    @PutMapping("/personas/{toolCode}/repos")
    public R<Void> saveRepos(@PathVariable String toolCode,
                             @RequestBody @Valid ToolChannelRepoBindDTO dto) {
        // 人物存在性校验（不存在/非 aichat 类型抛 NOT_FOUND）
        toolInfoService.getQaPersona(toolCode);
        bindService.saveChannelRepos(toolCode, ContextHolder.getChannelCode(), dto.getRepoIds());
        return R.ok();
    }
}
