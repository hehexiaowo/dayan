package com.dayan.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 课程信息创建入参。
 *
 * <p>{@code courseCode} 由系统生成（CR 前缀 + 5 位序列）。
 * 非平台自研板块（渠道/外部/雁鸣）可只填基础信息，课程形态字段留空。
 */
@Data
public class CourseInfoCreateDTO {

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 200)
    private String courseName;

    /** 课程类型（1=线上录播, 2=线上直播, 3=线下课程, 4=混合课程；非平台自研板块可空） */
    private Integer courseType;

    /** 板块来源（1=平台自研大雁 2=渠道课程 3=外部课程 4=雁鸣中国资讯），默认 1 */
    private Integer courseSource;

    private String categoryCode;

    @Size(max = 500)
    private String coverImage;

    @Size(max = 500)
    private String videoUrl;

    private String courseDescription;

    /** 正文（详情页长文，纯文本） */
    private String courseBody;

    private String courseOutline;
    private String targetAudience;
    private String learningObjectives;

    /** 作者/来源（渠道/外部/资讯用，平台课程走讲师） */
    @Size(max = 100)
    private String author;

    /** 时长展示文本（如 28:30 / 约 15 分钟） */
    @Size(max = 20)
    private String durationText;

    private String lecturerCode;

    @PositiveOrZero
    private Integer totalClass;

    private Integer totalDuration;
    private Integer validDays;

    private BigDecimal originalPrice;

    private BigDecimal salePrice;

    /** 最大学员数（线下/直播课）；为空表示不限制 */
    private Integer maxStudents;

    private Integer isFree;
    private Integer isRecommend;
    private LocalDate courseStartDate;
    private LocalDate courseEndDate;

    /** 角标（热/新/要闻/人物/动态/洞察） */
    @Size(max = 20)
    private String badge;

    /** 发布时间（资讯/内容用，课程走开课日期） */
    private LocalDateTime publishTime;

    private Integer sortOrder;

    /** 状态（0=草稿, 1=待上架, 2=已上架, 3=已下架, 4=已结课），默认 0（草稿） */
    private Integer courseStatus;

    private String remark;
}
