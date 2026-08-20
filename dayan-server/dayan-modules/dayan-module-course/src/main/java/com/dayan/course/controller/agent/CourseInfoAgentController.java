package com.dayan.course.controller.agent;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.course.service.CourseInfoService;
import com.dayan.course.vo.CourseAgentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Agent 代理人端 — 课程浏览（只读）。
 *
 * <p>仅暴露上架课程（courseStatus=2）；无 @SaCheckPermission（登录即可，agent 端惯例）。
 * 路径前缀 {@code /courses}（dayan-agent context-path=/agent-api 拼为 {@code /agent-api/courses}）。
 */
@Tag(name = "Agent 课程浏览")
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseInfoAgentController {

    private final CourseInfoService courseInfoService;

    @Operation(summary = "上架课程列表（按渠道过滤：平台课程 + 本渠道课程）")
    @GetMapping
    public R<List<CourseAgentVO>> list(
            @Parameter(description = "课程类型 1=线上录播 2=线上直播 3=线下课程 4=混合课程")
            @RequestParam(required = false) Integer courseType,
            @Parameter(description = "板块来源 1=平台自研大雁 2=渠道课程 3=外部课程 4=雁鸣中国资讯")
            @RequestParam(required = false) Integer courseSource) {
        String channelCode = ContextHolder.getChannelCode();
        return R.ok(courseInfoService.listForAgent(channelCode, courseType, courseSource));
    }

    @Operation(summary = "上架课程分页（支持关键词/分类筛选，按渠道配置过滤）")
    @GetMapping("/page")
    public R<PageResult<CourseAgentVO>> page(
            @Parameter(description = "课程类型") @RequestParam(required = false) Integer courseType,
            @Parameter(description = "板块来源") @RequestParam(required = false) Integer courseSource,
            @Parameter(description = "关键词（模糊搜索课程名/描述）") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类编码") @RequestParam(required = false) String categoryCode,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        String channelCode = ContextHolder.getChannelCode();
        return R.ok(courseInfoService.pagePublished(channelCode, courseType, courseSource, keyword, categoryCode, current, size));
    }

    @Operation(summary = "各板块上架课程计数")
    @GetMapping("/count")
    public R<Map<Integer, Long>> count() {
        return R.ok(courseInfoService.countPublishedBySource());
    }

    @Operation(summary = "课程详情（仅上架；累加浏览量）")
    @GetMapping("/{courseCode}")
    public R<CourseAgentVO> detail(@PathVariable String courseCode) {
        return R.ok(courseInfoService.getPublishedDetail(courseCode));
    }
}
