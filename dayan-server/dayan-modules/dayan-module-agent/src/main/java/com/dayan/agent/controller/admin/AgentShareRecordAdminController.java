package com.dayan.agent.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.agent.dto.AgentShareRecordCreateDTO;
import com.dayan.agent.dto.AgentShareRecordQueryDTO;
import com.dayan.agent.service.AgentShareRecordService;
import com.dayan.agent.vo.AgentShareRecordVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端代理人分享记录接口。
 */
@Tag(name = "代理人分享记录管理")
@RestController
@RequestMapping("/agent-share-records")
@RequiredArgsConstructor
public class AgentShareRecordAdminController {

    private final AgentShareRecordService agentShareRecordService;

    @Operation(summary = "分享记录分页列表")
    @SaCheckPermission("agent:share:list")
    @GetMapping
    public R<PageResult<AgentShareRecordVO>> page(AgentShareRecordQueryDTO query) {
        return R.ok(agentShareRecordService.page(query));
    }

    @Operation(summary = "新增分享记录")
    @SaCheckPermission("agent:share:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid AgentShareRecordCreateDTO dto) {
        return R.ok(agentShareRecordService.create(dto));
    }

    @Operation(summary = "查代理人分享记录列表")
    @SaCheckPermission("agent:share:list")
    @GetMapping("/by-agent/{agentCode}")
    public R<List<AgentShareRecordVO>> listByAgent(@PathVariable String agentCode) {
        return R.ok(agentShareRecordService.listByAgent(agentCode));
    }
}
