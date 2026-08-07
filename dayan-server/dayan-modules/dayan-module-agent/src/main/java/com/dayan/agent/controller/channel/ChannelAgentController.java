package com.dayan.agent.controller.channel;

import com.dayan.agent.dto.AgentInfoQueryDTO;
import com.dayan.agent.service.AgentInfoService;
import com.dayan.agent.vo.AgentInfoVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Channel 渠道端代理人接口。
 *
 * <p>路径：{@code /channel-api/agents}（由 dayan-channel 启动模块的 context-path 拼接）。
 *
 * <p>防越权：channelCode 从 {@link ContextHolder} 强制注入，不接受前端传入，
 * 确保只返回当前渠道的代理人。
 */
@Tag(name = "Channel 代理人")
@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class ChannelAgentController {

    private final AgentInfoService agentInfoService;

    @Operation(summary = "本渠道代理人列表")
    @GetMapping
    public R<PageResult<AgentInfoVO>> page(AgentInfoQueryDTO query) {
        query.setChannelCode(ContextHolder.getChannelCode());
        return R.ok(agentInfoService.page(query));
    }
}
