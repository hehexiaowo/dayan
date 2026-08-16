package com.dayan.course.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Agent 端课程 VO（列表+详情共用；不含 remark）。
 *
 * <p>列表时 lecturer 为 null、仅携带 lecturerName；详情时 lecturer 为讲师简要信息。
 */
@Data
public class CourseAgentVO {

    private String courseCode;
    private String courseName;
    /** 1=线上录播, 2=线上直播, 3=线下课程, 4=混合课程 */
    private Integer courseType;
    private String categoryCode;
    private String coverImage;
    private String courseDescription;
    /** 课程大纲 JSON：[{"title":"章节","lessons":[{"title":"课名","duration":分钟}]}] */
    private String courseOutline;
    private String targetAudience;
    private String learningObjectives;
    private String lecturerCode;
    /** 讲师姓名（列表展示用；讲师缺失时为 null） */
    private String lecturerName;
    /** 讲师简要信息（详情独有） */
    private LecturerBrief lecturer;
    private Integer totalClass;
    private Integer totalDuration;
    private Integer validDays;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private Integer currentStudents;
    private Integer maxStudents;
    private Integer viewCount;
    private Integer salesCount;
    private BigDecimal ratingAvg;
    /** 0=否, 1=是 */
    private Integer isFree;
    /** 0=否, 1=是 */
    private Integer isRecommend;
    private LocalDate courseStartDate;
    private LocalDate courseEndDate;
    private Integer sortOrder;
    /** 0=草稿, 1=待上架, 2=已上架, 3=已下架, 4=已结课（恒为 2，仅作前端确认） */
    private Integer courseStatus;

    /** 讲师简要信息（agent 端展示） */
    @Data
    public static class LecturerBrief {
        private String lecturerCode;
        private String lecturerName;
        private String avatar;
        private String title;
        private String organization;
        private String introduction;
    }
}
