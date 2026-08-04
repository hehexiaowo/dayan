package com.dayan.agent.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.agent.dto.AgentInfoCreateDTO;
import com.dayan.agent.dto.AgentInfoQueryDTO;
import com.dayan.agent.dto.AgentInfoUpdateDTO;
import com.dayan.agent.service.AgentInfoService;
import com.dayan.agent.vo.AgentInfoVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端代理人信息接口。
 *
 * <p>路径前缀由 dayan-admin 启动模块的 context-path（{@code /admin-api}）拼接。
 */
@Tag(name = "代理人信息管理")
@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentInfoAdminController {

    private final AgentInfoService agentInfoService;

    @Operation(summary = "代理人分页列表")
    @SaCheckPermission("agent:info:list")
    @GetMapping
    public R<PageResult<AgentInfoVO>> page(AgentInfoQueryDTO query) {
        return R.ok(agentInfoService.page(query));
    }

    @Operation(summary = "代理人详情")
    @SaCheckPermission("agent:info:query")
    @GetMapping("/{agentCode}")
    public R<AgentInfoVO> getDetail(@PathVariable String agentCode) {
        return R.ok(agentInfoService.getDetail(agentCode));
    }

    @Operation(summary = "新增代理人")
    @SaCheckPermission("agent:info:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid AgentInfoCreateDTO dto) {
        return R.ok(agentInfoService.create(dto));
    }

    @Operation(summary = "修改代理人")
    @SaCheckPermission("agent:info:update")
    @PutMapping("/{agentCode}")
    public R<Void> update(@PathVariable String agentCode, @RequestBody @Valid AgentInfoUpdateDTO dto) {
        agentInfoService.update(agentCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除代理人")
    @SaCheckPermission("agent:info:delete")
    @DeleteMapping("/{agentCode}")
    public R<Void> delete(@PathVariable String agentCode) {
        agentInfoService.delete(agentCode);
        return R.ok();
    }
}
