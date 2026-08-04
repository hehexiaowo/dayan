package com.dayan.service.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.service.dto.ServiceEvaluationCreateDTO;
import com.dayan.service.dto.ServiceEvaluationQueryDTO;
import com.dayan.service.dto.ServiceEvaluationUpdateDTO;
import com.dayan.service.service.ServiceEvaluationService;
import com.dayan.service.vo.ServiceEvaluationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端服务评价接口。
 *
 * <p>路径前缀 {@code /service/evaluation}。<b>一会话一评价</b>（同 sessionCode 仅 1 条），
 * 4 维评分（attitude/professional/responsiveness/satisfaction，1-5）。
 */
@Tag(name = "服务评价")
@RestController
@RequestMapping("/service/evaluation")
@RequiredArgsConstructor
public class ServiceEvaluationAdminController {

    private final ServiceEvaluationService serviceEvaluationService;

    @Operation(summary = "评价分页列表")
    @GetMapping("/page")
    public R<PageResult<ServiceEvaluationVO>> page(ServiceEvaluationQueryDTO query) {
        return R.ok(serviceEvaluationService.page(query));
    }

    @Operation(summary = "评价列表（支持 sessionCode/butlerCode/parkCode 等过滤，一会话一评价至多 1 条）")
    @GetMapping("/list")
    public R<List<ServiceEvaluationVO>> list(ServiceEvaluationQueryDTO query) {
        return R.ok(serviceEvaluationService.list(query));
    }

    @Operation(summary = "评价详情")
    @GetMapping("/{id}")
    public R<ServiceEvaluationVO> getDetail(@PathVariable Long id) {
        return R.ok(serviceEvaluationService.getDetail(id));
    }

    @Operation(summary = "新增评价（一会话一评价，重复将抛业务异常）")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ServiceEvaluationCreateDTO dto) {
        return R.ok(serviceEvaluationService.create(dto));
    }

    @Operation(summary = "修改评价（含回复）")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ServiceEvaluationUpdateDTO dto) {
        serviceEvaluationService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除评价")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        serviceEvaluationService.delete(id);
        return R.ok();
    }
}
