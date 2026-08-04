package com.dayan.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客户照护需求评估创建入参。
 */
@Data
public class ClientCareNeedCreateDTO {

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 64)
    private String clientCode;

    /** 评估管家编码 */
    private String butlerCode;

    /** 评估管家姓名（快照） */
    private String butlerFullName;

    private LocalDate evalDate;
    private Integer careLevel;
    private String careTypePreference;
    private String livingPreference;
    private String foodPreference;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String areaPreference;
    private String specialRequirements;
    private LocalDate expectedCheckinDate;
    private String parkRecommendations;
    private String evalResult;
    private Integer status;
    private String remark;
}
