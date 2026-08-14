package com.dayan.service.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.service.dto.ServiceEquitySolutionCreateDTO;
import com.dayan.service.dto.ServiceEquitySolutionQueryDTO;
import com.dayan.service.dto.ServiceEquitySolutionUpdateDTO;
import com.dayan.service.dto.SolutionAcceptDTO;
import com.dayan.service.service.ServiceEquitySolutionService;
import com.dayan.service.vo.ServiceEquitySolutionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端方案定制接口。
 *
 * <p>路径前缀 {@code /service/solution}。按 sessionCode 聚合，solutionCode(SO+10) 自动生成。
 * 确认会话方案前须存在 isAccepted=1 的方案。
 */
@Tag(name = "服务方案定制")
@RestController
@RequestMapping("/service/solution")
@RequiredArgsConstructor
public class ServiceEquitySolutionAdminController {

    private final ServiceEquitySolutionService serviceEquitySolutionService;

    @Operation(summary = "方案分页列表")
    @SaCheckPermission("service:equity-solution:list")
    @GetMapping("/page")
    public R<PageResult<ServiceEquitySolutionVO>> page(ServiceEquitySolutionQueryDTO query) {
        return R.ok(serviceEquitySolutionService.page(query));
    }

    @Operation(summary = "会话下方案列表")
    @SaCheckPermission("service:equity-solution:list")
    @GetMapping("/list")
    public R<List<ServiceEquitySolutionVO>> list(@RequestParam String sessionCode) {
        return R.ok(serviceEquitySolutionService.listBySession(sessionCode));
    }

    @Operation(summary = "方案详情")
    @SaCheckPermission("service:equity-solution:query")
    @GetMapping("/{solutionCode}")
    public R<ServiceEquitySolutionVO> getDetail(@PathVariable String solutionCode) {
        return R.ok(serviceEquitySolutionService.getDetail(solutionCode));
    }

    @Operation(summary = "新增方案")
    @OperationLog(module = "权益方案", action = "新增")
    @SaCheckPermission("service:equity-solution:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ServiceEquitySolutionCreateDTO dto) {
        return R.ok(serviceEquitySolutionService.create(dto));
    }

    @Operation(summary = "修改方案")
    @OperationLog(module = "权益方案", action = "修改")
    @SaCheckPermission("service:equity-solution:update")
    @PutMapping("/{solutionCode}")
    public R<Void> update(@PathVariable String solutionCode,
                          @RequestBody ServiceEquitySolutionUpdateDTO dto) {
        serviceEquitySolutionService.update(solutionCode, dto);
        return R.ok();
    }

    @Operation(summary = "方案接受/拒绝标记（0=否, 1=是, 2=需调整）")
    @OperationLog(module = "权益方案", action = "接受标记")
    @SaCheckPermission("service:equity-solution:accept")
    @PostMapping("/accept")
    public R<Void> accept(@RequestBody @Valid SolutionAcceptDTO dto) {
        serviceEquitySolutionService.accept(dto);
        return R.ok();
    }

    @Operation(summary = "删除方案")
    @OperationLog(module = "权益方案", action = "删除")
    @SaCheckPermission("service:equity-solution:delete")
    @DeleteMapping("/{solutionCode}")
    public R<Void> delete(@PathVariable String solutionCode) {
        serviceEquitySolutionService.delete(solutionCode);
        return R.ok();
    }
}
