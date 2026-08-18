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
    /** 板块来源（1=平台自研大雁 2=渠道课程 3=外部课程 4=雁鸣中国资讯） */
    private Integer courseSource;
    private String categoryCode;
    private String lecturerCode;
    private Integer courseStatus;
    private Integer isRecommend;
}
