package com.dayan.butler.controller.admin;

import com.dayan.butler.dto.ButlerClientRelBindDTO;
import com.dayan.butler.dto.ButlerClientRelQueryDTO;
import com.dayan.butler.service.ButlerClientRelService;
import com.dayan.butler.vo.ButlerClientRelVO;
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
 * Admin 端管家-客户绑定接口。
 *
 * <p>路径：{@code /butler/client-rel/*}。
 */
@Tag(name = "管家-客户绑定管理")
@RestController
@RequestMapping("/butler/client-rel")
@RequiredArgsConstructor
public class ButlerClientRelAdminController {

    private final ButlerClientRelService butlerClientRelService;

    @Operation(summary = "管家-客户绑定分页列表")
    @GetMapping("/page")
    public R<PageResult<ButlerClientRelVO>> page(ButlerClientRelQueryDTO query) {
        return R.ok(butlerClientRelService.page(query));
    }

    @Operation(summary = "管家-客户绑定列表")
    @GetMapping("/list")
    public R<List<ButlerClientRelVO>> list(ButlerClientRelQueryDTO query) {
        return R.ok(butlerClientRelService.list(query));
    }

    @Operation(summary = "管家-客户绑定详情")
    @GetMapping("/{id}")
    public R<ButlerClientRelVO> getDetail(@PathVariable Long id) {
        return R.ok(butlerClientRelService.getDetail(id));
    }

    @Operation(summary = "绑定管家与客户（一客户一管家约束）")
    @OperationLog(module = "管家-客户绑定", action = "绑定")
    @PostMapping
    public R<Long> bind(@RequestBody @Valid ButlerClientRelBindDTO dto) {
        return R.ok(butlerClientRelService.bind(dto));
    }

    @Operation(summary = "解绑管家-客户（status 置 0）")
    @OperationLog(module = "管家-客户绑定", action = "解绑")
    @PutMapping("/{id}/unbind")
    public R<Void> unbind(@PathVariable Long id) {
        butlerClientRelService.unbind(id);
        return R.ok();
    }

    @Operation(summary = "删除管家-客户绑定")
    @OperationLog(module = "管家-客户绑定", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        butlerClientRelService.delete(id);
        return R.ok();
    }
}
