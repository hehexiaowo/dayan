package com.dayan.service.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.service.dto.ServiceEquityDemandCreateDTO;
import com.dayan.service.dto.ServiceEquityDemandQueryDTO;
import com.dayan.service.dto.ServiceEquityDemandUpdateDTO;
import com.dayan.service.service.ServiceEquityDemandService;
import com.dayan.service.vo.ServiceEquityDemandVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端需求收集接口。
 *
 * <p>路径前缀 {@code /service/demand}。按 sessionCode 聚合，demandCode(DM+10) 自动生成。
 */
@Tag(name = "服务需求收集")
@RestController
@RequestMapping("/service/demand")
@RequiredArgsConstructor
public class ServiceEquityDemandAdminController {

    private final ServiceEquityDemandService serviceEquityDemandService;

    @Operation(summary = "需求分页列表")
    @SaCheckPermission("service:equity-demand:list")
    @GetMapping("/page")
    public R<PageResult<ServiceEquityDemandVO>> page(ServiceEquityDemandQueryDTO query) {
        return R.ok(serviceEquityDemandService.page(query));
    }

    @Operation(summary = "会话下需求列表")
    @SaCheckPermission("service:equity-demand:list")
    @GetMapping("/list")
    public R<List<ServiceEquityDemandVO>> list(@RequestParam String sessionCode) {
        return R.ok(serviceEquityDemandService.listBySession(sessionCode));
    }

    @Operation(summary = "需求详情")
    @SaCheckPermission("service:equity-demand:query")
    @GetMapping("/{demandCode}")
    public R<ServiceEquityDemandVO> getDetail(@PathVariable String demandCode) {
        return R.ok(serviceEquityDemandService.getDetail(demandCode));
    }

    @Operation(summary = "新增需求")
    @SaCheckPermission("service:equity-demand:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ServiceEquityDemandCreateDTO dto) {
        return R.ok(serviceEquityDemandService.create(dto));
    }

    @Operation(summary = "修改需求")
    @SaCheckPermission("service:equity-demand:update")
    @PutMapping("/{demandCode}")
    public R<Void> update(@PathVariable String demandCode,
                          @RequestBody ServiceEquityDemandUpdateDTO dto) {
        serviceEquityDemandService.update(demandCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除需求")
    @SaCheckPermission("service:equity-demand:delete")
    @DeleteMapping("/{demandCode}")
    public R<Void> delete(@PathVariable String demandCode) {
        serviceEquityDemandService.delete(demandCode);
        return R.ok();
    }
}
