package com.dayan.course.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学习记录 VO。
 */
@Data
public class CourseRecordLearnVO {

    private Long id;
    private String courseCode;
    private String clientCode;
    private String agentCode;
    private String learnerName;
    private String learnerPhone;
    private LocalDateTime enrollTime;
    private String orderCode;
    private Integer currentLesson;
    private Integer totalLesson;
    /** 学习进度（%） */
    private BigDecimal learnProgress;
    private Integer totalLearnTime;
    private LocalDateTime lastLearnTime;
    /** 是否完成：0=否, 1=是 */
    private Integer isCompleted;
    private LocalDateTime completeTime;
    private String certificateUrl;
    private Integer rating;
    private String ratingContent;
    /** 状态：0=已退课, 1=学习中, 2=已完成, 3=已过期 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
