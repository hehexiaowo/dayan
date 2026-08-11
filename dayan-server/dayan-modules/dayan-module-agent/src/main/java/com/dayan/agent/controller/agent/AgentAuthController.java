package com.dayan.agent.controller.agent;

import com.dayan.agent.dto.AgentLoginDTO;
import com.dayan.agent.dto.SmsLoginDTO;
import com.dayan.agent.dto.SmsSendDTO;
import com.dayan.agent.dto.WxLoginDTO;
import com.dayan.agent.service.AgentAuthService;
import com.dayan.agent.service.AgentSmsCodeService;
import com.dayan.agent.vo.AgentLoginVO;
import com.dayan.agent.vo.ChannelOptionVO;
import com.dayan.agent.vo.SmsSendVO;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 代理人端认证接口。
 *
 * <p>路径：{@code /agent-api/auth/*}（由 dayan-agent 启动模块的 context-path 拼接）。
 *
 * <p>支持三种登录方式：
 * <ul>
 *   <li>密码登录：{@code POST /auth/login}</li>
 *   <li>验证码登录：{@code POST /auth/sms/send} + {@code POST /auth/sms/login}</li>
 *   <li>微信登录：{@code POST /auth/wx/login}</li>
 * </ul>
 */
@Tag(name = "Agent 认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AgentAuthController {

    private final AgentAuthService agentAuthService;
    private final AgentSmsCodeService smsCodeService;

    @Operation(summary = "选渠道列表（按手机号/用户名/OpenID 检索关联渠道）")
    @GetMapping("/channels")
    public R<List<ChannelOptionVO>> channels(@RequestParam(required = false) String mobile,
                                             @RequestParam(required = false) String openId) {
        return R.ok(agentAuthService.listChannels(mobile, openId));
    }

    // ==================== 密码登录 ====================

    @Operation(summary = "密码登录")
    @OperationLog(module = "认证", action = "登录", maskFields = "password")
    @PostMapping("/login")
    public R<AgentLoginVO> login(@RequestBody @Valid AgentLoginDTO dto) {
        return R.ok(agentAuthService.login(dto));
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
    public R<AgentLoginVO> smsLogin(@RequestBody @Valid SmsLoginDTO dto) {
        return R.ok(agentAuthService.smsLogin(dto));
    }

    // ==================== 微信登录 ====================

    @Operation(summary = "微信授权登录")
    @OperationLog(module = "认证", action = "微信登录")
    @PostMapping("/wx/login")
    public R<AgentLoginVO> wxLogin(@RequestBody @Valid WxLoginDTO dto) {
        return R.ok(agentAuthService.wxLogin(dto.getCode(), dto.getChannelCode()));
    }

    // ==================== 通用 ====================

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        agentAuthService.logout();
        return R.ok();
    }

    @Operation(summary = "当前登录人信息")
    @GetMapping("/info")
    public R<AgentLoginVO> info() {
        return R.ok(agentAuthService.current());
    }
}
