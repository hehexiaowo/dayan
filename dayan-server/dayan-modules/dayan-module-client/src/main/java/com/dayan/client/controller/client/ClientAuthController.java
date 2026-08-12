package com.dayan.client.controller.client;

import com.dayan.client.dto.ClientLoginDTO;
import com.dayan.client.dto.SmsLoginDTO;
import com.dayan.client.dto.SmsSendDTO;
import com.dayan.client.dto.WxLoginDTO;
import com.dayan.client.service.ClientAuthService;
import com.dayan.client.service.ClientSmsCodeService;
import com.dayan.client.vo.ChannelOptionVO;
import com.dayan.client.vo.ClientLoginVO;
import com.dayan.client.vo.SmsSendVO;
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
 * <p>支持三种登录方式：
 * <ul>
 *   <li>密码登录：{@code POST /auth/login}</li>
 *   <li>验证码登录：{@code POST /auth/sms/send} + {@code POST /auth/sms/login}</li>
 *   <li>微信登录：{@code POST /auth/wx/login}</li>
 * </ul>
 */
@Tag(name = "Client 认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ClientAuthController {

    private final ClientAuthService clientAuthService;
    private final ClientSmsCodeService smsCodeService;

    @Operation(summary = "选渠道列表（按手机号/OpenID 检索关联渠道）")
    @GetMapping("/channels")
    public R<List<ChannelOptionVO>> channels(@RequestParam(required = false) String mobile,
                                             @RequestParam(required = false) String openId) {
        return R.ok(clientAuthService.listChannels(mobile, openId));
    }

    // ==================== 密码登录 ====================

    @Operation(summary = "密码登录")
    @OperationLog(module = "认证", action = "登录", logArgs = false)
    @PostMapping("/login")
    public R<ClientLoginVO> login(@RequestBody @Valid ClientLoginDTO dto) {
        return R.ok(clientAuthService.login(dto));
    }

    // ==================== 验证码登录 ====================

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms/send")
    public R<SmsSendVO> sendSmsCode(@RequestBody @Valid SmsSendDTO dto) {
        return R.ok(smsCodeService.sendCode(dto.getMobile(), dto.getChannelCode()));
    }

    @Operation(summary = "验证码登录")
    @OperationLog(module = "认证", action = "验证码登录")
    @PostMapping("/sms/login")
    public R<ClientLoginVO> smsLogin(@RequestBody @Valid SmsLoginDTO dto) {
        return R.ok(clientAuthService.smsLogin(dto));
    }

    // ==================== 微信登录 ====================

    @Operation(summary = "微信授权登录")
    @OperationLog(module = "认证", action = "微信登录")
    @PostMapping("/wx/login")
    public R<ClientLoginVO> wxLogin(@RequestBody @Valid WxLoginDTO dto) {
        return R.ok(clientAuthService.wxLogin(dto.getCode(), dto.getChannelCode()));
    }

    // ==================== 通用 ====================

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
