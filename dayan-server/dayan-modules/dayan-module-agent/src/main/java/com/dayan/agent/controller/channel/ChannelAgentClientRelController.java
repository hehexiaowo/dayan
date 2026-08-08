package com.dayan.agent.controller.channel;

import com.dayan.agent.dto.AgentClientRelQueryDTO;
import com.dayan.agent.dto.AgentInfoQueryDTO;
import com.dayan.agent.service.AgentClientRelService;
import com.dayan.agent.service.AgentInfoService;
import com.dayan.agent.vo.AgentClientRelVO;
import com.dayan.agent.vo.AgentInfoVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel 渠道端客户线索（代理人-客户绑定关系）接口。
 *
 * <p>路径：{@code /channel-api/agent-client-rels}。
 * 防越权：agent_client_rel 表无 channel_code 列，靠反查本渠道代理人编码集合（agentCodes）做 IN 过滤。
 */
@Tag(name = "Channel 客户线索")
@RestController
@RequestMapping("/agent-client-rels")
@RequiredArgsConstructor
public class ChannelAgentClientRelController {

    private final AgentClientRelService agentClientRelService;
    private final AgentInfoService agentInfoService;

    @Operation(summary = "本渠道客户线索列表")
    @SaCheckPermission("channel:agentClient:list")
    @GetMapping
    public R<PageResult<AgentClientRelVO>> page(AgentClientRelQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        List<String> agentCodes = collectChannelAgentCodes(channelCode);
        if (agentCodes.isEmpty()) {
            return R.ok(new PageResult<>(query.getCurrent(), query.getSize(), 0L, Collections.emptyList()));
        }
        query.setAgentCodes(agentCodes);
        return R.ok(agentClientRelService.page(query));
    }

    /**
     * 收集本渠道全部代理人编码（agent_info 按 channelCode 查）。
     * AgentInfoService 无 list() 方法，用 page 大页查。
     */
    private List<String> collectChannelAgentCodes(String channelCode) {
        AgentInfoQueryDTO q = new AgentInfoQueryDTO();
        q.setChannelCode(channelCode);
        q.setCurrent(1L);
        q.setSize(1000L);
        return agentInfoService.page(q).getRecords().stream()
                .map(AgentInfoVO::getAgentCode)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toList());
    }
}
