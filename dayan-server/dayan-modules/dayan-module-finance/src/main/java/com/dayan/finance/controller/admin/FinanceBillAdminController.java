package com.dayan.finance.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.finance.dto.BillAuditDTO;
import com.dayan.finance.dto.BillFinishSettleDTO;
import com.dayan.finance.dto.FinanceBillQueryDTO;
import com.dayan.finance.dto.GenerateBillDTO;
import com.dayan.finance.service.FinanceBillService;
import com.dayan.finance.vo.FinanceBillVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端结算单接口。
 *
 * <p>路径前缀 {@code /finance/bill}。
 */
@Tag(name = "结算单管理")
@RestController
@RequestMapping("/finance/bill")
@RequiredArgsConstructor
public class FinanceBillAdminController {

    private final FinanceBillService financeBillService;

    @Operation(summary = "结算单分页列表")
    @GetMapping("/page")
    public R<PageResult<FinanceBillVO>> page(FinanceBillQueryDTO query) {
        return R.ok(financeBillService.page(query));
    }

    @Operation(summary = "结算单列表（全量）")
    @GetMapping("/list")
    public R<List<FinanceBillVO>> list(FinanceBillQueryDTO query) {
        return R.ok(financeBillService.list(query));
    }

    @Operation(summary = "结算单详情")
    @GetMapping("/{billCode}")
    public R<FinanceBillVO> getDetail(@PathVariable String billCode) {
        return R.ok(financeBillService.getDetail(billCode));
    }

    @Operation(summary = "生成结算单")
    @PostMapping("/generate")
    public R<String> generate(@RequestBody @Valid GenerateBillDTO dto) {
        return R.ok(financeBillService.generate(dto));
    }

    @Operation(summary = "审核结算单（0→1 通过 / 0→4 拒绝）")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody @Valid BillAuditDTO dto) {
        financeBillService.audit(dto);
        return R.ok();
    }

    @Operation(summary = "开始结算（1→2）")
    @PostMapping("/start-settle/{billCode}")
    public R<Void> startSettle(@PathVariable String billCode) {
        financeBillService.startSettle(billCode);
        return R.ok();
    }

    @Operation(summary = "完成结算（2→3）")
    @PostMapping("/finish-settle")
    public R<Void> finishSettle(@RequestBody @Valid BillFinishSettleDTO dto) {
        financeBillService.finishSettle(dto);
        return R.ok();
    }
}
