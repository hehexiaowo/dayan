package com.dayan.agent.controller.channel;

import com.dayan.agent.dto.AgentAccountQueryDTO;
import com.dayan.agent.service.AgentAccountService;
import com.dayan.agent.vo.AgentAccountVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Channel 渠道端代理人账号接口。
 *
 * <p>路径：{@code /channel-api/agent-accounts}。
 * 防越权：channelCode 从 {@link ContextHolder} 注入，agent_account 表有 channel_code 列，
 * TenantHandler 自动追加隔离条件（显式注入更安全）。
 */
@Tag(name = "Channel 代理人账号")
@RestController
@RequestMapping("/agent-accounts")
@RequiredArgsConstructor
public class ChannelAgentAccountController {

    private final AgentAccountService agentAccountService;

    @Operation(summary = "本渠道代理人账号列表")
    @SaCheckPermission("channel:agent:list")
    @GetMapping
    public R<PageResult<AgentAccountVO>> page(AgentAccountQueryDTO query) {
        query.setChannelCode(ContextHolder.getChannelCode());
        return R.ok(agentAccountService.page(query));
    }

    @Operation(summary = "代理人账号详情")
    @SaCheckPermission("channel:agent:query")
    @GetMapping("/{agentCode}")
    public R<AgentAccountVO> getDetail(@PathVariable String agentCode) {
        AgentAccountVO vo = agentAccountService.getDetail(agentCode);
        if (vo == null || !ContextHolder.getChannelCode().equals(vo.getChannelCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在或无权访问");
        }
        return R.ok(vo);
    }
}
