package com.dayan.organ.controller.admin;

import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.organ.dto.AuthLoginDTO;
import com.dayan.organ.service.AdminAuthService;
import com.dayan.organ.vo.AuthLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 运营端认证接口。
 *
 * <p>路径：{@code /admin-api/auth/*}（由 dayan-admin 启动模块的 context-path 拼接）。
 */
@Tag(name = "Admin 认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "登录")
    @OperationLog(module = "认证", action = "登录", maskFields = "password")
    @PostMapping("/login")
    public R<AuthLoginVO> login(@RequestBody @Valid AuthLoginDTO dto) {
        return R.ok(adminAuthService.login(dto));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        adminAuthService.logout();
        return R.ok();
    }

    @Operation(summary = "当前登录人信息")
    @GetMapping("/info")
    public R<AuthLoginVO> info() {
        return R.ok(adminAuthService.current());
    }
}
