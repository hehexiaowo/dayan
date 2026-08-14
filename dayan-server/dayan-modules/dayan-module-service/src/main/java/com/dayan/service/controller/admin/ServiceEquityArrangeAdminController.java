package com.dayan.service.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.service.dto.ArrangeConfirmDTO;
import com.dayan.service.dto.ServiceEquityArrangeCreateDTO;
import com.dayan.service.dto.ServiceEquityArrangeQueryDTO;
import com.dayan.service.dto.ServiceEquityArrangeUpdateDTO;
import com.dayan.service.service.ServiceEquityArrangeService;
import com.dayan.service.vo.ServiceEquityArrangeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端全程安排接口。
 *
 * <p>路径前缀 {@code /service/arrange}。按 sessionCode/solutionCode 聚合，
 * arrangeCode(AR+10) 自动生成。isConfirmed 置 1 后方可 start_service。
 */
@Tag(name = "服务全程安排")
@RestController
@RequestMapping("/service/arrange")
@RequiredArgsConstructor
public class ServiceEquityArrangeAdminController {

    private final ServiceEquityArrangeService serviceEquityArrangeService;

    @Operation(summary = "安排分页列表")
    @SaCheckPermission("service:equity-arrange:list")
    @GetMapping("/page")
    public R<PageResult<ServiceEquityArrangeVO>> page(ServiceEquityArrangeQueryDTO query) {
        return R.ok(serviceEquityArrangeService.page(query));
    }

    @Operation(summary = "会话下安排列表")
    @SaCheckPermission("service:equity-arrange:list")
    @GetMapping("/list")
    public R<List<ServiceEquityArrangeVO>> list(@RequestParam String sessionCode) {
        return R.ok(serviceEquityArrangeService.listBySession(sessionCode));
    }

    @Operation(summary = "安排详情")
    @SaCheckPermission("service:equity-arrange:query")
    @GetMapping("/{arrangeCode}")
    public R<ServiceEquityArrangeVO> getDetail(@PathVariable String arrangeCode) {
        return R.ok(serviceEquityArrangeService.getDetail(arrangeCode));
    }

    @Operation(summary = "新增安排")
    @OperationLog(module = "全程安排", action = "新增")
    @SaCheckPermission("service:equity-arrange:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ServiceEquityArrangeCreateDTO dto) {
        return R.ok(serviceEquityArrangeService.create(dto));
    }

    @Operation(summary = "修改安排")
    @OperationLog(module = "全程安排", action = "修改")
    @SaCheckPermission("service:equity-arrange:update")
    @PutMapping("/{arrangeCode}")
    public R<Void> update(@PathVariable String arrangeCode,
                          @RequestBody ServiceEquityArrangeUpdateDTO dto) {
        serviceEquityArrangeService.update(arrangeCode, dto);
        return R.ok();
    }

    @Operation(summary = "确认安排（isConfirmed 置 1，写 confirmTime）")
    @OperationLog(module = "全程安排", action = "确认安排")
    @SaCheckPermission("service:equity-arrange:confirm")
    @PostMapping("/confirm")
    public R<Void> confirm(@RequestBody @Valid ArrangeConfirmDTO dto) {
        serviceEquityArrangeService.confirm(dto);
        return R.ok();
    }

    @Operation(summary = "删除安排")
    @OperationLog(module = "全程安排", action = "删除")
    @SaCheckPermission("service:equity-arrange:delete")
    @DeleteMapping("/{arrangeCode}")
    public R<Void> delete(@PathVariable String arrangeCode) {
        serviceEquityArrangeService.delete(arrangeCode);
        return R.ok();
    }
}
