package com.dayan.tool.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.tool.dto.ToolAiQaConfigCreateDTO;
import com.dayan.tool.dto.ToolAiQaConfigQueryDTO;
import com.dayan.tool.dto.ToolAiQaConfigUpdateDTO;
import com.dayan.tool.service.ToolAiQaConfigService;
import com.dayan.tool.vo.ToolAiQaConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 问答人物管理")
@RestController
@RequestMapping("/tool/qa/config")
@RequiredArgsConstructor
public class ToolAiQaConfigAdminController {

    private final ToolAiQaConfigService configService;

    @Operation(summary = "人物分页列表")
    @SaCheckPermission("tool:qa:list")
    @GetMapping("/page")
    public R<PageResult<ToolAiQaConfigVO>> page(ToolAiQaConfigQueryDTO query) {
        return R.ok(configService.page(query));
    }

    @Operation(summary = "人物详情")
    @SaCheckPermission("tool:qa:query")
    @GetMapping("/{id}")
    public R<ToolAiQaConfigVO> detail(@PathVariable Long id) {
        return R.ok(configService.getDetail(id));
    }

    @Operation(summary = "新增人物")
    @OperationLog(module = "AI 问答", action = "新增人物")
    @SaCheckPermission("tool:qa:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ToolAiQaConfigCreateDTO dto) {
        return R.ok(configService.create(dto));
    }

    @Operation(summary = "修改人物")
    @OperationLog(module = "AI 问答", action = "修改人物")
    @SaCheckPermission("tool:qa:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody ToolAiQaConfigUpdateDTO dto) {
        configService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除人物")
    @OperationLog(module = "AI 问答", action = "删除人物")
    @SaCheckPermission("tool:qa:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return R.ok();
    }
}
