package com.dayan.client.controller.client;

import com.dayan.client.dto.ClientProfileUpdateDTO;
import com.dayan.client.service.ClientProfileService;
import com.dayan.client.vo.ClientProfileVO;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Client 端个人资料接口（查看 / 编辑）。
 *
 * <p>路径：{@code /client-api/profile/*}。所有操作仅作用于当前登录客户（clientCode 取自登录态，防越权）。
 */
@Tag(name = "Client 个人资料")
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ClientProfileController {

    private final ClientProfileService clientProfileService;

    @Operation(summary = "我的资料")
    @GetMapping
    public R<ClientProfileVO> get() {
        return R.ok(clientProfileService.getProfile());
    }

    @Operation(summary = "更新基础资料")
    @PutMapping
    public R<Void> update(@RequestBody @Valid ClientProfileUpdateDTO dto) {
        clientProfileService.updateProfile(dto);
        return R.ok();
    }
}
