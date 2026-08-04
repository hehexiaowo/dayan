package com.dayan.butler.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 管家排班 VO。
 */
@Data
public class ButlerScheduleVO {

    private Long id;
    /** 管家编码 */
    private String butlerCode;
    /** 排班日期 */
    private LocalDate scheduleDate;
    /** 排班类型：1=上班 / 2=休假 / 3=外勤 / 4=培训 */
    private Integer scheduleType;
    /** 上班时间 */
    private LocalTime startTime;
    /** 下班时间 */
    private LocalTime endTime;
    /** 状态：0=无效 / 1=有效 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
