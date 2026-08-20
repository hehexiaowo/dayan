package com.dayan.channel.controller.channel;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.channel.service.ChannelConfigCourseService;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.course.service.CourseInfoService;
import com.dayan.course.vo.CourseAgentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Channel 渠道端课程配置接口。
 *
 * <p>路径 {@code /courses/config}（dayan-channel starter context-path 拼为 {@code /channel-api/courses/config/*}）。
 *
 * <p>渠道隔离：channelCode 一律从 {@link ContextHolder} 强制注入，不接收前端参数。</p>
 */
@Tag(name = "Channel 课程配置")
@RestController
@RequestMapping("/courses/config")
@RequiredArgsConstructor
public class ChannelCourseConfigController {

    private static final int CONFIG_TYPE_VISIBILITY = 0;

    private final CourseInfoService courseInfoService;
    private final ChannelConfigCourseService channelConfigCourseService;

    @Operation(summary = "可配置课程列表（平台课程 + 渠道课程）")
    @SaCheckPermission("channel:course:view")
    @GetMapping("/available")
    public R<List<CourseAgentVO>> availableCourses() {
        String channelCode = ContextHolder.getChannelCode();
        // 返回平台课程（channel_code IS NULL）+ 本渠道课程
        return R.ok(courseInfoService.listAvailableForChannel(channelCode));
    }

    @Operation(summary = "本渠道已配置的课程编码列表")
    @SaCheckPermission("channel:course:view")
    @GetMapping("/configured")
    public R<List<String>> configuredCourseCodes() {
        String channelCode = ContextHolder.getChannelCode();
        return R.ok(channelConfigCourseService.listConfiguredCourseCodes(channelCode));
    }

    @Operation(summary = "保存课程可见性配置（全量替换）")
    @SaCheckPermission("channel:course:update")
    @PutMapping("/visibility")
    public R<Void> saveVisibility(@RequestBody List<String> courseCodes) {
        String channelCode = ContextHolder.getChannelCode();
        for (String courseCode : courseCodes) {
            channelConfigCourseService.save(channelCode, courseCode, CONFIG_TYPE_VISIBILITY, "{}");
        }
        return R.ok();
    }
}
