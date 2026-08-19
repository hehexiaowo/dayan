package com.dayan.channel.controller.channel;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.R;
import com.dayan.system.service.SystemKnowledgeRepoService;
import com.dayan.system.vo.SystemCategoryAddDTO;
import com.dayan.system.vo.SystemCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Channel 渠道端类目管理接口（百炼业务空间级类目，所有知识库共享，多级树）。
 *
 * <p>路径前缀 {@code /system/knowledge/categories}（由 dayan-channel 启动模块 context-path 拼接）。
 */
@Tag(name = "Channel 知识仓库类目")
@RestController
@RequestMapping("/system/knowledge/categories")
@RequiredArgsConstructor
public class ChannelKnowledgeCategoryController {

    private final SystemKnowledgeRepoService knowledgeRepoService;

    @Operation(summary = "类目列表（全量平铺，前端组树）")
    @SaCheckPermission("channel:knowledge:view")
    @GetMapping
    public R<List<SystemCategoryVO>> listCategories() {
        return R.ok(knowledgeRepoService.listCategories());
    }

    @Operation(summary = "新增类目（parentCategoryId 空=顶级）")
    @SaCheckPermission("channel:knowledge:create")
    @PostMapping
    public R<String> addCategory(@RequestBody @Valid SystemCategoryAddDTO dto) {
        return R.ok(knowledgeRepoService.addCategory(dto.getCategoryName(), dto.getParentCategoryId()));
    }

    @Operation(summary = "删除类目（类目下有文件时百炼拒绝）")
    @SaCheckPermission("channel:knowledge:delete")
    @DeleteMapping("/{categoryId}")
    public R<Void> deleteCategory(@PathVariable String categoryId) {
        knowledgeRepoService.deleteCategory(categoryId);
        return R.ok();
    }
}
