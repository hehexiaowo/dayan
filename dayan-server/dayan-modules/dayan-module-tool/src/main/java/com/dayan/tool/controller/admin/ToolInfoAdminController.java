package com.dayan.tool.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.tool.dto.ToolInfoCreateDTO;
import com.dayan.tool.dto.ToolInfoQueryDTO;
import com.dayan.tool.dto.ToolInfoUpdateDTO;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.vo.ToolInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端工具管理接口。
 *
 * <p>路径：{@code /tool/info/*}（context-path 拼接为 {@code /admin-api/tool/info/*}）。
 */
@Tag(name = "工具配置")
@RestController
@RequestMapping("/tool/info")
@RequiredArgsConstructor
public class ToolInfoAdminController {

    private final ToolInfoService toolInfoService;

    @Operation(summary = "工具分页列表")
    @SaCheckPermission("tool:info:list")
    @GetMapping("/page")
    public R<PageResult<ToolInfoVO>> page(ToolInfoQueryDTO query) {
        return R.ok(toolInfoService.page(query));
    }

    @Operation(summary = "工具列表（全量）")
    @SaCheckPermission("tool:info:list")
    @GetMapping("/list")
    public R<List<ToolInfoVO>> list(ToolInfoQueryDTO query) {
        return R.ok(toolInfoService.list(query));
    }

    @Operation(summary = "工具详情")
    @SaCheckPermission("tool:info:query")
    @GetMapping("/{toolCode}")
    public R<ToolInfoVO> getDetail(@PathVariable String toolCode) {
        return R.ok(toolInfoService.getDetail(toolCode));
    }

    @Operation(summary = "新增工具")
    @OperationLog(module = "工具配置", action = "新增")
    @SaCheckPermission("tool:info:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ToolInfoCreateDTO dto) {
        return R.ok(toolInfoService.create(dto));
    }

    @Operation(summary = "修改工具")
    @OperationLog(module = "工具配置", action = "修改")
    @SaCheckPermission("tool:info:update")
    @PutMapping("/{toolCode}")
    public R<Void> update(@PathVariable String toolCode, @RequestBody ToolInfoUpdateDTO dto) {
        toolInfoService.update(toolCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除工具")
    @OperationLog(module = "工具配置", action = "删除")
    @SaCheckPermission("tool:info:delete")
    @DeleteMapping("/{toolCode}")
    public R<Void> delete(@PathVariable String toolCode) {
        toolInfoService.delete(toolCode);
        return R.ok();
    }
}
