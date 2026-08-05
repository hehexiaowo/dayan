package com.dayan.scene.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.scene.dto.SceneItemCreateDTO;
import com.dayan.scene.dto.SceneItemQueryDTO;
import com.dayan.scene.dto.SceneItemUpdateDTO;
import com.dayan.scene.service.SceneItemService;
import com.dayan.scene.vo.SceneItemVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端场景项目接口。
 *
 * <p>路径：{@code /scene/item/*}。
 */
@Tag(name = "场景项目管理")
@RestController
@RequestMapping("/scene/item")
@RequiredArgsConstructor
public class SceneItemAdminController {

    private final SceneItemService sceneItemService;

    @Operation(summary = "场景项目分页列表")
    @SaCheckPermission("scene:item:list")
    @GetMapping("/page")
    public R<PageResult<SceneItemVO>> page(SceneItemQueryDTO query) {
        return R.ok(sceneItemService.page(query));
    }

    @Operation(summary = "场景项目列表")
    @SaCheckPermission("scene:item:list")
    @GetMapping("/list")
    public R<List<SceneItemVO>> list(SceneItemQueryDTO query) {
        return R.ok(sceneItemService.list(query));
    }

    @Operation(summary = "场景项目详情")
    @SaCheckPermission("scene:item:query")
    @GetMapping("/{id}")
    public R<SceneItemVO> getDetail(@PathVariable Long id) {
        return R.ok(sceneItemService.getDetail(id));
    }

    @Operation(summary = "新增场景项目")
    @OperationLog(module = "场景项目", action = "新增")
    @SaCheckPermission("scene:item:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SceneItemCreateDTO dto) {
        return R.ok(sceneItemService.create(dto));
    }

    @Operation(summary = "修改场景项目")
    @OperationLog(module = "场景项目", action = "修改")
    @SaCheckPermission("scene:item:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody @Valid SceneItemUpdateDTO dto) {
        sceneItemService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除场景项目")
    @OperationLog(module = "场景项目", action = "删除")
    @SaCheckPermission("scene:item:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sceneItemService.delete(id);
        return R.ok();
    }
}
