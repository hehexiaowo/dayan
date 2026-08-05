package com.dayan.content.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.content.dto.ContentInfoAuditDTO;
import com.dayan.content.dto.ContentInfoCreateDTO;
import com.dayan.content.dto.ContentInfoQueryDTO;
import com.dayan.content.dto.ContentInfoUpdateDTO;
import com.dayan.content.service.ContentInfoService;
import com.dayan.content.vo.ContentInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端内容信息接口（审核流）。
 *
 * <p>路径：{@code /content/info/*}（由 dayan-admin 启动模块 context-path=/admin-api 拼接为
 * {@code /admin-api/content/info/*}）。
 *
 * <p>审核流：{@code /submit}（草稿→待审）、{@code /audit}（待审→通过/拒绝）、
 * {@code /publish}（通过→发布）、{@code /offline}（通过→下线）。
 */
@Tag(name = "内容信息管理")
@RestController
@RequestMapping("/content/info")
@RequiredArgsConstructor
public class ContentInfoAdminController {

    private final ContentInfoService contentInfoService;

    @Operation(summary = "内容分页列表")
    @SaCheckPermission("content:info:list")
    @GetMapping("/page")
    public R<PageResult<ContentInfoVO>> page(ContentInfoQueryDTO query) {
        return R.ok(contentInfoService.page(query));
    }

    @Operation(summary = "内容列表（全量）")
    @SaCheckPermission("content:info:list")
    @GetMapping("/list")
    public R<PageResult<ContentInfoVO>> list(ContentInfoQueryDTO query) {
        query.setSize(1000L);
        return R.ok(contentInfoService.page(query));
    }

    @Operation(summary = "内容详情")
    @SaCheckPermission("content:info:query")
    @GetMapping("/{contentCode}")
    public R<ContentInfoVO> getDetail(@PathVariable String contentCode) {
        return R.ok(contentInfoService.getDetail(contentCode));
    }

    @Operation(summary = "新增内容")
    @SaCheckPermission("content:info:create")
    @OperationLog(module = "内容信息", action = "新增")
    @PostMapping
    public R<String> create(@RequestBody @Valid ContentInfoCreateDTO dto) {
        return R.ok(contentInfoService.create(dto));
    }

    @Operation(summary = "修改内容")
    @SaCheckPermission("content:info:update")
    @OperationLog(module = "内容信息", action = "修改")
    @PutMapping
    public R<Void> update(@RequestParam String contentCode,
                          @RequestBody ContentInfoUpdateDTO dto) {
        contentInfoService.update(contentCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除内容")
    @SaCheckPermission("content:info:delete")
    @OperationLog(module = "内容信息", action = "删除")
    @DeleteMapping("/{contentCode}")
    public R<Void> delete(@PathVariable String contentCode) {
        contentInfoService.delete(contentCode);
        return R.ok();
    }

    @Operation(summary = "提交审核（草稿→待审）")
    @SaCheckPermission("content:info:submit")
    @OperationLog(module = "内容信息", action = "提交审核")
    @PostMapping("/submit")
    public R<Void> submit(@RequestParam String contentCode) {
        contentInfoService.submit(contentCode);
        return R.ok();
    }

    @Operation(summary = "审核内容（待审→通过/拒绝）")
    @SaCheckPermission("content:info:audit")
    @OperationLog(module = "内容信息", action = "审核")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody @Valid ContentInfoAuditDTO dto) {
        contentInfoService.audit(dto);
        return R.ok();
    }

    @Operation(summary = "发布内容（通过→发布）")
    @SaCheckPermission("content:info:publish")
    @OperationLog(module = "内容信息", action = "发布")
    @PostMapping("/publish")
    public R<Void> publish(@RequestParam String contentCode) {
        contentInfoService.publish(contentCode);
        return R.ok();
    }

    @Operation(summary = "下线内容（通过→下线）")
    @SaCheckPermission("content:info:offline")
    @OperationLog(module = "内容信息", action = "下线")
    @PostMapping("/offline")
    public R<Void> offline(@RequestParam String contentCode) {
        contentInfoService.offline(contentCode);
        return R.ok();
    }
}
