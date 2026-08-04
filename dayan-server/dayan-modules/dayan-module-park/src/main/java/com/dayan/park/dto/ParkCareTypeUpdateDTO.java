package com.dayan.park.dto;

import lombok.Data;

/**
 * 照护类型（park_care_type）更新入参。
 */
@Data
public class ParkCareTypeUpdateDTO {

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
}
