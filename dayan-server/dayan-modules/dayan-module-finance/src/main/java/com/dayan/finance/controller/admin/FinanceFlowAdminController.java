package com.dayan.finance.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.finance.dto.FinanceFlowQueryDTO;
import com.dayan.finance.dto.RecordFlowDTO;
import com.dayan.finance.service.FinanceFlowService;
import com.dayan.finance.vo.FinanceFlowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端财务流水接口。
 *
 * <p>路径前缀 {@code /finance/flow}（context-path=/admin-api 拼接为 {@code /admin-api/finance/flow/*}）。
 */
@Tag(name = "财务流水管理")
@RestController
@RequestMapping("/finance/flow")
@RequiredArgsConstructor
public class FinanceFlowAdminController {

    private final FinanceFlowService financeFlowService;

    @Operation(summary = "财务流水分页列表")
    @GetMapping("/page")
    public R<PageResult<FinanceFlowVO>> page(FinanceFlowQueryDTO query) {
        return R.ok(financeFlowService.page(query));
    }

    @Operation(summary = "财务流水列表（全量）")
    @GetMapping("/list")
    public R<List<FinanceFlowVO>> list(FinanceFlowQueryDTO query) {
        return R.ok(financeFlowService.list(query));
    }

    @Operation(summary = "财务流水详情")
    @GetMapping("/{flowCode}")
    public R<FinanceFlowVO> getDetail(@PathVariable String flowCode) {
        return R.ok(financeFlowService.getDetail(flowCode));
    }

    @Operation(summary = "记录财务流水")
    @PostMapping("/record")
    public R<String> record(@RequestBody @Valid RecordFlowDTO dto) {
        return R.ok(financeFlowService.record(dto));
    }
}
