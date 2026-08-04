package com.dayan.course.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 课程信息修改入参（按 courseCode 定位，非空字段才更新）。
 *
 * <p>含 maxStudents 调整时需重新校验 currentStudents ≤ maxStudents。
 */
@Data
public class CourseInfoUpdateDTO {

    private String courseName;
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

    /** 最大学员数（调整后需校验 currentStudents ≤ maxStudents） */
    private Integer maxStudents;

    private Integer isFree;
    private Integer isRecommend;
    private LocalDate courseStartDate;
    private LocalDate courseEndDate;
    private Integer sortOrder;

    /** 状态：0=下架, 1=上架 */
    private Integer courseStatus;

    private String remark;
}
