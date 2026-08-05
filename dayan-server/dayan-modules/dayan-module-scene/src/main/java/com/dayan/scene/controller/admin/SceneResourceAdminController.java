package com.dayan.scene.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.scene.dto.SceneResourceCreateDTO;
import com.dayan.scene.dto.SceneResourceQueryDTO;
import com.dayan.scene.dto.SceneResourceUpdateDTO;
import com.dayan.scene.service.SceneResourceService;
import com.dayan.scene.vo.SceneResourceVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端场景资源接口。
 *
 * <p>路径：{@code /scene/resource/*}。
 */
@Tag(name = "场景资源管理")
@RestController
@RequestMapping("/scene/resource")
@RequiredArgsConstructor
public class SceneResourceAdminController {

    private final SceneResourceService sceneResourceService;

    @Operation(summary = "场景资源分页列表")
    @SaCheckPermission("scene:resource:list")
    @GetMapping("/page")
    public R<PageResult<SceneResourceVO>> page(SceneResourceQueryDTO query) {
        return R.ok(sceneResourceService.page(query));
    }

    @Operation(summary = "场景资源列表")
    @SaCheckPermission("scene:resource:list")
    @GetMapping("/list")
    public R<List<SceneResourceVO>> list(SceneResourceQueryDTO query) {
        return R.ok(sceneResourceService.list(query));
    }

    @Operation(summary = "场景资源详情")
    @SaCheckPermission("scene:resource:query")
    @GetMapping("/{id}")
    public R<SceneResourceVO> getDetail(@PathVariable Long id) {
        return R.ok(sceneResourceService.getDetail(id));
    }

    @Operation(summary = "新增场景资源（含资源冲突检测）")
    @OperationLog(module = "场景资源", action = "新增")
    @SaCheckPermission("scene:resource:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SceneResourceCreateDTO dto) {
        return R.ok(sceneResourceService.create(dto));
    }

    @Operation(summary = "修改场景资源（含资源冲突检测）")
    @OperationLog(module = "场景资源", action = "修改")
    @SaCheckPermission("scene:resource:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody @Valid SceneResourceUpdateDTO dto) {
        sceneResourceService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除场景资源")
    @OperationLog(module = "场景资源", action = "删除")
    @SaCheckPermission("scene:resource:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sceneResourceService.delete(id);
        return R.ok();
    }
}
