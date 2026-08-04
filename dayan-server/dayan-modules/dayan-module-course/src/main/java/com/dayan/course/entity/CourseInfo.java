package com.dayan.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 表 course_info 对应实体。
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

    /** 课程类型 */
    private Integer courseType;

    /** 分类编码 */
    private String categoryCode;

    /** 封面图URL */
    private String coverImage;

    /** 宣传视频URL */
    private String videoUrl;

    /** 课程描述 */
    private String courseDescription;

    /** 课程大纲 */
    private String courseOutline;

    /** 目标人群 */
    private String targetAudience;

    /** 学习目标 */
    private String learningObjectives;

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

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer courseStatus;

    /** 备注 */
    private String remark;
}
