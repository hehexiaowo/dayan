package com.dayan.butler.controller.admin;

import com.dayan.butler.dto.ButlerAccountCreateDTO;
import com.dayan.butler.dto.ButlerAccountQueryDTO;
import com.dayan.butler.dto.ButlerAccountUpdateDTO;
import com.dayan.butler.service.ButlerAccountService;
import com.dayan.butler.vo.ButlerAccountVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端管家账号接口。
 *
 * <p>路径：{@code /butler/account/*}。
 */
@Tag(name = "管家账号管理")
@RestController
@RequestMapping("/butler/account")
@RequiredArgsConstructor
public class ButlerAccountAdminController {

    private final ButlerAccountService butlerAccountService;

    @Operation(summary = "管家账号分页列表")
    @SaCheckPermission("butler:account:list")
    @GetMapping("/page")
    public R<PageResult<ButlerAccountVO>> page(ButlerAccountQueryDTO query) {
        return R.ok(butlerAccountService.page(query));
    }

    @Operation(summary = "管家账号列表")
    @SaCheckPermission("butler:account:list")
    @GetMapping("/list")
    public R<List<ButlerAccountVO>> list(ButlerAccountQueryDTO query) {
        return R.ok(butlerAccountService.list(query));
    }

    @Operation(summary = "管家账号详情")
    @SaCheckPermission("butler:account:query")
    @GetMapping("/{id}")
    public R<ButlerAccountVO> getDetail(@PathVariable Long id) {
        return R.ok(butlerAccountService.getDetail(id));
    }

    @Operation(summary = "新增管家账号")
    @OperationLog(module = "管家账号", action = "新增")
    @SaCheckPermission("butler:account:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ButlerAccountCreateDTO dto) {
        return R.ok(butlerAccountService.create(dto));
    }

    @Operation(summary = "修改管家账号")
    @OperationLog(module = "管家账号", action = "修改")
    @SaCheckPermission("butler:account:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody ButlerAccountUpdateDTO dto) {
        butlerAccountService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "重置管家账号密码")
    @OperationLog(module = "管家账号", action = "重置密码")
    @SaCheckPermission("butler:account:reset")
    @PutMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id) {
        butlerAccountService.resetPassword(id);
        return R.ok();
    }

    @Operation(summary = "删除管家账号")
    @OperationLog(module = "管家账号", action = "删除")
    @SaCheckPermission("butler:account:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        butlerAccountService.delete(id);
        return R.ok();
    }
}
