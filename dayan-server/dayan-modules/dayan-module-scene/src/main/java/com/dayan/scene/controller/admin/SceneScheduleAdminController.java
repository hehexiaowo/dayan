package com.dayan.scene.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.scene.dto.SceneScheduleCreateDTO;
import com.dayan.scene.dto.SceneScheduleQueryDTO;
import com.dayan.scene.dto.SceneScheduleUpdateDTO;
import com.dayan.scene.service.SceneScheduleService;
import com.dayan.scene.vo.SceneScheduleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端场景日程接口。
 *
 * <p>路径：{@code /scene/schedule/*}。
 */
@Tag(name = "场景日程管理")
@RestController
@RequestMapping("/scene/schedule")
@RequiredArgsConstructor
public class SceneScheduleAdminController {

    private final SceneScheduleService sceneScheduleService;

    @Operation(summary = "场景日程分页列表")
    @GetMapping("/page")
    public R<PageResult<SceneScheduleVO>> page(SceneScheduleQueryDTO query) {
        return R.ok(sceneScheduleService.page(query));
    }

    @Operation(summary = "场景日程列表")
    @GetMapping("/list")
    public R<List<SceneScheduleVO>> list(SceneScheduleQueryDTO query) {
        return R.ok(sceneScheduleService.list(query));
    }

    @Operation(summary = "场景日程详情")
    @GetMapping("/{id}")
    public R<SceneScheduleVO> getDetail(@PathVariable Long id) {
        return R.ok(sceneScheduleService.getDetail(id));
    }

    @Operation(summary = "新增场景日程（含容量校验）")
    @OperationLog(module = "场景日程", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SceneScheduleCreateDTO dto) {
        return R.ok(sceneScheduleService.create(dto));
    }

    @Operation(summary = "修改场景日程（含容量校验）")
    @OperationLog(module = "场景日程", action = "修改")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody @Valid SceneScheduleUpdateDTO dto) {
        sceneScheduleService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除场景日程")
    @OperationLog(module = "场景日程", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sceneScheduleService.delete(id);
        return R.ok();
    }
}
