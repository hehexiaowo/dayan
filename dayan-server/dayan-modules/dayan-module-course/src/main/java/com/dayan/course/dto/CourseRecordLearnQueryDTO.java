package com.dayan.course.dto;

import lombok.Data;

/**
 * 学习记录查询入参（按 courseCode/clientCode 维度）。
 */
@Data
public class CourseRecordLearnQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String courseCode;
    private String clientCode;
    private String agentCode;
    private String learnerName;
    private String learnerPhone;
    private Integer isCompleted;
    /** 状态：0=已退课, 1=学习中, 2=已完成, 3=已过期 */
    private Integer status;
}
