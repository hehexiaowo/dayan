package com.dayan.content.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.content.dto.ContentCategoryCreateDTO;
import com.dayan.content.dto.ContentCategoryQueryDTO;
import com.dayan.content.dto.ContentCategoryUpdateDTO;
import com.dayan.content.service.ContentCategoryService;
import com.dayan.content.vo.ContentCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端内容分类接口。
 *
 * <p>路径：{@code /content/category/*}（由 dayan-admin context-path=/admin-api 拼接为
 * {@code /admin-api/content/category/*}）。
 */
@Tag(name = "内容分类管理")
@RestController
@RequestMapping("/content/category")
@RequiredArgsConstructor
public class ContentCategoryAdminController {

    private final ContentCategoryService contentCategoryService;

    @Operation(summary = "内容分类分页列表")
    @SaCheckPermission("content:category:list")
    @GetMapping("/page")
    public R<PageResult<ContentCategoryVO>> page(ContentCategoryQueryDTO query) {
        return R.ok(contentCategoryService.page(query));
    }

    @Operation(summary = "内容分类列表（全量，按名称排序）")
    @SaCheckPermission("content:category:list")
    @GetMapping("/list")
    public R<List<ContentCategoryVO>> list(ContentCategoryQueryDTO query) {
        return R.ok(contentCategoryService.list(query));
    }

    @Operation(summary = "内容分类详情")
    @SaCheckPermission("content:category:query")
    @GetMapping("/{id}")
    public R<ContentCategoryVO> getDetail(@PathVariable Long id) {
        return R.ok(contentCategoryService.getDetail(id));
    }

    @Operation(summary = "新增内容分类")
    @SaCheckPermission("content:category:create")
    @OperationLog(module = "内容分类", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ContentCategoryCreateDTO dto) {
        return R.ok(contentCategoryService.create(dto));
    }

    @Operation(summary = "修改内容分类")
    @SaCheckPermission("content:category:update")
    @OperationLog(module = "内容分类", action = "修改")
    @PutMapping
    public R<Void> update(@RequestParam Long id,
                          @RequestBody ContentCategoryUpdateDTO dto) {
        contentCategoryService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除内容分类")
    @SaCheckPermission("content:category:delete")
    @OperationLog(module = "内容分类", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        contentCategoryService.delete(id);
        return R.ok();
    }
}
