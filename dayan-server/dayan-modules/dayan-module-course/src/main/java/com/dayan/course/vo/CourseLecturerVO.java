package com.dayan.course.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程讲师 VO。
 */
@Data
public class CourseLecturerVO {

    private Long id;
    /** 讲师编码（LT 前缀，被 course_info.lecturerCode 引用） */
    private String lecturerCode;
    private String lecturerName;
    private Integer gender;
    private String avatar;
    private String title;
    private String organization;
    private String specialty;
    private String introduction;
    private String certifications;
    private String phone;
    private String email;
    private Integer courseCount;
    private Integer studentCount;
    private BigDecimal ratingAvg;
    private Integer isCertified;
    private Integer sortOrder;
    /** 状态：0=禁用, 1=启用 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
