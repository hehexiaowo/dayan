package com.dayan.agent.controller.agent;

import com.dayan.agent.service.LearningContentService;
import com.dayan.agent.vo.LearningContentVO;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代理人端 — 学习中心内容浏览。
 * 内容由后台发布，代理人只能浏览。
 */
@Tag(name = "学习中心")
@RestController
@RequestMapping("/learning")
@RequiredArgsConstructor
public class LearningContentController {

    private final LearningContentService learningContentService;

    @Operation(summary = "内容列表（按板块分类）")
    @GetMapping("/contents")
    public R<List<LearningContentVO>> list(
            @Parameter(description = "板块分类 1=渠道课程 2=外部课程 3=雁鸣中国")
            @RequestParam(required = false) Integer category) {
        return R.ok(learningContentService.listByCategory(category));
    }

    @Operation(summary = "内容详情")
    @GetMapping("/contents/{contentCode}")
    public R<LearningContentVO> detail(@PathVariable String contentCode) {
        return R.ok(learningContentService.getDetail(contentCode));
    }
}
