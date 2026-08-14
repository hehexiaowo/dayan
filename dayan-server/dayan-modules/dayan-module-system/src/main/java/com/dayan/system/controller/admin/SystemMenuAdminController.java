package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.R;
import com.dayan.common.security.StpKit;
import com.dayan.organ.security.DayanStpInterface;
import com.dayan.system.entity.SystemMenu;
import com.dayan.system.service.SystemMenuService;
import com.dayan.system.vo.MenuGrantTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端菜单管理接口。
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class SystemMenuAdminController {

    private final SystemMenuService menuService;

    @Operation(summary = "菜单列表（可按端过滤）")
    @SaCheckPermission("system:menu:list")
    @GetMapping
    public R<List<SystemMenu>> list(@RequestParam(required = false) String domainType) {
        return R.ok(menuService.listAll(domainType));
    }

    @Operation(summary = "当前账号可见菜单（RBAC 动态路由，组装树）")
    @GetMapping("/mine")
    public R<List<SystemMenu>> mine(@RequestParam(required = false) String domainType) {
        String accountCode = StpKit.ADMIN.getLoginIdAsString();
        boolean isAdmin = StpKit.ADMIN.getRoleList().contains(DayanStpInterface.ROLE_SUPER_ADMIN);
        return R.ok(menuService.treeByRole(domainType, isAdmin, accountCode));
    }

    @Operation(summary = "当前账号按钮级权限码（前端 v-permission 指令消费；超管返回 [\"*\"]）")
    @GetMapping("/mine/permissions")
    public R<List<String>> myPermissions() {
        return R.ok(StpKit.ADMIN.getPermissionList());
    }

    @Operation(summary = "菜单树")
    @SaCheckPermission("system:menu:list")
    @GetMapping("/tree")
    public R<List<SystemMenu>> tree(@RequestParam(required = false) String domainType) {
        return R.ok(menuService.tree(domainType));
    }

    @Operation(summary = "角色授权树（目录→菜单→操作权限）")
    @SaCheckPermission("organ:role:assign")
    @GetMapping("/grant-tree")
    public R<List<MenuGrantTreeVO>> grantTree() {
        return R.ok(menuService.grantTree());
    }

    @Operation(summary = "新增菜单")
    @SaCheckPermission("system:menu:create")
    @PostMapping
    public R<String> create(@RequestBody SystemMenu menu) {
        return R.ok(menuService.create(menu));
    }

    @Operation(summary = "修改菜单")
    @SaCheckPermission("system:menu:update")
    @PutMapping("/{menuCode}")
    public R<Void> update(@PathVariable String menuCode, @RequestBody SystemMenu menu) {
        menuService.update(menuCode, menu);
        return R.ok();
    }

    @Operation(summary = "删除菜单")
    @SaCheckPermission("system:menu:delete")
    @DeleteMapping("/{menuCode}")
    public R<Void> delete(@PathVariable String menuCode) {
        menuService.delete(menuCode);
        return R.ok();
    }
}
