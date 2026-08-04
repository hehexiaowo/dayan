package com.dayan.content.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.content.dto.ContentMediaCreateDTO;
import com.dayan.content.dto.ContentMediaQueryDTO;
import com.dayan.content.dto.ContentMediaUpdateDTO;
import com.dayan.content.service.ContentMediaService;
import com.dayan.content.vo.ContentMediaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端内容多媒体资源接口。
 *
 * <p>路径：{@code /content/media/*}（由 dayan-admin context-path=/admin-api 拼接为
 * {@code /admin-api/content/media/*}）。
 */
@Tag(name = "内容多媒体资源管理")
@RestController
@RequestMapping("/content/media")
@RequiredArgsConstructor
public class ContentMediaAdminController {

    private final ContentMediaService contentMediaService;

    @Operation(summary = "多媒体资源分页列表（按 contentCode）")
    @GetMapping("/page")
    public R<PageResult<ContentMediaVO>> page(ContentMediaQueryDTO query) {
        return R.ok(contentMediaService.page(query));
    }

    @Operation(summary = "按 contentCode 查询资源列表")
    @GetMapping("/list")
    public R<List<ContentMediaVO>> list(@RequestParam String contentCode) {
        return R.ok(contentMediaService.listByContentCode(contentCode));
    }

    @Operation(summary = "多媒体资源详情")
    @GetMapping("/{id}")
    public R<ContentMediaVO> getDetail(@PathVariable Long id) {
        return R.ok(contentMediaService.getDetail(id));
    }

    @Operation(summary = "新增多媒体资源")
    @OperationLog(module = "内容多媒体", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ContentMediaCreateDTO dto) {
        return R.ok(contentMediaService.create(dto));
    }

    @Operation(summary = "修改多媒体资源")
    @OperationLog(module = "内容多媒体", action = "修改")
    @PutMapping
    public R<Void> update(@RequestParam Long id,
                          @RequestBody ContentMediaUpdateDTO dto) {
        contentMediaService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除多媒体资源")
    @OperationLog(module = "内容多媒体", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contentMediaService.delete(id);
        return R.ok();
    }

    @Operation(summary = "按 contentCode 批量删除多媒体资源")
    @OperationLog(module = "内容多媒体", action = "批量删除")
    @DeleteMapping("/by-code")
    public R<Void> deleteByContentCode(@RequestParam String contentCode) {
        contentMediaService.deleteByContentCode(contentCode);
        return R.ok();
    }
}
