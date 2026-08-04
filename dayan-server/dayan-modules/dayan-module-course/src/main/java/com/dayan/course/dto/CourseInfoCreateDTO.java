package com.dayan.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 课程信息创建入参。
 *
 * <p>{@code courseCode} 由系统生成（CR 前缀 + 5 位序列）。
 */
@Data
public class CourseInfoCreateDTO {

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 200)
    private String courseName;

    /** 课程类型（1=线上录播, 2=线上直播, 3=线下课程, 4=混合课程） */
    @NotNull(message = "课程类型不能为空")
    private Integer courseType;

    private String categoryCode;

    @Size(max = 500)
    private String coverImage;

    @Size(max = 500)
    private String videoUrl;

    private String courseDescription;
    private String courseOutline;
    private String targetAudience;
    private String learningObjectives;
    private String lecturerCode;

    @NotNull(message = "总课时数不能为空")
    @PositiveOrZero
    private Integer totalClass;

    private Integer totalDuration;
    private Integer validDays;

    @NotNull(message = "原价不能为空")
    private BigDecimal originalPrice;

    @NotNull(message = "售价不能为空")
    private BigDecimal salePrice;

    /** 最大学员数（线下/直播课）；为空表示不限制 */
    private Integer maxStudents;

    private Integer isFree;
    private Integer isRecommend;
    private LocalDate courseStartDate;
    private LocalDate courseEndDate;
    private Integer sortOrder;

    /** 状态：0=下架, 1=上架，默认 0（下架） */
    private Integer courseStatus;

    private String remark;
}
