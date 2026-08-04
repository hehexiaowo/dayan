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
    private String categoryCode;
    private String coverImage;
    private String videoUrl;
    private String courseDescription;
    private String courseOutline;
    private String targetAudience;
    private String learningObjectives;
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
    private Integer sortOrder;
    /** 状态：0=下架, 1=上架 */
    private Integer courseStatus;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
