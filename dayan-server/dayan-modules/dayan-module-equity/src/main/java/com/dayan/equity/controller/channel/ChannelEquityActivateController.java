package com.dayan.equity.controller.channel;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.equity.dto.EquityActivateQueryDTO;
import com.dayan.equity.dto.EquityDepotQueryDTO;
import com.dayan.equity.service.EquityActivateService;
import com.dayan.equity.service.EquityDepotService;
import com.dayan.equity.vo.EquityActivateVO;
import com.dayan.equity.vo.EquityDepotVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel 渠道端权益激活记录接口。
 *
 * <p>路径：{@code /channel-api/equity-activates}。
 * 防越权：equity_activate 表无 channel_code 列，靠反查本渠道 equity_depot 得 equityCode 集合做 IN 过滤。
 */
@Tag(name = "Channel 激活记录")
@RestController
@RequestMapping("/equity-activates")
@RequiredArgsConstructor
public class ChannelEquityActivateController {

    private final EquityActivateService equityActivateService;
    private final EquityDepotService equityDepotService;

    @Operation(summary = "本渠道权益激活记录列表")
    @SaCheckPermission("channel:activate:list")
    @GetMapping
    public R<PageResult<EquityActivateVO>> page(EquityActivateQueryDTO query) {
        String channelCode = ContextHolder.getChannelCode();
        List<String> equityCodes = collectChannelEquityCodes(channelCode);
        if (equityCodes.isEmpty()) {
            return R.ok(new PageResult<>(query.getCurrent(), query.getSize(), 0L, Collections.emptyList()));
        }
        query.setEquityCodes(equityCodes);
        return R.ok(equityActivateService.page(query));
    }

    /**
     * 收集本渠道全部权益编码（equity_depot 按 channelCode 查）。
     */
    private List<String> collectChannelEquityCodes(String channelCode) {
        EquityDepotQueryDTO q = new EquityDepotQueryDTO();
        q.setChannelCode(channelCode);
        q.setCurrent(1L);
        q.setSize(1000L);
        return equityDepotService.page(q).getRecords().stream()
                .map(EquityDepotVO::getEquityCode)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toList());
    }
}
