package com.dayan.agent.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.agent.dto.AgentClientRelBindDTO;
import com.dayan.agent.dto.AgentClientRelQueryDTO;
import com.dayan.agent.service.AgentClientRelService;
import com.dayan.agent.vo.AgentClientRelVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端代理人-客户绑定关系接口。
 */
@Tag(name = "代理人-客户绑定管理")
@RestController
@RequestMapping("/agent-client-rels")
@RequiredArgsConstructor
public class AgentClientRelAdminController {

    private final AgentClientRelService agentClientRelService;

    @Operation(summary = "绑定关系分页列表")
    @SaCheckPermission("agent:clientrel:list")
    @GetMapping
    public R<PageResult<AgentClientRelVO>> page(AgentClientRelQueryDTO query) {
        return R.ok(agentClientRelService.page(query));
    }

    @Operation(summary = "绑定客户")
    @SaCheckPermission("agent:clientrel:bind")
    @PostMapping("/bind")
    public R<Long> bind(@RequestBody @Valid AgentClientRelBindDTO dto) {
        return R.ok(agentClientRelService.bind(dto));
    }

    @Operation(summary = "解绑")
    @SaCheckPermission("agent:clientrel:unbind")
    @PutMapping("/{id}/unbind")
    public R<Void> unbind(@PathVariable Long id) {
        agentClientRelService.unbind(id);
        return R.ok();
    }

    @Operation(summary = "查代理人的客户列表")
    @SaCheckPermission("agent:clientrel:list")
    @GetMapping("/by-agent/{agentCode}")
    public R<List<AgentClientRelVO>> listByAgent(@PathVariable String agentCode) {
        return R.ok(agentClientRelService.listByAgent(agentCode));
    }

    @Operation(summary = "查客户的代理人列表")
    @SaCheckPermission("agent:clientrel:list")
    @GetMapping("/by-client/{clientCode}")
    public R<List<AgentClientRelVO>> listByClient(@PathVariable String clientCode) {
        return R.ok(agentClientRelService.listByClient(clientCode));
    }
}
