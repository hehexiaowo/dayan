package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构评分更新入参（upsert 语义——不存在则创建）。
 */
@Data
public class ParkScoreUpdateDTO {

    private Integer scoreTotal;
    private Integer scoreEnvironment;
    private Integer scoreRecreation;
    private Integer scoreNursing;
    private Integer scoreFood;
    private Integer scoreService;
    private Integer scorePrice;
    private String scoreDescription;
}
