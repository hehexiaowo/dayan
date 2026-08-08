package com.dayan.agent.controller.channel;

import com.dayan.agent.dto.AgentInfoQueryDTO;
import com.dayan.agent.dto.AgentShareRecordQueryDTO;
import com.dayan.agent.service.AgentInfoService;
import com.dayan.agent.service.AgentShareRecordService;
import com.dayan.agent.vo.AgentInfoVO;
import com.dayan.agent.vo.AgentShareRecordVO;
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
 * Channel 渠道端分享记录接口。
 *
 * <p>路径：{@code /channel-api/agent-share-records}。
 * 防越权：agent_share_record 表无 channel_code 列，靠反查本渠道 agentCodes 做 IN 过滤。
 */
@Tag(name = "Channel 分享记录")
@RestController
@RequestMapping("/agent-share-records")
@RequiredArgsConstructor
public class ChannelAgentShareRecordController {

    private final AgentShareRecordService agentShareRecordService;
    private final AgentInfoService agentInfoService;

    @Operation(summary = "本渠道分享记录列表")
    @SaCheckPermission("channel:shareRecord:list")
    @GetMapping
    public R<PageResult<AgentShareRecordVO>> page(AgentShareRecordQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        List<String> agentCodes = collectChannelAgentCodes(channelCode);
        if (agentCodes.isEmpty()) {
            return R.ok(new PageResult<>(query.getCurrent(), query.getSize(), 0L, Collections.emptyList()));
        }
        query.setAgentCodes(agentCodes);
        return R.ok(agentShareRecordService.page(query));
    }

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
