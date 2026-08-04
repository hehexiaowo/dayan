package com.dayan.butler.controller.admin;

import com.dayan.butler.dto.ButlerServiceRecordCreateDTO;
import com.dayan.butler.dto.ButlerServiceRecordQueryDTO;
import com.dayan.butler.dto.ButlerServiceRecordUpdateDTO;
import com.dayan.butler.service.ButlerServiceRecordService;
import com.dayan.butler.vo.ButlerServiceRecordVO;
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
 * Admin 端管家服务记录接口。
 *
 * <p>路径：{@code /butler/service-record/*}。
 */
@Tag(name = "管家服务记录管理")
@RestController
@RequestMapping("/butler/service-record")
@RequiredArgsConstructor
public class ButlerServiceRecordAdminController {

    private final ButlerServiceRecordService butlerServiceRecordService;

    @Operation(summary = "管家服务记录分页列表")
    @GetMapping("/page")
    public R<PageResult<ButlerServiceRecordVO>> page(ButlerServiceRecordQueryDTO query) {
        return R.ok(butlerServiceRecordService.page(query));
    }

    @Operation(summary = "管家服务记录列表")
    @GetMapping("/list")
    public R<List<ButlerServiceRecordVO>> list(ButlerServiceRecordQueryDTO query) {
        return R.ok(butlerServiceRecordService.list(query));
    }

    @Operation(summary = "管家服务记录详情")
    @GetMapping("/{id}")
    public R<ButlerServiceRecordVO> getDetail(@PathVariable Long id) {
        return R.ok(butlerServiceRecordService.getDetail(id));
    }

    @Operation(summary = "新增管家服务记录")
    @OperationLog(module = "管家服务记录", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ButlerServiceRecordCreateDTO dto) {
        return R.ok(butlerServiceRecordService.create(dto));
    }

    @Operation(summary = "修改管家服务记录")
    @OperationLog(module = "管家服务记录", action = "修改")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ButlerServiceRecordUpdateDTO dto) {
        butlerServiceRecordService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除管家服务记录")
    @OperationLog(module = "管家服务记录", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        butlerServiceRecordService.delete(id);
        return R.ok();
    }
}
