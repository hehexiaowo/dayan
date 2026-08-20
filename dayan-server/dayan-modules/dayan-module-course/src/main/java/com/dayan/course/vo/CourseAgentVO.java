package com.dayan.course.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Agent 端课程 VO（列表+详情共用；不含 remark）。
 *
 * <p>列表时 lecturer 为 null、仅携带 lecturerName；详情时 lecturer 为讲师简要信息。
 */
@Data
public class CourseAgentVO {

    private String courseCode;
    private String courseName;
    /** 1=线上录播, 2=线上直播, 3=线下课程, 4=混合课程（非平台自研板块为 null） */
    private Integer courseType;
    /** 板块来源（1=平台自研大雁 2=渠道课程 3=外部课程 4=雁鸣中国资讯） */
    private Integer courseSource;
    private String categoryCode;
    private String coverImage;
    private String videoUrl;
    private String courseDescription;
    /** 正文（详情页长文，纯文本） */
    private String courseBody;
    /** 课程大纲 JSON：[{"title":"章节","lessons":[{"title":"课名","duration":分钟}]}] */
    private String courseOutline;
    private String targetAudience;
    private String learningObjectives;
    /** 作者/来源（渠道/外部/资讯用，平台课程走讲师） */
    private String author;
    /** 时长展示文本（如 28:30 / 约 15 分钟） */
    private String durationText;
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
    /** 角标（热/新/要闻/人物/动态/洞察） */
    private String badge;
    /** 发布时间（资讯/内容用，课程走开课日期） */
    private LocalDateTime publishTime;
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
