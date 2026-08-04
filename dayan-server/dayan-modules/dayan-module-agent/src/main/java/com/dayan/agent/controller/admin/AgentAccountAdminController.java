package com.dayan.agent.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.agent.dto.AgentAccountCreateDTO;
import com.dayan.agent.dto.AgentAccountQueryDTO;
import com.dayan.agent.dto.AgentAccountUpdateDTO;
import com.dayan.agent.service.AgentAccountService;
import com.dayan.agent.vo.AgentAccountVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端代理人账号接口。
 */
@Tag(name = "代理人账号管理")
@RestController
@RequestMapping("/agent-accounts")
@RequiredArgsConstructor
public class AgentAccountAdminController {

    private final AgentAccountService agentAccountService;

    @Operation(summary = "代理人账号分页列表")
    @SaCheckPermission("agent:account:list")
    @GetMapping
    public R<PageResult<AgentAccountVO>> page(AgentAccountQueryDTO query) {
        return R.ok(agentAccountService.page(query));
    }

    @Operation(summary = "代理人账号详情")
    @SaCheckPermission("agent:account:query")
    @GetMapping("/{agentCode}")
    public R<AgentAccountVO> getDetail(@PathVariable String agentCode) {
        return R.ok(agentAccountService.getDetail(agentCode));
    }

    @Operation(summary = "新增代理人账号")
    @SaCheckPermission("agent:account:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid AgentAccountCreateDTO dto) {
        return R.ok(agentAccountService.create(dto));
    }

    @Operation(summary = "修改代理人账号")
    @SaCheckPermission("agent:account:update")
    @PutMapping("/{agentCode}")
    public R<Void> update(@PathVariable String agentCode, @RequestBody @Valid AgentAccountUpdateDTO dto) {
        agentAccountService.update(agentCode, dto);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @SaCheckPermission("agent:account:reset")
    @PutMapping("/{agentCode}/reset-password")
    public R<Void> resetPassword(@PathVariable String agentCode) {
        agentAccountService.resetPassword(agentCode);
        return R.ok();
    }

    @Operation(summary = "删除代理人账号")
    @SaCheckPermission("agent:account:delete")
    @DeleteMapping("/{agentCode}")
    public R<Void> delete(@PathVariable String agentCode) {
        agentAccountService.delete(agentCode);
        return R.ok();
    }
}
