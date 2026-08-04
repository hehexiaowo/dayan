package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.R;
import com.dayan.system.entity.SystemDictCommon;
import com.dayan.system.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端字典查询接口。
 *
 * <p>路径 /admin-api/dicts（context-path 拼接）。
 */
@Tag(name = "字典管理")
@RestController
@RequestMapping("/dicts")
@RequiredArgsConstructor
public class SystemDictAdminController {

    private final DictService dictService;

    @Operation(summary = "按类型查询字典项")
    @SaCheckPermission("system:dict:list")
    @GetMapping("/type/{dictType}")
    public R<List<SystemDictCommon>> listByType(@PathVariable String dictType) {
        return R.ok(dictService.getByType(dictType));
    }

    @Operation(summary = "查询单个字典项")
    @SaCheckPermission("system:dict:query")
    @GetMapping("/{dictType}/{dictCode}")
    public R<SystemDictCommon> getByCode(@PathVariable String dictType, @PathVariable String dictCode) {
        return R.ok(dictService.getByCode(dictType, dictCode));
    }
}
