package com.dayan.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习记录创建入参（按 courseCode/clientCode 维度）。
 *
 * <p>{@code id} 由雪花算法生成（分片表，IdType.ASSIGN_ID）。
 */
@Data
public class CourseRecordLearnCreateDTO {

    @NotBlank(message = "课程编码不能为空")
    @Size(max = 50)
    private String courseCode;

    private String clientCode;
    private String agentCode;

    @NotBlank(message = "学员姓名不能为空")
    @Size(max = 50)
    private String learnerName;

    @Size(max = 20)
    private String learnerPhone;

    /** 报名时间，为空取当前时间 */
    private LocalDateTime enrollTime;

    private String orderCode;

    @NotNull(message = "总课时不能为空")
    private Integer totalLesson;

    private Integer currentLesson;
    private Integer totalLearnTime;

    /** 状态：0=已退课, 1=学习中, 2=已完成, 3=已过期，默认 1（学习中） */
    private Integer status;
}
