package com.dayan.agent.controller.agent;

import com.dayan.agent.dto.AgentFavoriteAgentDTO;
import com.dayan.agent.service.AgentFavoriteService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 代理人端收藏接口。
 *
 * <p>路径：{@code /agent-api/favorites}（由 dayan-agent 启动模块 context-path 拼接）。
 * agentCode 从 {@link ContextHolder} 强制注入，所有操作自动限定当前代理人。
 *
 * <p>取消收藏用 POST {@code /favorites/cancel}（而非 DELETE）—— uni-app 的 uni.request
 * 对 DELETE body 支持不一致，POST + body 最可靠。
 */
@Tag(name = "Agent 收藏")
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class AgentFavoriteController {

    private final AgentFavoriteService agentFavoriteService;

    @Operation(summary = "新增收藏")
    @PostMapping
    public R<Long> add(@RequestBody @Valid AgentFavoriteAgentDTO dto) {
        return R.ok(agentFavoriteService.addForAgent(
                requireAgent(), dto.getTargetType(), dto.getTargetCode()));
    }

    @Operation(summary = "取消收藏")
    @PostMapping("/cancel")
    public R<Void> cancel(@RequestBody @Valid AgentFavoriteAgentDTO dto) {
        agentFavoriteService.removeByTarget(
                requireAgent(), dto.getTargetType(), dto.getTargetCode());
        return R.ok();
    }

    @Operation(summary = "查询已收藏的目标编码列表")
    @GetMapping("/codes")
    public R<List<String>> favoritedCodes(@RequestParam Integer targetType) {
        return R.ok(agentFavoriteService.listTargetCodes(requireAgent(), targetType));
    }

    private String requireAgent() {
        String agentCode = ContextHolder.getAccountCode();
        if (agentCode == null || agentCode.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return agentCode;
    }
}
