package com.dayan.content.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.content.dto.ContentRecordShareCreateDTO;
import com.dayan.content.dto.ContentRecordShareQueryDTO;
import com.dayan.content.dto.ContentRecordShareUpdateDTO;
import com.dayan.content.service.ContentRecordShareService;
import com.dayan.content.vo.ContentRecordShareVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端内容分享记录接口。
 *
 * <p>路径：{@code /content/record-share/*}（由 dayan-admin context-path=/admin-api 拼接为
 * {@code /admin-api/content/record-share/*}）。
 */
@Tag(name = "内容分享记录")
@RestController
@RequestMapping("/content/record-share")
@RequiredArgsConstructor
public class ContentRecordShareAdminController {

    private final ContentRecordShareService contentRecordShareService;

    @Operation(summary = "分享记录分页列表（按 contentCode）")
    @SaCheckPermission("content:record-share:list")
    @GetMapping("/page")
    public R<PageResult<ContentRecordShareVO>> page(ContentRecordShareQueryDTO query) {
        return R.ok(contentRecordShareService.page(query));
    }

    @Operation(summary = "分享记录详情")
    @SaCheckPermission("content:record-share:query")
    @GetMapping("/{id}")
    public R<ContentRecordShareVO> getDetail(@PathVariable Long id) {
        return R.ok(contentRecordShareService.getDetail(id));
    }

    @Operation(summary = "记录分享事件（前端上报）")
    @SaCheckPermission("content:record-share:create")
    @OperationLog(module = "分享记录", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ContentRecordShareCreateDTO dto) {
        return R.ok(contentRecordShareService.create(dto));
    }

    @Operation(summary = "回填分享点击/转化数据")
    @SaCheckPermission("content:record-share:update")
    @OperationLog(module = "分享记录", action = "修改")
    @PutMapping
    public R<Void> update(@RequestParam Long id,
                          @RequestBody ContentRecordShareUpdateDTO dto) {
        contentRecordShareService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除分享记录")
    @SaCheckPermission("content:record-share:delete")
    @OperationLog(module = "分享记录", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contentRecordShareService.delete(id);
        return R.ok();
    }
}
