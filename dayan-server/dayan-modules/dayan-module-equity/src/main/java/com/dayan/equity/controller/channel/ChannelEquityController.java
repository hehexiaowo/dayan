package com.dayan.equity.controller.channel;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.equity.dto.EquityDepotQueryDTO;
import com.dayan.equity.service.EquityDepotService;
import com.dayan.equity.vo.EquityDepotVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Channel 渠道端权益接口。
 *
 * <p>路径：{@code /channel-api/equities}（由 dayan-channel 启动模块的 context-path 拼接）。
 *
 * <p>防越权：channelCode 从 {@link ContextHolder} 强制注入，不接受前端传入。
 * 只读列表查询，不触发权益状态机写链路。
 */
@Tag(name = "Channel 权益")
@RestController
@RequestMapping("/equities")
@RequiredArgsConstructor
public class ChannelEquityController {

    private final EquityDepotService equityDepotService;

    @Operation(summary = "本渠道权益列表")
    @GetMapping
    public R<PageResult<EquityDepotVO>> page(EquityDepotQueryDTO query) {
        query.setChannelCode(ContextHolder.getChannelCode());
        return R.ok(equityDepotService.page(query));
    }
}
