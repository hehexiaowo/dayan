package com.dayan.service.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.service.dto.ServiceEquityFollowupCreateDTO;
import com.dayan.service.dto.ServiceEquityFollowupQueryDTO;
import com.dayan.service.dto.ServiceEquityFollowupUpdateDTO;
import com.dayan.service.service.ServiceEquityFollowupService;
import com.dayan.service.vo.ServiceEquityFollowupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端回访品控接口。
 *
 * <p>路径前缀 {@code /service/followup}。按 sessionCode/arrangeCode 聚合，
 * followupCode(FU+10) 自动生成。4 维满意度，任一 <3 自动 isFollowupNeeded=1。
 */
@Tag(name = "服务回访品控")
@RestController
@RequestMapping("/service/followup")
@RequiredArgsConstructor
public class ServiceEquityFollowupAdminController {

    private final ServiceEquityFollowupService serviceEquityFollowupService;

    @Operation(summary = "回访分页列表")
    @GetMapping("/page")
    public R<PageResult<ServiceEquityFollowupVO>> page(ServiceEquityFollowupQueryDTO query) {
        return R.ok(serviceEquityFollowupService.page(query));
    }

    @Operation(summary = "会话下回访列表")
    @GetMapping("/list")
    public R<List<ServiceEquityFollowupVO>> list(@RequestParam String sessionCode) {
        return R.ok(serviceEquityFollowupService.listBySession(sessionCode));
    }

    @Operation(summary = "回访详情")
    @GetMapping("/{id}")
    public R<ServiceEquityFollowupVO> getDetail(@PathVariable Long id) {
        return R.ok(serviceEquityFollowupService.getDetail(id));
    }

    @Operation(summary = "新增回访")
    @PostMapping
    public R<String> create(@RequestBody @Valid ServiceEquityFollowupCreateDTO dto) {
        return R.ok(serviceEquityFollowupService.create(dto));
    }

    @Operation(summary = "修改回访")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ServiceEquityFollowupUpdateDTO dto) {
        serviceEquityFollowupService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除回访")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        serviceEquityFollowupService.delete(id);
        return R.ok();
    }
}
