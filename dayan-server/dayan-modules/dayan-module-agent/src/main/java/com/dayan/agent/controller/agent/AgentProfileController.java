package com.dayan.agent.controller.agent;

import com.dayan.agent.service.AgentProfileService;
import com.dayan.agent.vo.AgentProfileVO;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 代理人端个人资料接口。
 *
 * <p>路径：{@code /agent-api/profile/*}。所有操作仅作用于当前登录代理人。
 */
@Tag(name = "Agent 个人资料")
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class AgentProfileController {

    private final AgentProfileService agentProfileService;

    @Operation(summary = "我的资料")
    @GetMapping
    public R<AgentProfileVO> get() {
        return R.ok(agentProfileService.getProfile());
    }
}
