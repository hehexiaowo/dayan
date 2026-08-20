package com.dayan.course.controller.agent;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.course.service.CourseRecordLearnService;
import com.dayan.course.vo.CourseRecordLearnVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent 代理人端 — 学习记录（进度上报 + 查询）。
 *
 * <p>agentCode 从 {@link ContextHolder} 注入，防越权。
 * 路径前缀 {@code /course-records}（dayan-agent context-path=/agent-api）。
 */
@Tag(name = "Agent 学习记录")
@RestController
@RequestMapping("/course-records")
@RequiredArgsConstructor
@Validated
public class CourseRecordLearnAgentController {

    private final CourseRecordLearnService courseRecordLearnService;

    @Operation(summary = "上报学习进度（首次自动建档）")
    @PostMapping("/progress")
    public R<CourseRecordLearnVO> reportProgress(@RequestBody @Validated ProgressBody body) {
        String agentCode = requireAgentCode();
        return R.ok(courseRecordLearnService.reportProgress(
                agentCode, body.getCourseCode(), body.getCurrentLesson(), body.getLearnTimeDelta()));
    }

    @Operation(summary = "我的学习记录列表")
    @GetMapping("/my")
    public R<List<CourseRecordLearnVO>> listMy() {
        String agentCode = requireAgentCode();
        return R.ok(courseRecordLearnService.listMyRecords(agentCode));
    }

    @Operation(summary = "某课程的学习记录")
    @GetMapping("/my/{courseCode}")
    public R<CourseRecordLearnVO> getMyRecord(@PathVariable String courseCode) {
        String agentCode = requireAgentCode();
        CourseRecordLearnVO vo = courseRecordLearnService.getMyRecord(agentCode, courseCode);
        return R.ok(vo); // 可能为 null（未开始学习）
    }

    private String requireAgentCode() {
        String agentCode = ContextHolder.getAccountCode();
        if (agentCode == null || agentCode.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未获取到代理人身份");
        }
        return agentCode;
    }

    /** 进度上报请求体 */
    @Data
    public static class ProgressBody {
        @NotBlank(message = "课程编码不能为空")
        private String courseCode;
        /** 当前课时（可选） */
        @Min(value = 0, message = "课时不能为负数")
        private Integer currentLesson;
        /** 本次学习时长增量（分钟） */
        @Min(value = 0, message = "学习时长不能为负数")
        private Integer learnTimeDelta;
    }
}
