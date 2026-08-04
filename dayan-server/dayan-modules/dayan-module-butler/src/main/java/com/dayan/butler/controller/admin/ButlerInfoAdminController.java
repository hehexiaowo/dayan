package com.dayan.butler.controller.admin;

import com.dayan.butler.dto.ButlerInfoCreateDTO;
import com.dayan.butler.dto.ButlerInfoQueryDTO;
import com.dayan.butler.dto.ButlerInfoUpdateDTO;
import com.dayan.butler.service.ButlerInfoService;
import com.dayan.butler.vo.ButlerInfoVO;
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
 * Admin 端管家信息接口。
 *
 * <p>路径：{@code /butler/info/*}。
 */
@Tag(name = "管家信息管理")
@RestController
@RequestMapping("/butler/info")
@RequiredArgsConstructor
public class ButlerInfoAdminController {

    private final ButlerInfoService butlerInfoService;

    @Operation(summary = "管家信息分页列表")
    @GetMapping("/page")
    public R<PageResult<ButlerInfoVO>> page(ButlerInfoQueryDTO query) {
        return R.ok(butlerInfoService.page(query));
    }

    @Operation(summary = "管家信息列表")
    @GetMapping("/list")
    public R<List<ButlerInfoVO>> list(ButlerInfoQueryDTO query) {
        return R.ok(butlerInfoService.list(query));
    }

    @Operation(summary = "管家信息详情")
    @GetMapping("/{butlerCode}")
    public R<ButlerInfoVO> getDetail(@PathVariable String butlerCode) {
        return R.ok(butlerInfoService.getDetail(butlerCode));
    }

    @Operation(summary = "新增管家信息")
    @OperationLog(module = "管家信息", action = "新增")
    @PostMapping
    public R<String> create(@RequestBody @Valid ButlerInfoCreateDTO dto) {
        return R.ok(butlerInfoService.create(dto));
    }

    @Operation(summary = "修改管家信息")
    @OperationLog(module = "管家信息", action = "修改")
    @PutMapping("/{butlerCode}")
    public R<Void> update(@PathVariable String butlerCode,
                          @RequestBody ButlerInfoUpdateDTO dto) {
        butlerInfoService.update(butlerCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除管家信息")
    @OperationLog(module = "管家信息", action = "删除")
    @DeleteMapping("/{butlerCode}")
    public R<Void> delete(@PathVariable String butlerCode) {
        butlerInfoService.delete(butlerCode);
        return R.ok();
    }
}
