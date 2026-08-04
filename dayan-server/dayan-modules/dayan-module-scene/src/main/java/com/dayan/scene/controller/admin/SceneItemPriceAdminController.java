package com.dayan.scene.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.scene.dto.SceneItemPriceCreateDTO;
import com.dayan.scene.dto.SceneItemPriceQueryDTO;
import com.dayan.scene.dto.SceneItemPriceUpdateDTO;
import com.dayan.scene.service.SceneItemPriceService;
import com.dayan.scene.vo.SceneItemPriceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端场景项目定价接口。
 *
 * <p>路径：{@code /scene/item-price/*}。
 */
@Tag(name = "场景项目定价管理")
@RestController
@RequestMapping("/scene/item-price")
@RequiredArgsConstructor
public class SceneItemPriceAdminController {

    private final SceneItemPriceService sceneItemPriceService;

    @Operation(summary = "场景项目定价分页列表")
    @GetMapping("/page")
    public R<PageResult<SceneItemPriceVO>> page(SceneItemPriceQueryDTO query) {
        return R.ok(sceneItemPriceService.page(query));
    }

    @Operation(summary = "场景项目定价列表")
    @GetMapping("/list")
    public R<List<SceneItemPriceVO>> list(SceneItemPriceQueryDTO query) {
        return R.ok(sceneItemPriceService.list(query));
    }

    @Operation(summary = "场景项目定价详情")
    @GetMapping("/{id}")
    public R<SceneItemPriceVO> getDetail(@PathVariable Long id) {
        return R.ok(sceneItemPriceService.getDetail(id));
    }

    @Operation(summary = "新增场景项目定价")
    @OperationLog(module = "场景项目定价", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SceneItemPriceCreateDTO dto) {
        return R.ok(sceneItemPriceService.create(dto));
    }

    @Operation(summary = "修改场景项目定价")
    @OperationLog(module = "场景项目定价", action = "修改")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody @Valid SceneItemPriceUpdateDTO dto) {
        sceneItemPriceService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除场景项目定价")
    @OperationLog(module = "场景项目定价", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sceneItemPriceService.delete(id);
        return R.ok();
    }
}
