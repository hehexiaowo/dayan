package com.dayan.scene.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 场景日程 VO。
 */
@Data
public class SceneScheduleVO {

    private Long id;
    private String sceneCode;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxPerson;
    private Integer currentPerson;
    private BigDecimal priceOverride;
    private String remark;
    /** 状态：1开放 / 2已约满 / 3关闭 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
