package com.dayan.agent.controller.agent;

import com.dayan.agent.dto.AgentPhoneChangeDTO;
import com.dayan.agent.dto.AgentPhoneSendDTO;
import com.dayan.agent.dto.AgentProfileUpdateDTO;
import com.dayan.agent.service.AgentProfileService;
import com.dayan.agent.vo.AgentProfileVO;
import com.dayan.agent.vo.SmsSendVO;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 代理人端个人资料接口。
 *
 * <p>路径：{@code /agent-api/profile/*}。所有操作仅作用于当前登录代理人。
 */
@Tag(name = "Agent 个人资料")
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class AgentProfileController {

    private final AgentProfileService agentProfileService;

    @Operation(summary = "我的资料")
    @GetMapping
    public R<AgentProfileVO> get() {
        return R.ok(agentProfileService.getProfile());
    }

    @Operation(summary = "更新基础资料")
    @PutMapping
    public R<Void> update(@RequestBody @Valid AgentProfileUpdateDTO dto) {
        agentProfileService.updateProfile(dto);
        return R.ok();
    }

    @Operation(summary = "换绑手机号-发送验证码")
    @PostMapping("/phone/send")
    public R<SmsSendVO> sendPhoneCode(@RequestBody @Valid AgentPhoneSendDTO dto) {
        return R.ok(agentProfileService.sendPhoneChangeCode(dto.getMobile()));
    }

    @Operation(summary = "换绑手机号")
    @PostMapping("/phone/change")
    public R<Void> changePhone(@RequestBody @Valid AgentPhoneChangeDTO dto) {
        agentProfileService.changePhone(dto.getMobile(), dto.getCode());
        return R.ok();
    }
}
