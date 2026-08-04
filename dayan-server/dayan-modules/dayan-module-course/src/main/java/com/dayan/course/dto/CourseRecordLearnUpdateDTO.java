package com.dayan.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习记录修改入参（按 id 定位，非空字段才更新）。
 *
 * <p>进度更新（currentLesson/totalLearnTime/learnProgress）会同步刷新 lastLearnTime。
 * learnProgress 自动按 currentLesson/totalLesson 推算（也可手动指定）。
 */
@Data
public class CourseRecordLearnUpdateDTO {

    private String learnerName;
    private String learnerPhone;
    private String orderCode;

    /** 当前学到第几课 */
    @Min(0)
    private Integer currentLesson;

    /** 总课时 */
    @Min(0)
    private Integer totalLesson;

    /** 学习进度（%），为空则按 currentLesson/totalLesson 自动计算 */
    @Min(0)
    @Max(100)
    private java.math.BigDecimal learnProgress;

    /** 累计学习时长（分钟） */
    @Min(0)
    private Integer totalLearnTime;

    /** 最近学习时间，为空取当前时间 */
    private LocalDateTime lastLearnTime;

    /** 是否完成：0=否, 1=是 */
    private Integer isCompleted;

    /** 完成时间 */
    private LocalDateTime completeTime;

    private String certificateUrl;

    /** 课程评分（1-5） */
    @Min(1)
    @Max(5)
    private Integer rating;

    private String ratingContent;

    /** 状态：0=已退课, 1=学习中, 2=已完成, 3=已过期 */
    private Integer status;
}
