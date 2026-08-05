package com.dayan.supplier.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.supplier.dto.SupplierOpenPlatformCreateDTO;
import com.dayan.supplier.dto.SupplierOpenPlatformQueryDTO;
import com.dayan.supplier.dto.SupplierOpenPlatformUpdateDTO;
import com.dayan.supplier.service.SupplierOpenPlatformService;
import com.dayan.supplier.vo.SupplierOpenPlatformVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端供应商开放平台配置接口。
 *
 * <p>路径：{@code /supplier/open-platform/*}（由 dayan-admin context-path=/admin-api 拼接为
 * {@code /admin-api/supplier/open-platform/*}）。{@code appSecret}/{@code webhookSecret}
 * 入参明文、出参脱敏。
 */
@Tag(name = "供应商开放平台配置")
@RestController
@RequestMapping("/supplier/open-platform")
@RequiredArgsConstructor
public class SupplierOpenPlatformAdminController {

    private final SupplierOpenPlatformService supplierOpenPlatformService;

    @Operation(summary = "供应商开放平台配置分页列表")
    @SaCheckPermission("supplier:open-platform:list")
    @GetMapping("/page")
    public R<PageResult<SupplierOpenPlatformVO>> page(SupplierOpenPlatformQueryDTO query) {
        return R.ok(supplierOpenPlatformService.page(query));
    }

    @Operation(summary = "供应商开放平台配置详情")
    @SaCheckPermission("supplier:open-platform:query")
    @GetMapping("/{id}")
    public R<SupplierOpenPlatformVO> getDetail(@PathVariable Long id) {
        return R.ok(supplierOpenPlatformService.getDetail(id));
    }

    @Operation(summary = "新增供应商开放平台配置")
    @OperationLog(module = "供应商开放平台", action = "新增")
    @SaCheckPermission("supplier:open-platform:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SupplierOpenPlatformCreateDTO dto) {
        return R.ok(supplierOpenPlatformService.create(dto));
    }

    @Operation(summary = "修改供应商开放平台配置")
    @OperationLog(module = "供应商开放平台", action = "修改")
    @SaCheckPermission("supplier:open-platform:update")
    @PutMapping
    public R<Void> update(@RequestParam Long id,
                          @RequestBody SupplierOpenPlatformUpdateDTO dto) {
        supplierOpenPlatformService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除供应商开放平台配置")
    @OperationLog(module = "供应商开放平台", action = "删除")
    @SaCheckPermission("supplier:open-platform:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        supplierOpenPlatformService.delete(id);
        return R.ok();
    }
}
