package com.dayan.butler.controller.admin;

import com.dayan.butler.dto.ButlerScheduleCreateDTO;
import com.dayan.butler.dto.ButlerScheduleQueryDTO;
import com.dayan.butler.dto.ButlerScheduleUpdateDTO;
import com.dayan.butler.service.ButlerScheduleService;
import com.dayan.butler.vo.ButlerScheduleVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端管家排班接口。
 *
 * <p>路径：{@code /butler/schedule/*}。
 */
@Tag(name = "管家排班管理")
@RestController
@RequestMapping("/butler/schedule")
@RequiredArgsConstructor
public class ButlerScheduleAdminController {

    private final ButlerScheduleService butlerScheduleService;

    @Operation(summary = "管家排班分页列表")
    @GetMapping("/page")
    public R<PageResult<ButlerScheduleVO>> page(ButlerScheduleQueryDTO query) {
        return R.ok(butlerScheduleService.page(query));
    }

    @Operation(summary = "管家排班列表")
    @GetMapping("/list")
    public R<List<ButlerScheduleVO>> list(ButlerScheduleQueryDTO query) {
        return R.ok(butlerScheduleService.list(query));
    }

    @Operation(summary = "管家排班详情")
    @GetMapping("/{id}")
    public R<ButlerScheduleVO> getDetail(@PathVariable Long id) {
        return R.ok(butlerScheduleService.getDetail(id));
    }

    @Operation(summary = "新增管家排班（含时间重叠检测）")
    @OperationLog(module = "管家排班", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ButlerScheduleCreateDTO dto) {
        return R.ok(butlerScheduleService.create(dto));
    }

    @Operation(summary = "修改管家排班（含时间重叠检测）")
    @OperationLog(module = "管家排班", action = "修改")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody @Valid ButlerScheduleUpdateDTO dto) {
        butlerScheduleService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除管家排班")
    @OperationLog(module = "管家排班", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        butlerScheduleService.delete(id);
        return R.ok();
    }
}
