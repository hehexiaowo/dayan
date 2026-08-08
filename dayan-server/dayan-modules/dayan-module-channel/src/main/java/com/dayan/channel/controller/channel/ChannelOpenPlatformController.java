package com.dayan.channel.controller.channel;

import com.dayan.channel.dto.ChannelOpenPlatformQueryDTO;
import com.dayan.channel.service.ChannelOpenPlatformService;
import com.dayan.channel.vo.ChannelOpenPlatformVO;
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
 * Channel 渠道端开放平台配置接口（只读）。
 *
 * <p>路径：{@code /channel-api/open-platforms}。
 * 渠道运营查看本渠道的对接配置（appKey/appSecret 脱敏/回调/IP 白名单等），
 * 不支持自行修改（配置由运营在 admin 端操作）。
 *
 * <p>防越权：channelCode 从 {@link ContextHolder} 强制注入，getDetail 校验归属。
 * appSecret 脱敏由 {@link com.dayan.channel.service.impl.ChannelOpenPlatformServiceImpl} 处理（查询返回 ***）。
 */
@Tag(name = "Channel 开放平台")
@RestController
@RequestMapping("/open-platforms")
@RequiredArgsConstructor
public class ChannelOpenPlatformController {

    private final ChannelOpenPlatformService channelOpenPlatformService;

    @Operation(summary = "本渠道对接配置")
    @SaCheckPermission("channel:openApp:list")
    @GetMapping
    public R<PageResult<ChannelOpenPlatformVO>> page(ChannelOpenPlatformQueryDTO query) {
        query.setChannelCode(ContextHolder.getChannelCode());
        return R.ok(channelOpenPlatformService.page(query));
    }

    @Operation(summary = "对接配置详情")
    @SaCheckPermission("channel:openApp:list")
    @GetMapping("/{id}")
    public R<ChannelOpenPlatformVO> getDetail(@PathVariable Long id) {
        ChannelOpenPlatformVO vo = channelOpenPlatformService.getDetail(id);
        if (vo == null || !ContextHolder.getChannelCode().equals(vo.getChannelCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置不存在或无权访问");
        }
        return R.ok(vo);
    }
}
