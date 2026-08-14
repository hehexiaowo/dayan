package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.system.entity.SystemDictCommon;
import com.dayan.system.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端字典管理接口。
 *
 * <p>路径 /admin-api/dicts（context-path 拼接）。
 * 查询：listByType（业务消费，仅启用、走缓存）/ listAllByType（管理页，含禁用）/ getByCode。
 * 管理：类型枚举 + 字典项 CRUD（写入后失效缓存）。
 */
@Tag(name = "字典管理")
@RestController
@RequestMapping("/dicts")
@RequiredArgsConstructor
public class SystemDictAdminController {

    private final DictService dictService;

    @Operation(summary = "全部字典类型枚举")
    @SaCheckPermission("system:dict:list")
    @GetMapping("/types")
    public R<List<String>> listTypes() {
        return R.ok(dictService.listTypes());
    }

    @Operation(summary = "按类型查询字典项（仅启用，业务消费用）")
    @SaCheckPermission("system:dict:list")
    @GetMapping("/type/{dictType}")
    public R<List<SystemDictCommon>> listByType(@PathVariable String dictType) {
        return R.ok(dictService.getByType(dictType));
    }

    @Operation(summary = "按类型查询全部字典项（含禁用，管理页用）")
    @SaCheckPermission("system:dict:list")
    @GetMapping("/type/{dictType}/all")
    public R<List<SystemDictCommon>> listAllByType(@PathVariable String dictType) {
        return R.ok(dictService.listAllByType(dictType));
    }

    @Operation(summary = "查询单个字典项")
    @SaCheckPermission("system:dict:query")
    @GetMapping("/{dictType}/{dictCode}")
    public R<SystemDictCommon> getByCode(@PathVariable String dictType, @PathVariable String dictCode) {
        return R.ok(dictService.getByCode(dictType, dictCode));
    }

    @Operation(summary = "新增字典项")
    @OperationLog(module = "字典管理", action = "新增")
    @SaCheckPermission("system:dict:create")
    @PostMapping
    public R<Long> create(@RequestBody SystemDictCommon dict) {
        return R.ok(dictService.create(dict));
    }

    @Operation(summary = "修改字典项")
    @OperationLog(module = "字典管理", action = "修改")
    @SaCheckPermission("system:dict:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SystemDictCommon dict) {
        dictService.update(id, dict);
        return R.ok();
    }

    @Operation(summary = "删除字典项")
    @OperationLog(module = "字典管理", action = "删除")
    @SaCheckPermission("system:dict:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        dictService.delete(id);
        return R.ok();
    }
}
