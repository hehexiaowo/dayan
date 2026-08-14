package com.dayan.equity.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.equity.dto.EquityBatchCreateDTO;
import com.dayan.equity.dto.EquityBatchQueryDTO;
import com.dayan.equity.dto.EquityBatchUpdateDTO;
import com.dayan.equity.service.EquityBatchService;
import com.dayan.equity.vo.EquityBatchVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端权益批次接口。
 *
 * <p>路径前缀 {@code /equity/batch}（context-path=/admin-api 拼接为 {@code /admin-api/equity/batch/*}）。
 */
@Tag(name = "权益批次管理")
@RestController
@RequestMapping("/equity/batch")
@RequiredArgsConstructor
public class EquityBatchAdminController {

    private final EquityBatchService equityBatchService;

    @Operation(summary = "权益批次分页列表")
    @SaCheckPermission("equity:batch:list")
    @GetMapping("/page")
    public R<PageResult<EquityBatchVO>> page(EquityBatchQueryDTO query) {
        return R.ok(equityBatchService.page(query));
    }

    @Operation(summary = "权益批次列表（全量）")
    @SaCheckPermission("equity:batch:list")
    @GetMapping("/list")
    public R<List<EquityBatchVO>> list(EquityBatchQueryDTO query) {
        return R.ok(equityBatchService.list(query));
    }

    @Operation(summary = "权益批次详情（含统计字段）")
    @SaCheckPermission("equity:batch:query")
    @GetMapping("/{batchCode}")
    public R<EquityBatchVO> getDetail(@PathVariable String batchCode) {
        return R.ok(equityBatchService.getDetail(batchCode));
    }

    @Operation(summary = "查询批次统计字段（与 /{batchCode} 等价，语义化别名）")
    @SaCheckPermission("equity:batch:stats")
    @GetMapping("/stats/{batchCode}")
    public R<EquityBatchVO> stats(@PathVariable String batchCode) {
        return R.ok(equityBatchService.getDetail(batchCode));
    }

    @Operation(summary = "新增权益批次")
    @OperationLog(module = "权益批次", action = "新增")
    @SaCheckPermission("equity:batch:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid EquityBatchCreateDTO dto) {
        return R.ok(equityBatchService.create(dto));
    }

    @Operation(summary = "修改权益批次")
    @OperationLog(module = "权益批次", action = "修改")
    @SaCheckPermission("equity:batch:update")
    @PutMapping("/{batchCode}")
    public R<Void> update(@PathVariable String batchCode,
                          @RequestBody EquityBatchUpdateDTO dto) {
        equityBatchService.update(batchCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除权益批次（仅未生产）")
    @OperationLog(module = "权益批次", action = "删除")
    @SaCheckPermission("equity:batch:delete")
    @DeleteMapping("/{batchCode}")
    public R<Void> delete(@PathVariable String batchCode) {
        equityBatchService.delete(batchCode);
        return R.ok();
    }
}
