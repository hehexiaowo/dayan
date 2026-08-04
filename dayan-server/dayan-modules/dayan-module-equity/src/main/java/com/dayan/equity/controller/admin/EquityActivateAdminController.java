package com.dayan.equity.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.equity.dto.EquityActivateQueryDTO;
import com.dayan.equity.service.EquityActivateService;
import com.dayan.equity.vo.EquityActivateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端权益激活记录接口（仅查询，记录由 depot.activate 自动产生）。
 *
 * <p>路径前缀 {@code /equity/activate}（context-path=/admin-api 拼接为 {@code /admin-api/equity/activate/*}）。
 */
@Tag(name = "权益激活记录")
@RestController
@RequestMapping("/equity/activate")
@RequiredArgsConstructor
public class EquityActivateAdminController {

    private final EquityActivateService equityActivateService;

    @Operation(summary = "激活记录分页列表")
    @GetMapping("/page")
    public R<PageResult<EquityActivateVO>> page(EquityActivateQueryDTO query) {
        return R.ok(equityActivateService.page(query));
    }

    @Operation(summary = "激活记录列表（全量）")
    @GetMapping("/list")
    public R<List<EquityActivateVO>> list(EquityActivateQueryDTO query) {
        return R.ok(equityActivateService.list(query));
    }

    @Operation(summary = "按权益编码查激活记录")
    @GetMapping("/{equityCode}")
    public R<EquityActivateVO> getByEquityCode(@PathVariable String equityCode) {
        return R.ok(equityActivateService.getByEquityCode(equityCode));
    }
}
