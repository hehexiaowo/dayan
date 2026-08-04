package com.dayan.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客户照护需求评估更新入参。
 */
@Data
public class ClientCareNeedUpdateDTO {

    private String butlerCode;
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
