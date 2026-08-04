package com.dayan.course.dto;

import lombok.Data;

/**
 * 课程信息查询入参。
 */
@Data
public class CourseInfoQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String courseCode;
    private String courseName;
    private Integer courseType;
    private String categoryCode;
    private String lecturerCode;
    private Integer courseStatus;
    private Integer isRecommend;
}
