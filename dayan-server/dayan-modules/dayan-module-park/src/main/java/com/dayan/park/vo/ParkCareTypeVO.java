package com.dayan.park.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 照护类型 VO。
 */
@Data
public class ParkCareTypeVO {

    private Long id;
    private String parkCode;
    private String careTypeCode;
    private String careTypeName;
    private Integer careLevel;
    private String careTarget;
    private String careItems;
    private String careFrequency;
    private String nursePatientRatio;
    private String assessmentCriteria;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
