package com.dayan.course.dto;

import lombok.Data;

/**
 * 课程讲师修改入参（按 id 定位，非空字段才更新）。
 */
@Data
public class CourseLecturerUpdateDTO {

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
    private Integer isCertified;
    private Integer sortOrder;
    private Integer status;
}
