package com.dayan.scene.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.scene.dto.SceneInfoAuditDTO;
import com.dayan.scene.dto.SceneInfoCreateDTO;
import com.dayan.scene.dto.SceneInfoQueryDTO;
import com.dayan.scene.dto.SceneInfoUpdateDTO;
import com.dayan.scene.service.SceneInfoService;
import com.dayan.scene.vo.SceneInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端场景信息接口。
 *
 * <p>路径：{@code /scene/info/*}（由 dayan-admin 启动模块 context-path=/admin-api 拼接为
 * {@code /admin-api/scene/info/*}）。
 */
@Tag(name = "场景信息管理")
@RestController
@RequestMapping("/scene/info")
@RequiredArgsConstructor
public class SceneInfoAdminController {

    private final SceneInfoService sceneInfoService;

    @Operation(summary = "场景分页列表")
    @GetMapping("/page")
    public R<PageResult<SceneInfoVO>> page(SceneInfoQueryDTO query) {
        return R.ok(sceneInfoService.page(query));
    }

    @Operation(summary = "场景列表")
    @GetMapping("/list")
    public R<List<SceneInfoVO>> list(SceneInfoQueryDTO query) {
        return R.ok(sceneInfoService.list(query));
    }

    @Operation(summary = "场景详情")
    @GetMapping("/{sceneCode}")
    public R<SceneInfoVO> getDetail(@PathVariable String sceneCode) {
        return R.ok(sceneInfoService.getDetail(sceneCode));
    }

    @Operation(summary = "新增场景")
    @OperationLog(module = "场景信息", action = "新增")
    @PostMapping
    public R<String> create(@RequestBody @Valid SceneInfoCreateDTO dto) {
        return R.ok(sceneInfoService.create(dto));
    }

    @Operation(summary = "修改场景")
    @OperationLog(module = "场景信息", action = "修改")
    @PutMapping("/{sceneCode}")
    public R<Void> update(@PathVariable String sceneCode,
                          @RequestBody SceneInfoUpdateDTO dto) {
        sceneInfoService.update(sceneCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除场景")
    @OperationLog(module = "场景信息", action = "删除")
    @DeleteMapping("/{sceneCode}")
    public R<Void> delete(@PathVariable String sceneCode) {
        sceneInfoService.delete(sceneCode);
        return R.ok();
    }

    @Operation(summary = "提交审核")
    @OperationLog(module = "场景信息", action = "提交审核")
    @PostMapping("/submit")
    public R<Void> submit(@RequestParam String sceneCode) {
        sceneInfoService.submit(sceneCode);
        return R.ok();
    }

    @Operation(summary = "审核场景")
    @OperationLog(module = "场景信息", action = "审核")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody @Valid SceneInfoAuditDTO dto) {
        sceneInfoService.audit(dto);
        return R.ok();
    }

    @Operation(summary = "上架")
    @OperationLog(module = "场景信息", action = "上架")
    @PostMapping("/shelves")
    public R<Void> shelves(@RequestParam String sceneCode) {
        sceneInfoService.shelves(sceneCode);
        return R.ok();
    }

    @Operation(summary = "下架")
    @OperationLog(module = "场景信息", action = "下架")
    @PostMapping("/offshelves")
    public R<Void> offshelves(@RequestParam String sceneCode) {
        sceneInfoService.offshelves(sceneCode);
        return R.ok();
    }

    @Operation(summary = "重新上架")
    @OperationLog(module = "场景信息", action = "重新上架")
    @PostMapping("/reshelves")
    public R<Void> reshelves(@RequestParam String sceneCode) {
        sceneInfoService.reshelves(sceneCode);
        return R.ok();
    }

    @Operation(summary = "满期（活动到期或名额约满）")
    @OperationLog(module = "场景信息", action = "满期")
    @PostMapping("/full")
    public R<Void> full(@RequestParam String sceneCode) {
        sceneInfoService.full(sceneCode);
        return R.ok();
    }
}
