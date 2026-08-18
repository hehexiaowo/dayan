package com.dayan.course.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 课程信息 VO。
 */
@Data
public class CourseInfoVO {

    private Long id;
    /** 课程编码（CR 前缀） */
    private String courseCode;
    /** 课程名称 */
    private String courseName;
    /** 课程类型（1=线上录播, 2=线上直播, 3=线下课程, 4=混合课程） */
    private Integer courseType;
    /** 板块来源（1=平台自研大雁 2=渠道课程 3=外部课程 4=雁鸣中国资讯） */
    private Integer courseSource;
    private String categoryCode;
    private String coverImage;
    private String videoUrl;
    private String courseDescription;
    /** 正文（详情页长文，纯文本） */
    private String courseBody;
    private String courseOutline;
    private String targetAudience;
    private String learningObjectives;
    /** 作者/来源（渠道/外部/资讯用，平台课程走讲师） */
    private String author;
    /** 时长展示文本（如 28:30 / 约 15 分钟） */
    private String durationText;
    private String lecturerCode;
    private Integer totalClass;
    private Integer totalDuration;
    private Integer validDays;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    /** 最大学员数 */
    private Integer maxStudents;
    /** 当前学员数 */
    private Integer currentStudents;
    private Integer viewCount;
    private Integer salesCount;
    private BigDecimal ratingAvg;
    private Integer isFree;
    private Integer isRecommend;
    private LocalDate courseStartDate;
    private LocalDate courseEndDate;
    /** 角标（热/新/要闻/人物/动态/洞察） */
    private String badge;
    /** 发布时间（资讯/内容用，课程走开课日期） */
    private LocalDateTime publishTime;
    private Integer sortOrder;
    /** 状态：0=下架, 1=上架 */
    private Integer courseStatus;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
