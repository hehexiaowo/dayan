package com.dayan.scene.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 场景项目定价查询入参。
 */
@Data
public class SceneItemPriceQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String sceneCode;
    private String sceneItemCode;
    private Integer priceType;
    private Integer status;
    /** 查询在某天生效的定价（effectiveDate <= ? <= expireDate） */
    private LocalDate activeOn;
}
