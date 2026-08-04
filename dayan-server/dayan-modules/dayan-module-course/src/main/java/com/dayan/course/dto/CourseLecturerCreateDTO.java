package com.dayan.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 课程讲师创建入参。
 *
 * <p>{@code lecturerCode} 由系统生成（LT 前缀 + 5 位序列）。
 */
@Data
public class CourseLecturerCreateDTO {

    @NotBlank(message = "讲师姓名不能为空")
    @Size(max = 50)
    private String lecturerName;

    private Integer gender;

    @Size(max = 500)
    private String avatar;

    @Size(max = 100)
    private String title;

    @Size(max = 200)
    private String organization;

    private String specialty;
    private String introduction;
    private String certifications;

    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String email;

    private Integer isCertified;
    private Integer sortOrder;

    /** 状态：0=禁用, 1=启用，默认 1 */
    private Integer status;
}
