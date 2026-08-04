package com.dayan.organ.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.organ.dto.OrganPermissionCreateDTO;
import com.dayan.organ.dto.OrganPermissionQueryDTO;
import com.dayan.organ.dto.OrganPermissionUpdateDTO;
import com.dayan.organ.service.OrganPermissionService;
import com.dayan.organ.vo.OrganPermissionTreeVO;
import com.dayan.organ.vo.OrganPermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端权限项管理接口。
 *
 * <p>路径：{@code /admin-api/permissions/*}。
 */
@Tag(name = "Admin 权限管理")
@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class OrganPermissionAdminController {

    private final OrganPermissionService organPermissionService;

    @Operation(summary = "权限分页列表")
    @SaCheckPermission("organ:permission:list")
    @GetMapping
    public R<PageResult<OrganPermissionVO>> page(OrganPermissionQueryDTO query) {
        return R.ok(organPermissionService.page(query));
    }

    @Operation(summary = "全部启用权限（授权选择用）")
    @SaCheckPermission("organ:permission:query")
    @GetMapping("/all")
    public R<List<OrganPermissionVO>> listAll() {
        return R.ok(organPermissionService.listAll());
    }

    @Operation(summary = "权限树（按类型分组，授权勾选用）")
    @SaCheckPermission("organ:permission:query")
    @GetMapping("/tree")
    public R<List<OrganPermissionTreeVO>> tree() {
        return R.ok(organPermissionService.tree());
    }

    @Operation(summary = "新增权限")
    @OperationLog(module = "权限管理", action = "新增")
    @SaCheckPermission("organ:permission:create")
    @PostMapping
    public R<Void> create(@RequestBody @Valid OrganPermissionCreateDTO dto) {
        organPermissionService.create(dto);
        return R.ok();
    }

    @Operation(summary = "修改权限")
    @OperationLog(module = "权限管理", action = "修改")
    @SaCheckPermission("organ:permission:update")
    @PutMapping("/{permissionCode}")
    public R<Void> update(@PathVariable String permissionCode,
                          @RequestBody @Valid OrganPermissionUpdateDTO dto) {
        organPermissionService.update(permissionCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除权限")
    @OperationLog(module = "权限管理", action = "删除")
    @SaCheckPermission("organ:permission:delete")
    @DeleteMapping("/{permissionCode}")
    public R<Void> delete(@PathVariable String permissionCode) {
        organPermissionService.delete(permissionCode);
        return R.ok();
    }
}
