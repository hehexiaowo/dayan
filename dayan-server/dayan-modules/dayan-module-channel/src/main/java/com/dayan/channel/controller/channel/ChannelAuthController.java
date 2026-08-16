package com.dayan.channel.controller.channel;

import com.dayan.channel.dto.AuthLoginDTO;
import com.dayan.channel.service.ChannelAuthService;
import com.dayan.channel.vo.AuthLoginVO;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Channel 渠道端认证接口。
 *
 * <p>路径：{@code /channel-api/auth/*}（由 dayan-channel 启动模块的 context-path 拼接）。
 */
@Tag(name = "Channel 认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ChannelAuthController {

    private final ChannelAuthService channelAuthService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<AuthLoginVO> login(@RequestBody @Valid AuthLoginDTO dto) {
        return R.ok(channelAuthService.login(dto));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        channelAuthService.logout();
        return R.ok();
    }

    @Operation(summary = "当前登录人信息")
    @GetMapping("/info")
    public R<AuthLoginVO> info() {
        return R.ok(channelAuthService.current());
    }
}
