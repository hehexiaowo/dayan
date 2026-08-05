package com.dayan.distributor.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.distributor.dto.DistributorInfoCreateDTO;
import com.dayan.distributor.dto.DistributorInfoQueryDTO;
import com.dayan.distributor.dto.DistributorInfoUpdateDTO;
import com.dayan.distributor.service.DistributorInfoService;
import com.dayan.distributor.vo.DistributorInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端分销商信息接口。
 *
 * <p>路径：{@code /admin-api/distributor/info/*}（由 dayan-admin 启动模块 context-path={@code /admin-api}
 * 与本类 {@code @RequestMapping("/distributor/info")} 拼接）。
 *
 * <p>{@code distributor_info} 为平台共享表，查询不带 channel_code 隔离条件。
 */
@Tag(name = "分销商信息管理")
@RestController
@RequestMapping("/distributor/info")
@RequiredArgsConstructor
public class DistributorInfoAdminController {

    private final DistributorInfoService distributorInfoService;

    @Operation(summary = "分销商分页列表")
    @SaCheckPermission("distributor:info:list")
    @GetMapping("/page")
    public R<PageResult<DistributorInfoVO>> page(DistributorInfoQueryDTO query) {
        return R.ok(distributorInfoService.page(query));
    }

    @Operation(summary = "分销商全量列表")
    @SaCheckPermission("distributor:info:list")
    @GetMapping("/list")
    public R<List<DistributorInfoVO>> list(DistributorInfoQueryDTO query) {
        return R.ok(distributorInfoService.list(query));
    }

    @Operation(summary = "分销商详情")
    @SaCheckPermission("distributor:info:query")
    @GetMapping("/{distributorCode}")
    public R<DistributorInfoVO> getDetail(@PathVariable String distributorCode) {
        return R.ok(distributorInfoService.getDetail(distributorCode));
    }

    @Operation(summary = "新增分销商")
    @SaCheckPermission("distributor:info:create")
    @OperationLog(module = "分销商信息管理", action = "新增")
    @PostMapping
    public R<String> create(@RequestBody @Valid DistributorInfoCreateDTO dto) {
        return R.ok(distributorInfoService.create(dto));
    }

    @Operation(summary = "修改分销商")
    @SaCheckPermission("distributor:info:update")
    @OperationLog(module = "分销商信息管理", action = "修改")
    @PutMapping
    public R<Void> update(@RequestParam String distributorCode,
                          @RequestBody @Valid DistributorInfoUpdateDTO dto) {
        distributorInfoService.update(distributorCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除分销商")
    @SaCheckPermission("distributor:info:delete")
    @OperationLog(module = "分销商信息管理", action = "删除")
    @DeleteMapping("/{distributorCode}")
    public R<Void> delete(@PathVariable String distributorCode) {
        distributorInfoService.delete(distributorCode);
        return R.ok();
    }
}
