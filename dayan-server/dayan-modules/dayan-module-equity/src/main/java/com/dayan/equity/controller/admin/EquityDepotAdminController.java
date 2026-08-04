package com.dayan.equity.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.equity.dto.ActivateDTO;
import com.dayan.equity.dto.ChangeDoneDTO;
import com.dayan.equity.dto.ChangeHolderDTO;
import com.dayan.equity.dto.ChangeRollbackDTO;
import com.dayan.equity.dto.EquityDepotQueryDTO;
import com.dayan.equity.dto.OutboundDTO;
import com.dayan.equity.dto.StockInDTO;
import com.dayan.equity.dto.VoidDTO;
import com.dayan.equity.service.EquityDepotService;
import com.dayan.equity.vo.EquityDepotVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端权益卡/函接口（核心链路）。
 *
 * <p>路径前缀 {@code /equity/depot}（context-path=/admin-api 拼接为 {@code /admin-api/equity/depot/*}）。
 */
@Tag(name = "权益卡/函管理（核心链路）")
@Validated
@RestController
@RequestMapping("/equity/depot")
@RequiredArgsConstructor
public class EquityDepotAdminController {

    private final EquityDepotService equityDepotService;

    @Operation(summary = "权益分页列表")
    @GetMapping("/page")
    public R<PageResult<EquityDepotVO>> page(EquityDepotQueryDTO query) {
        return R.ok(equityDepotService.page(query));
    }

    @Operation(summary = "权益列表（全量）")
    @GetMapping("/list")
    public R<List<EquityDepotVO>> list(EquityDepotQueryDTO query) {
        return R.ok(equityDepotService.list(query));
    }

    @Operation(summary = "权益详情")
    @GetMapping("/{equityCode}")
    public R<EquityDepotVO> getDetail(@PathVariable String equityCode) {
        return R.ok(equityDepotService.getDetail(equityCode));
    }

    @Operation(summary = "批量入库（生成 N 张权益）")
    @PostMapping("/stock-in")
    public R<Integer> stockIn(@RequestBody @Valid StockInDTO dto) {
        return R.ok(equityDepotService.stockIn(dto));
    }

    @Operation(summary = "出库（批量 0→1）")
    @PostMapping("/outbound")
    public R<Integer> outbound(@RequestBody @Valid OutboundDTO dto) {
        return R.ok(equityDepotService.outbound(dto));
    }

    @Operation(summary = "激活（1→2 + 写激活记录 + 自动建默认使用人）")
    @PostMapping("/activate")
    public R<String> activate(@RequestBody @Valid ActivateDTO dto) {
        return R.ok(equityDepotService.activate(dto));
    }

    @Operation(summary = "作废（0/1→6）")
    @PostMapping("/void")
    public R<Void> voidEquity(@RequestBody @Valid VoidDTO dto) {
        equityDepotService.voidEquity(dto);
        return R.ok();
    }

    @Operation(summary = "发起更换权益人（2→7）")
    @PostMapping("/change-holder")
    public R<Long> changeHolder(@RequestBody @Valid ChangeHolderDTO dto) {
        return R.ok(equityDepotService.changeHolder(dto));
    }

    @Operation(summary = "完成更换权益人（7→2 + 切换默认使用人）")
    @PostMapping("/change-done")
    public R<Void> changeDone(@RequestBody @Valid ChangeDoneDTO dto) {
        equityDepotService.changeDone(dto);
        return R.ok();
    }

    @Operation(summary = "回滚更换权益人（7→2 + change_status=2）")
    @PostMapping("/change-rollback")
    public R<Void> changeRollback(@RequestBody @Valid ChangeRollbackDTO dto) {
        equityDepotService.changeRollback(dto);
        return R.ok();
    }

    @Operation(summary = "权益状态流转（start_service/end_service/complete/shelf_expire/expire 等）")
    @PostMapping("/transition")
    public R<Integer> transition(@RequestParam @NotBlank String equityCode,
                                 @RequestParam @NotBlank String event) {
        return R.ok(equityDepotService.transition(equityCode, event));
    }
}
