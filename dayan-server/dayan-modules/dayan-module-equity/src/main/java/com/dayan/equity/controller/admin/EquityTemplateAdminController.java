package com.dayan.equity.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.equity.dto.EquityTemplateCreateDTO;
import com.dayan.equity.dto.EquityTemplateQueryDTO;
import com.dayan.equity.dto.EquityTemplateUpdateDTO;
import com.dayan.equity.service.EquityTemplateService;
import com.dayan.equity.vo.EquityTemplateVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端权益模板接口。
 *
 * <p>路径前缀 {@code /equity/template}，由 dayan-admin 启动模块 context-path=/admin-api
 * 拼接为 {@code /admin-api/equity/template/*}。
 */
@Tag(name = "权益模板管理")
@RestController
@RequestMapping("/equity/template")
@RequiredArgsConstructor
public class EquityTemplateAdminController {

    private final EquityTemplateService equityTemplateService;

    @Operation(summary = "权益模板分页列表")
    @SaCheckPermission("equity:template:list")
    @GetMapping("/page")
    public R<PageResult<EquityTemplateVO>> page(EquityTemplateQueryDTO query) {
        return R.ok(equityTemplateService.page(query));
    }

    @Operation(summary = "权益模板列表（全量）")
    @SaCheckPermission("equity:template:list")
    @GetMapping("/list")
    public R<List<EquityTemplateVO>> list(EquityTemplateQueryDTO query) {
        return R.ok(equityTemplateService.list(query));
    }

    @Operation(summary = "权益模板详情")
    @SaCheckPermission("equity:template:query")
    @GetMapping("/{templateCode}")
    public R<EquityTemplateVO> getDetail(@PathVariable String templateCode) {
        return R.ok(equityTemplateService.getDetail(templateCode));
    }

    @Operation(summary = "新增权益模板")
    @SaCheckPermission("equity:template:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid EquityTemplateCreateDTO dto) {
        return R.ok(equityTemplateService.create(dto));
    }

    @Operation(summary = "修改权益模板")
    @SaCheckPermission("equity:template:update")
    @PutMapping("/{templateCode}")
    public R<Void> update(@PathVariable String templateCode,
                          @RequestBody EquityTemplateUpdateDTO dto) {
        equityTemplateService.update(templateCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除权益模板")
    @SaCheckPermission("equity:template:delete")
    @DeleteMapping("/{templateCode}")
    public R<Void> delete(@PathVariable String templateCode) {
        equityTemplateService.delete(templateCode);
        return R.ok();
    }
}
