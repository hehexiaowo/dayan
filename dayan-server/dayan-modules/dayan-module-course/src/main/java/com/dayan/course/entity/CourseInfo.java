package com.dayan.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 表 course_info 对应实体。
 *
 * <p>学习中心四板块统一存储：course_source 区隔板块（1=平台自研 2=渠道课程
 * 3=外部课程 4=雁鸣中国资讯），course_type 保留形态语义（仅平台自研使用）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_info")
public class CourseInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 课程编码 */
    private String courseCode;

    /** 课程名称 */
    private String courseName;

    /** 课程类型（1=线上录播, 2=线上直播, 3=线下课程, 4=混合课程；非平台自研板块可空） */
    private Integer courseType;

    /** 板块来源（1=平台自研大雁 2=渠道课程 3=外部课程 4=雁鸣中国资讯） */
    private Integer courseSource;

    /** 分类编码 */
    private String categoryCode;

    /** 封面图URL */
    private String coverImage;

    /** 宣传视频URL */
    private String videoUrl;

    /** 课程描述 */
    private String courseDescription;

    /** 正文（详情页长文，纯文本） */
    private String courseBody;

    /** 课程大纲 */
    private String courseOutline;

    /** 目标人群 */
    private String targetAudience;

    /** 学习目标 */
    private String learningObjectives;

    /** 作者/来源（渠道/外部/资讯用，平台课程走讲师） */
    private String author;

    /** 时长展示文本（如 28:30 / 约 15 分钟） */
    private String durationText;

    /** 主讲讲师编码 */
    private String lecturerCode;

    /** 总课时数 */
    private Integer totalClass;

    /** 总时长(分钟) */
    private Integer totalDuration;

    /** 有效天数 */
    private Integer validDays;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 售价 */
    private BigDecimal salePrice;

    /** 最大学员数 */
    private Integer maxStudents;

    /** 当前学员数 */
    private Integer currentStudents;

    /** 浏览次数 */
    private Integer viewCount;

    /** 已售数量 */
    private Integer salesCount;

    /** 平均评分 */
    private BigDecimal ratingAvg;

    /** 是否免费 */
    private Integer isFree;

    /** 是否推荐 */
    private Integer isRecommend;

    /** 开课日期 */
    private LocalDate courseStartDate;

    /** 结课日期 */
    private LocalDate courseEndDate;

    /** 角标（热/新/要闻/人物/动态/洞察） */
    private String badge;

    /** 发布时间（资讯/内容用，课程走开课日期） */
    private LocalDateTime publishTime;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer courseStatus;

    /** 备注 */
    private String remark;
}
