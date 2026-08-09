package com.dayan.park.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构评分 VO。
 */
@Data
public class ParkScoreVO {

    private Long id;
    private String parkCode;
    private Integer scoreTotal;
    private Integer scoreEnvironment;
    private Integer scoreRecreation;
    private Integer scoreNursing;
    private Integer scoreFood;
    private Integer scoreService;
    private Integer scorePrice;
    private String scoreDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
