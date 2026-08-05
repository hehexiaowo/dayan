package com.dayan.service.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.service.dto.ServiceVisitRecordCreateDTO;
import com.dayan.service.dto.ServiceVisitRecordQueryDTO;
import com.dayan.service.dto.ServiceVisitRecordUpdateDTO;
import com.dayan.service.service.ServiceVisitRecordService;
import com.dayan.service.vo.ServiceVisitRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端探访记录接口。
 *
 * <p>路径前缀 {@code /service/visit-record}。按 butlerCode/parkCode 聚合，
 * overallScore 综合评分，6 项检查（facility/service/hygiene/food/safety 文本 + issuesFound）。
 */
@Tag(name = "服务探访记录")
@RestController
@RequestMapping("/service/visit-record")
@RequiredArgsConstructor
public class ServiceVisitRecordAdminController {

    private final ServiceVisitRecordService serviceVisitRecordService;

    @Operation(summary = "探访记录分页列表")
    @SaCheckPermission("service:visit-record:list")
    @GetMapping("/page")
    public R<PageResult<ServiceVisitRecordVO>> page(ServiceVisitRecordQueryDTO query) {
        return R.ok(serviceVisitRecordService.page(query));
    }

    @Operation(summary = "探访记录列表（按 butlerCode/parkCode 等过滤）")
    @SaCheckPermission("service:visit-record:list")
    @GetMapping("/list")
    public R<List<ServiceVisitRecordVO>> list(ServiceVisitRecordQueryDTO query) {
        return R.ok(serviceVisitRecordService.list(query));
    }

    @Operation(summary = "探访记录详情")
    @SaCheckPermission("service:visit-record:query")
    @GetMapping("/{id}")
    public R<ServiceVisitRecordVO> getDetail(@PathVariable Long id) {
        return R.ok(serviceVisitRecordService.getDetail(id));
    }

    @Operation(summary = "新增探访记录")
    @SaCheckPermission("service:visit-record:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ServiceVisitRecordCreateDTO dto) {
        return R.ok(serviceVisitRecordService.create(dto));
    }

    @Operation(summary = "修改探访记录")
    @SaCheckPermission("service:visit-record:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ServiceVisitRecordUpdateDTO dto) {
        serviceVisitRecordService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除探访记录")
    @SaCheckPermission("service:visit-record:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        serviceVisitRecordService.delete(id);
        return R.ok();
    }
}
