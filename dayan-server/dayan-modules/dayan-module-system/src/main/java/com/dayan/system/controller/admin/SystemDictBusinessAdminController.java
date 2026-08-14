package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.system.entity.SystemDictBusiness;
import com.dayan.system.service.SystemDictBusinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端业务字典管理接口。
 *
 * <p>路径 /admin-api/dicts-business（context-path 拼接）。
 * 业务字典按 domain（业务域）组织，供各域维护专属字典项。
 */
@Tag(name = "业务字典管理")
@RestController
@RequestMapping("/dicts-business")
@RequiredArgsConstructor
public class SystemDictBusinessAdminController {

    private final SystemDictBusinessService service;

    @Operation(summary = "业务字典分页列表")
    @SaCheckPermission("system:dict-biz:list")
    @GetMapping
    public R<PageResult<SystemDictBusiness>> page(
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String domain,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(service.page(current, size, dictType, domain));
    }

    @Operation(summary = "新增业务字典项")
    @OperationLog(module = "业务字典", action = "新增")
    @SaCheckPermission("system:dict-biz:create")
    @PostMapping
    public R<Long> create(@RequestBody SystemDictBusiness dict) {
        return R.ok(service.create(dict));
    }

    @Operation(summary = "修改业务字典项")
    @OperationLog(module = "业务字典", action = "修改")
    @SaCheckPermission("system:dict-biz:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SystemDictBusiness dict) {
        service.update(id, dict);
        return R.ok();
    }

    @Operation(summary = "删除业务字典项")
    @OperationLog(module = "业务字典", action = "删除")
    @SaCheckPermission("system:dict-biz:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}
