package com.dayan.client.controller.channel;

import com.dayan.client.dto.ClientAccountQueryDTO;
import com.dayan.client.service.ClientAccountService;
import com.dayan.client.vo.ClientAccountVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Channel 渠道端客户账号接口。
 *
 * <p>路径：{@code /channel-api/client-accounts}。
 * 防越权：client_account 表有 channel_code 列，channelCode 注入隔离。
 *
 * <p>注：ClientAccountService 无公开 getDetail 方法（selectByCode 为 private），
 * 本期只提供 page 列表；详情页后续按需补（可加 Service.getDetail 或用 page 反查）。
 */
@Tag(name = "Channel 客户账号")
@RestController
@RequestMapping("/client-accounts")
@RequiredArgsConstructor
public class ChannelClientAccountController {

    private final ClientAccountService clientAccountService;

    @Operation(summary = "本渠道客户账号列表")
    @SaCheckPermission("channel:client:list")
    @GetMapping
    public R<PageResult<ClientAccountVO>> page(ClientAccountQueryDTO query) {
        query.setChannelCode(ContextHolder.getChannelCode());
        return R.ok(clientAccountService.page(query));
    }
}
