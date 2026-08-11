package com.dayan.agent.controller.agent;

import com.dayan.agent.dto.AgentCardCreateDTO;
import com.dayan.agent.dto.AgentCardQueryDTO;
import com.dayan.agent.dto.AgentCardUpdateDTO;
import com.dayan.agent.service.AgentCardService;
import com.dayan.agent.vo.AgentCardVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 代理人端电子名片接口。
 *
 * <p>路径：{@code /agent-api/agent-cards}（由 dayan-agent 启动模块的 context-path 拼接）。
 * 一个代理人可创建多张名片，所有操作自动限定为当前登录代理人。
 */
@Tag(name = "Agent 名片")
@RestController
@RequestMapping("/agent-cards")
@RequiredArgsConstructor
public class AgentCardController {

    private final AgentCardService agentCardService;

    @Operation(summary = "分页查询我的名片")
    @GetMapping
    public R<PageResult<AgentCardVO>> page(AgentCardQueryDTO query) {
        return R.ok(agentCardService.page(query));
    }

    @Operation(summary = "名片详情")
    @GetMapping("/{id}")
    public R<AgentCardVO> detail(@PathVariable Long id) {
        return R.ok(agentCardService.detail(id));
    }

    @Operation(summary = "新增名片")
    @OperationLog(module = "名片", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid AgentCardCreateDTO dto) {
        return R.ok(agentCardService.create(dto));
    }

    @Operation(summary = "更新名片")
    @OperationLog(module = "名片", action = "更新")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody AgentCardUpdateDTO dto) {
        agentCardService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除名片")
    @OperationLog(module = "名片", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        agentCardService.delete(id);
        return R.ok();
    }
}
