package com.dayan.finance.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.finance.dto.AccountReceiveDTO;
import com.dayan.finance.dto.CreateAccountDTO;
import com.dayan.finance.dto.FinanceAccountQueryDTO;
import com.dayan.finance.service.FinanceAccountService;
import com.dayan.finance.vo.FinanceAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端应收应付账目接口。
 *
 * <p>路径前缀 {@code /finance/account}。
 */
@Tag(name = "应收应付账目管理")
@RestController
@RequestMapping("/finance/account")
@RequiredArgsConstructor
public class FinanceAccountAdminController {

    private final FinanceAccountService financeAccountService;

    @Operation(summary = "账目分页列表")
    @GetMapping("/page")
    public R<PageResult<FinanceAccountVO>> page(FinanceAccountQueryDTO query) {
        return R.ok(financeAccountService.page(query));
    }

    @Operation(summary = "账目列表（全量）")
    @GetMapping("/list")
    public R<List<FinanceAccountVO>> list(FinanceAccountQueryDTO query) {
        return R.ok(financeAccountService.list(query));
    }

    @Operation(summary = "账目详情")
    @GetMapping("/{accountCode}")
    public R<FinanceAccountVO> getDetail(@PathVariable String accountCode) {
        return R.ok(financeAccountService.getDetail(accountCode));
    }

    @Operation(summary = "创建账目")
    @PostMapping
    public R<String> create(@RequestBody @Valid CreateAccountDTO dto) {
        return R.ok(financeAccountService.create(dto));
    }

    @Operation(summary = "账目收/付款（推进 account_status）")
    @PostMapping("/receive")
    public R<Void> receive(@RequestBody @Valid AccountReceiveDTO dto) {
        financeAccountService.receive(dto);
        return R.ok();
    }
}
