package com.dayan.equity.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.equity.dto.EquityChangeHolderQueryDTO;
import com.dayan.equity.service.EquityChangeHolderService;
import com.dayan.equity.vo.EquityChangeHolderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端权益更换权益人记录接口（仅查询；发起/完成/回滚在 {@code /equity/depot} 下）。
 *
 * <p>路径前缀 {@code /equity/change-holder}（context-path=/admin-api 拼接为 {@code /admin-api/equity/change-holder/*}）。
 */
@Tag(name = "权益更换权益人记录")
@RestController
@RequestMapping("/equity/change-holder")
@RequiredArgsConstructor
public class EquityChangeHolderAdminController {

    private final EquityChangeHolderService equityChangeHolderService;

    @Operation(summary = "更换记录分页列表")
    @GetMapping("/page")
    public R<PageResult<EquityChangeHolderVO>> page(EquityChangeHolderQueryDTO query) {
        return R.ok(equityChangeHolderService.page(query));
    }

    @Operation(summary = "按权益编码列出更换历史")
    @GetMapping("/list-by-equity/{equityCode}")
    public R<List<EquityChangeHolderVO>> listByEquity(@PathVariable String equityCode) {
        return R.ok(equityChangeHolderService.listByEquity(equityCode));
    }

    @Operation(summary = "更换记录详情")
    @GetMapping("/{id}")
    public R<EquityChangeHolderVO> getDetail(@PathVariable Long id) {
        return R.ok(equityChangeHolderService.getDetail(id));
    }
}
