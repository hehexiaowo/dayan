package com.dayan.park.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.ParkInfoCreateDTO;
import com.dayan.park.dto.ParkInfoQueryDTO;
import com.dayan.park.dto.ParkInfoUpdateDTO;
import com.dayan.park.service.ParkInfoService;
import com.dayan.park.vo.ParkInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端机构主信息接口（含状态机流转）。
 *
 * <p>路径前缀 {@code /park/info}，由 dayan-admin 启动模块 context-path=/admin-api
 * 拼接为 {@code /admin-api/park/info/*}。
 */
@Tag(name = "机构主信息管理")
@Validated
@RestController
@RequestMapping("/park/info")
@RequiredArgsConstructor
public class ParkInfoAdminController {

    private final ParkInfoService parkInfoService;

    @Operation(summary = "机构分页列表")
    @GetMapping("/page")
    public R<PageResult<ParkInfoVO>> page(ParkInfoQueryDTO query) {
        return R.ok(parkInfoService.page(query));
    }

    @Operation(summary = "机构列表（全量）")
    @GetMapping("/list")
    public R<List<ParkInfoVO>> list(ParkInfoQueryDTO query) {
        return R.ok(parkInfoService.list(query));
    }

    @Operation(summary = "机构详情")
    @GetMapping("/{parkCode}")
    public R<ParkInfoVO> getDetail(@PathVariable String parkCode) {
        return R.ok(parkInfoService.getDetail(parkCode));
    }

    @Operation(summary = "新增机构")
    @PostMapping
    public R<String> create(@RequestBody @Valid ParkInfoCreateDTO dto) {
        return R.ok(parkInfoService.create(dto));
    }

    @Operation(summary = "修改机构")
    @PutMapping("/{parkCode}")
    public R<Void> update(@PathVariable String parkCode,
                          @RequestBody ParkInfoUpdateDTO dto) {
        parkInfoService.update(parkCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除机构")
    @DeleteMapping("/{parkCode}")
    public R<Void> delete(@PathVariable String parkCode) {
        parkInfoService.delete(parkCode);
        return R.ok();
    }

    @Operation(summary = "机构状态流转（approve/offline/online/suspend/resume）")
    @PostMapping("/transition")
    public R<Integer> transition(@RequestParam @NotBlank String parkCode,
                                 @RequestParam @NotBlank String event) {
        return R.ok(parkInfoService.transition(parkCode, event));
    }
}
