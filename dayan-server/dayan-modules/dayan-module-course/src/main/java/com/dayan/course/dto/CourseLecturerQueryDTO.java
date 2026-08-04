package com.dayan.course.dto;

import lombok.Data;

/**
 * 课程讲师查询入参。
 */
@Data
public class CourseLecturerQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String lecturerCode;
    private String lecturerName;
    private String organization;
    private Integer isCertified;
    private Integer status;
}
