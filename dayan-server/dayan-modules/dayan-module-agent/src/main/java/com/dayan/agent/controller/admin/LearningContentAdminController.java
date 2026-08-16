package com.dayan.agent.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.agent.dto.LearningContentCreateDTO;
import com.dayan.agent.dto.LearningContentQueryDTO;
import com.dayan.agent.dto.LearningContentUpdateDTO;
import com.dayan.agent.service.LearningContentService;
import com.dayan.agent.vo.LearningContentAdminVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端学习中心内容管理（「课程管理」页板块 tab：渠道课程/外部课程/雁鸣中国）。
 *
 * <p>大雁课程（course_info）在课程管理页首 tab，由 course 模块承载。
 */
@Tag(name = "学习中心内容管理")
@RestController
@RequestMapping("/learning-contents")
@RequiredArgsConstructor
public class LearningContentAdminController {

    private final LearningContentService learningContentService;

    @Operation(summary = "内容分页列表")
    @SaCheckPermission("learning:content:list")
    @GetMapping
    public R<PageResult<LearningContentAdminVO>> page(LearningContentQueryDTO query) {
        return R.ok(learningContentService.page(query));
    }

    @Operation(summary = "新增内容")
    @SaCheckPermission("learning:content:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid LearningContentCreateDTO dto) {
        return R.ok(learningContentService.create(dto));
    }

    @Operation(summary = "修改内容")
    @SaCheckPermission("learning:content:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid LearningContentUpdateDTO dto) {
        learningContentService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除内容")
    @SaCheckPermission("learning:content:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        learningContentService.delete(id);
        return R.ok();
    }
}
