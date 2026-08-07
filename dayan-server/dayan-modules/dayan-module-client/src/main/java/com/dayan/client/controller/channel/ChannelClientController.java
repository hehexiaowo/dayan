package com.dayan.client.controller.channel;

import com.dayan.client.dto.ClientInfoQueryDTO;
import com.dayan.client.service.ClientInfoService;
import com.dayan.client.vo.ClientInfoVO;
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
 * Channel 渠道端客户接口。
 *
 * <p>路径：{@code /channel-api/clients}（由 dayan-channel 启动模块的 context-path 拼接）。
 *
 * <p>防越权：channelCode 从 {@link ContextHolder} 强制注入，不接受前端传入。
 */
@Tag(name = "Channel 客户")
@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ChannelClientController {

    private final ClientInfoService clientInfoService;

    @Operation(summary = "本渠道客户列表")
    @GetMapping
    public R<PageResult<ClientInfoVO>> page(ClientInfoQueryDTO query) {
        query.setChannelCode(ContextHolder.getChannelCode());
        return R.ok(clientInfoService.page(query));
    }
}
