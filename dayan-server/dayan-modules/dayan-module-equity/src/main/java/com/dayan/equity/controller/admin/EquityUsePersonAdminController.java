package com.dayan.equity.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.equity.dto.EquityUsePersonCreateDTO;
import com.dayan.equity.dto.EquityUsePersonQueryDTO;
import com.dayan.equity.dto.EquityUsePersonUpdateDTO;
import com.dayan.equity.dto.SetDefaultHolderDTO;
import com.dayan.equity.service.EquityUsePersonService;
import com.dayan.equity.vo.EquityUsePersonVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端权益使用人接口。
 *
 * <p>路径前缀 {@code /equity/use-person}（context-path=/admin-api 拼接为 {@code /admin-api/equity/use-person/*}）。
 */
@Tag(name = "权益使用人管理")
@RestController
@RequestMapping("/equity/use-person")
@RequiredArgsConstructor
public class EquityUsePersonAdminController {

    private final EquityUsePersonService equityUsePersonService;

    @Operation(summary = "使用人分页列表")
    @SaCheckPermission("equity:use-person:list")
    @GetMapping("/page")
    public R<PageResult<EquityUsePersonVO>> page(EquityUsePersonQueryDTO query) {
        return R.ok(equityUsePersonService.page(query));
    }

    @Operation(summary = "按权益编码列出全部使用人")
    @SaCheckPermission("equity:use-person:list")
    @GetMapping("/list-by-equity/{equityCode}")
    public R<List<EquityUsePersonVO>> listByEquity(@PathVariable String equityCode) {
        return R.ok(equityUsePersonService.listByEquity(equityCode));
    }

    @Operation(summary = "使用人详情")
    @SaCheckPermission("equity:use-person:query")
    @GetMapping("/{id}")
    public R<EquityUsePersonVO> getDetail(@PathVariable Long id) {
        return R.ok(equityUsePersonService.getDetail(id));
    }

    @Operation(summary = "登记使用人（≤3/身份证唯一/默认唯一）")
    @SaCheckPermission("equity:use-person:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid EquityUsePersonCreateDTO dto) {
        return R.ok(equityUsePersonService.create(dto));
    }

    @Operation(summary = "修改使用人")
    @SaCheckPermission("equity:use-person:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody EquityUsePersonUpdateDTO dto) {
        equityUsePersonService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除使用人")
    @SaCheckPermission("equity:use-person:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        equityUsePersonService.delete(id);
        return R.ok();
    }

    @Operation(summary = "设置默认权益人（同 equity_code 下其它置 0）")
    @SaCheckPermission("equity:use-person:set-default")
    @PostMapping("/set-default")
    public R<Void> setDefault(@RequestBody @Valid SetDefaultHolderDTO dto) {
        equityUsePersonService.setDefault(dto);
        return R.ok();
    }
}
