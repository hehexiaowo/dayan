package com.dayan.service.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.service.dto.AssignButlerDTO;
import com.dayan.service.dto.CancelSessionDTO;
import com.dayan.service.dto.ServiceSessionCreateDTO;
import com.dayan.service.dto.ServiceSessionQueryDTO;
import com.dayan.service.dto.ServiceSessionUpdateDTO;
import com.dayan.service.dto.SubStatusUpdateDTO;
import com.dayan.service.dto.TransitionDTO;
import com.dayan.service.service.ServiceSessionService;
import com.dayan.service.vo.ServiceSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端服务会话接口（含 SERVICE_SESSION_SM 状态机动作）。
 *
 * <p>路径前缀 {@code /service/session}（由 dayan-admin 启动模块 context-path=/admin-api
 * 拼接为 {@code /admin-api/service/session}）。
 *
 * <p>四环节联动：assign-butler → submit-demand → confirm-solution/reject-solution →
 * start-service → finish / cancel。子状态经 /sub-status 独立更新。
 */
@Tag(name = "服务会话管理")
@RestController
@RequestMapping("/service/session")
@RequiredArgsConstructor
public class ServiceSessionAdminController {

    private final ServiceSessionService serviceSessionService;

    @Operation(summary = "会话分页列表")
    @SaCheckPermission("service:session:list")
    @GetMapping("/page")
    public R<PageResult<ServiceSessionVO>> page(ServiceSessionQueryDTO query) {
        return R.ok(serviceSessionService.page(query));
    }

    @Operation(summary = "会话列表（按条件）")
    @SaCheckPermission("service:session:list")
    @GetMapping("/list")
    public R<List<ServiceSessionVO>> list(ServiceSessionQueryDTO query) {
        return R.ok(serviceSessionService.list(query));
    }

    @Operation(summary = "会话详情")
    @SaCheckPermission("service:session:query")
    @GetMapping("/{sessionCode}")
    public R<ServiceSessionVO> getDetail(@PathVariable String sessionCode) {
        return R.ok(serviceSessionService.getDetail(sessionCode));
    }

    @Operation(summary = "创建会话（初始状态=待分配）")
    @SaCheckPermission("service:session:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ServiceSessionCreateDTO dto) {
        return R.ok(serviceSessionService.create(dto));
    }

    @Operation(summary = "修改会话（普通字段）")
    @SaCheckPermission("service:session:update")
    @PutMapping("/{sessionCode}")
    public R<Void> update(@PathVariable String sessionCode,
                          @RequestBody ServiceSessionUpdateDTO dto) {
        serviceSessionService.update(sessionCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除会话")
    @SaCheckPermission("service:session:delete")
    @DeleteMapping("/{sessionCode}")
    public R<Void> delete(@PathVariable String sessionCode) {
        serviceSessionService.delete(sessionCode);
        return R.ok();
    }

    // ====== 状态机业务动作 ======

    @Operation(summary = "分配管家（assign_butler: 1→2）")
    @SaCheckPermission("service:session:assign-butler")
    @PostMapping("/assign-butler")
    public R<Void> assignButler(@RequestBody @Valid AssignButlerDTO dto) {
        serviceSessionService.assignButler(dto);
        return R.ok();
    }

    @Operation(summary = "提交需求（submit_demand: 2→3）")
    @SaCheckPermission("service:session:submit-demand")
    @PostMapping("/submit-demand")
    public R<Void> submitDemand(@RequestParam String sessionCode) {
        serviceSessionService.submitDemand(sessionCode);
        return R.ok();
    }

    @Operation(summary = "确认方案（confirm_solution: 3→4，校验已接受方案）")
    @SaCheckPermission("service:session:confirm-solution")
    @PostMapping("/confirm-solution")
    public R<Void> confirmSolution(@RequestParam String sessionCode) {
        serviceSessionService.confirmSolution(sessionCode);
        return R.ok();
    }

    @Operation(summary = "驳回方案（reject_solution: 3→2，方案 adjustCount+1）")
    @SaCheckPermission("service:session:reject-solution")
    @PostMapping("/reject-solution")
    public R<Void> rejectSolution(@RequestParam String sessionCode) {
        serviceSessionService.rejectSolution(sessionCode);
        return R.ok();
    }

    @Operation(summary = "开始服务（start_service: 4→5，校验已确认安排）")
    @SaCheckPermission("service:session:start-service")
    @PostMapping("/start-service")
    public R<Void> startService(@RequestParam String sessionCode) {
        serviceSessionService.startService(sessionCode);
        return R.ok();
    }

    @Operation(summary = "完成服务（finish: 5→6）")
    @SaCheckPermission("service:session:finish")
    @PostMapping("/finish")
    public R<Void> finish(@RequestParam String sessionCode) {
        serviceSessionService.finish(sessionCode);
        return R.ok();
    }

    @Operation(summary = "取消会话（cancel: 1/2/5→7）")
    @SaCheckPermission("service:session:cancel")
    @PostMapping("/cancel")
    public R<Void> cancel(@RequestBody @Valid CancelSessionDTO dto) {
        serviceSessionService.cancel(dto);
        return R.ok();
    }

    @Operation(summary = "通用状态机流转（传 sessionCode + event）")
    @SaCheckPermission("service:session:transition")
    @PostMapping("/transition")
    public R<Integer> transition(@RequestBody @Valid TransitionDTO dto) {
        return R.ok(serviceSessionService.transition(dto));
    }

    // ====== 子状态管理（独立于状态机） ======

    @Operation(summary = "更新子状态（终态且 refund_done 时拒绝）")
    @SaCheckPermission("service:session:sub-status")
    @PostMapping("/sub-status")
    public R<Void> updateSubStatus(@RequestBody @Valid SubStatusUpdateDTO dto) {
        serviceSessionService.updateSubStatus(dto);
        return R.ok();
    }
}
