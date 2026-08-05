package com.dayan.butler.controller.admin;

import com.dayan.butler.dto.ButlerRatingCreateDTO;
import com.dayan.butler.dto.ButlerRatingQueryDTO;
import com.dayan.butler.dto.ButlerRatingUpdateDTO;
import com.dayan.butler.service.ButlerRatingService;
import com.dayan.butler.vo.ButlerRatingVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端管家评价接口。
 *
 * <p>路径：{@code /butler/rating/*}。
 */
@Tag(name = "管家评价管理")
@RestController
@RequestMapping("/butler/rating")
@RequiredArgsConstructor
public class ButlerRatingAdminController {

    private final ButlerRatingService butlerRatingService;

    @Operation(summary = "管家评价分页列表")
    @SaCheckPermission("butler:rating:list")
    @GetMapping("/page")
    public R<PageResult<ButlerRatingVO>> page(ButlerRatingQueryDTO query) {
        return R.ok(butlerRatingService.page(query));
    }

    @Operation(summary = "管家评价列表")
    @SaCheckPermission("butler:rating:list")
    @GetMapping("/list")
    public R<List<ButlerRatingVO>> list(ButlerRatingQueryDTO query) {
        return R.ok(butlerRatingService.list(query));
    }

    @Operation(summary = "管家评价详情")
    @SaCheckPermission("butler:rating:query")
    @GetMapping("/{id}")
    public R<ButlerRatingVO> getDetail(@PathVariable Long id) {
        return R.ok(butlerRatingService.getDetail(id));
    }

    @Operation(summary = "新增管家评价")
    @OperationLog(module = "管家评价", action = "新增")
    @SaCheckPermission("butler:rating:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ButlerRatingCreateDTO dto) {
        return R.ok(butlerRatingService.create(dto));
    }

    @Operation(summary = "修改管家评价")
    @OperationLog(module = "管家评价", action = "修改")
    @SaCheckPermission("butler:rating:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid ButlerRatingUpdateDTO dto) {
        butlerRatingService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除管家评价")
    @OperationLog(module = "管家评价", action = "删除")
    @SaCheckPermission("butler:rating:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        butlerRatingService.delete(id);
        return R.ok();
    }
}
