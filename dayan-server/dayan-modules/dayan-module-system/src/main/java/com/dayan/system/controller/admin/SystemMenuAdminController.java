package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.R;
import com.dayan.system.entity.SystemMenu;
import com.dayan.system.service.SystemMenuService;
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

    @Operation(summary = "菜单树")
    @SaCheckPermission("system:menu:list")
    @GetMapping("/tree")
    public R<List<SystemMenu>> tree(@RequestParam(required = false) String domainType) {
        return R.ok(menuService.tree(domainType));
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
