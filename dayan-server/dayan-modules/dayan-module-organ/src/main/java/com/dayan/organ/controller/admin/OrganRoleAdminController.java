package com.dayan.organ.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.organ.dto.OrganRoleCreateDTO;
import com.dayan.organ.dto.OrganRoleGrantsDTO;
import com.dayan.organ.dto.OrganRoleQueryDTO;
import com.dayan.organ.dto.OrganRoleUpdateDTO;
import com.dayan.organ.service.OrganRoleService;
import com.dayan.organ.vo.OrganRoleSimpleVO;
import com.dayan.organ.vo.OrganRoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端角色管理接口。
 *
 * <p>路径：{@code /admin-api/roles/*}（由 dayan-admin 启动模块的 context-path=/admin-api 拼接）。
 * 所有接口均由 {@code @SaCheckPermission} 鉴权，超管（DayanStpInterface 返回 ["*"]）通配通过。
 */
@Tag(name = "Admin 角色管理")
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class OrganRoleAdminController {

    private final OrganRoleService organRoleService;

    @Operation(summary = "角色分页列表")
    @SaCheckPermission("organ:role:list")
    @GetMapping
    public R<PageResult<OrganRoleSimpleVO>> page(OrganRoleQueryDTO query) {
        return R.ok(organRoleService.page(query));
    }

    @Operation(summary = "角色详情（含权限码）")
    @SaCheckPermission("organ:role:query")
    @GetMapping("/{roleCode}")
    public R<OrganRoleVO> getDetail(@PathVariable String roleCode) {
        return R.ok(organRoleService.getDetail(roleCode));
    }

    @Operation(summary = "新增角色")
    @OperationLog(module = "角色管理", action = "新增")
    @SaCheckPermission("organ:role:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid OrganRoleCreateDTO dto) {
        return R.ok(organRoleService.create(dto));
    }

    @Operation(summary = "修改角色")
    @OperationLog(module = "角色管理", action = "修改")
    @SaCheckPermission("organ:role:update")
    @PutMapping("/{roleCode}")
    public R<Void> update(@PathVariable String roleCode, @RequestBody @Valid OrganRoleUpdateDTO dto) {
        organRoleService.update(roleCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @OperationLog(module = "角色管理", action = "删除")
    @SaCheckPermission("organ:role:delete")
    @DeleteMapping("/{roleCode}")
    public R<Void> delete(@PathVariable String roleCode) {
        organRoleService.delete(roleCode);
        return R.ok();
    }

    @Operation(summary = "给角色授权（菜单+权限，全量覆盖）")
    @OperationLog(module = "角色管理", action = "授权")
    @SaCheckPermission("organ:role:assign")
    @PutMapping("/{roleCode}/permissions")
    public R<Void> assignPermissions(@PathVariable String roleCode,
                                     @RequestBody OrganRoleGrantsDTO grants) {
        organRoleService.assignGrants(roleCode, grants);
        return R.ok();
    }

    @Operation(summary = "查询角色授权（菜单码+权限码）")
    @SaCheckPermission("organ:role:query")
    @GetMapping("/{roleCode}/permissions")
    public R<OrganRoleGrantsDTO> listPermissions(@PathVariable String roleCode) {
        return R.ok(organRoleService.listGrants(roleCode));
    }
}
