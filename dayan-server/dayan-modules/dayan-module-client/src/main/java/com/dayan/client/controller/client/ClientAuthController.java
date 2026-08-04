package com.dayan.client.controller.client;

import com.dayan.client.dto.ClientLoginDTO;
import com.dayan.client.service.ClientAuthService;
import com.dayan.client.vo.ChannelOptionVO;
import com.dayan.client.vo.ClientLoginVO;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Client 客户端认证接口。
 *
 * <p>路径：{@code /client-api/auth/*}（由 dayan-client 启动模块的 context-path 拼接）。
 *
 * <p>支持"选渠道"流程：先 {@code GET /auth/channels} 检索关联渠道，再 {@code POST /auth/login} 选定渠道登录。
 */
@Tag(name = "Client 认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ClientAuthController {

    private final ClientAuthService clientAuthService;

    @Operation(summary = "选渠道列表（按手机号/OpenID 检索关联渠道）")
    @GetMapping("/channels")
    public R<List<ChannelOptionVO>> channels(@RequestParam(required = false) String mobile,
                                             @RequestParam(required = false) String openId) {
        return R.ok(clientAuthService.listChannels(mobile, openId));
    }

    @Operation(summary = "登录")
    @OperationLog(module = "认证", action = "登录", logArgs = false)
    @PostMapping("/login")
    public R<ClientLoginVO> login(@RequestBody @Valid ClientLoginDTO dto) {
        return R.ok(clientAuthService.login(dto));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        clientAuthService.logout();
        return R.ok();
    }

    @Operation(summary = "当前登录人信息")
    @GetMapping("/info")
    public R<ClientLoginVO> info() {
        return R.ok(clientAuthService.current());
    }
}
