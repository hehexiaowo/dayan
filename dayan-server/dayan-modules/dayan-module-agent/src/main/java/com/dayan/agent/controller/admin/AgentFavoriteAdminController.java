package com.dayan.agent.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.agent.dto.AgentFavoriteAddDTO;
import com.dayan.agent.dto.AgentFavoriteQueryDTO;
import com.dayan.agent.service.AgentFavoriteService;
import com.dayan.agent.vo.AgentFavoriteVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端代理人收藏接口。
 */
@Tag(name = "代理人收藏管理")
@RestController
@RequestMapping("/agent-favorites")
@RequiredArgsConstructor
public class AgentFavoriteAdminController {

    private final AgentFavoriteService agentFavoriteService;

    @Operation(summary = "收藏分页列表")
    @SaCheckPermission("agent:favorite:list")
    @GetMapping
    public R<PageResult<AgentFavoriteVO>> page(AgentFavoriteQueryDTO query) {
        return R.ok(agentFavoriteService.page(query));
    }

    @Operation(summary = "新增收藏")
    @SaCheckPermission("agent:favorite:add")
    @PostMapping
    public R<Long> add(@RequestBody @Valid AgentFavoriteAddDTO dto) {
        return R.ok(agentFavoriteService.add(dto));
    }

    @Operation(summary = "取消收藏")
    @SaCheckPermission("agent:favorite:remove")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        agentFavoriteService.remove(id);
        return R.ok();
    }

    @Operation(summary = "查代理人的收藏列表")
    @SaCheckPermission("agent:favorite:list")
    @GetMapping("/by-agent/{agentCode}")
    public R<List<AgentFavoriteVO>> listByAgent(@PathVariable String agentCode) {
        return R.ok(agentFavoriteService.listByAgent(agentCode));
    }
}
