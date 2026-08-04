package com.dayan.organ.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.organ.service.OrganAccountRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端账号-角色关联接口。
 *
 * <p>路径：{@code /admin-api/account-roles/*}。
 */
@Tag(name = "Admin 账号角色")
@RestController
@RequestMapping("/account-roles")
@RequiredArgsConstructor
public class OrganAccountRoleAdminController {

    private final OrganAccountRoleService organAccountRoleService;

    @Operation(summary = "给账号分配角色（全量覆盖）")
    @OperationLog(module = "账号角色", action = "分配")
    @SaCheckPermission("organ:account:assign")
    @PutMapping("/{accountCode}/roles")
    public R<Void> assignRoles(@PathVariable String accountCode,
                               @RequestBody List<String> roleCodes) {
        organAccountRoleService.assignRoles(accountCode, roleCodes);
        return R.ok();
    }

    @Operation(summary = "查询账号的角色编码列表")
    @SaCheckPermission("organ:account:assign")
    @GetMapping("/{accountCode}/roles")
    public R<List<String>> listRoles(@PathVariable String accountCode) {
        return R.ok(organAccountRoleService.listRoles(accountCode));
    }
}
