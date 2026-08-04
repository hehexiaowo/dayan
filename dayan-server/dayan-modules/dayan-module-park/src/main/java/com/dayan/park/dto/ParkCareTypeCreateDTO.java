package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 照护类型（park_care_type）创建入参。
 *
 * <p>careLevel 1-5（5 级照护）。
 */
@Data
public class ParkCareTypeCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "照护类型编码不能为空")
    @Size(max = 50)
    private String careTypeCode;

    @NotBlank(message = "照护类型名称不能为空")
    @Size(max = 200)
    private String careTypeName;

    /** 照护等级 1-5 */
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
