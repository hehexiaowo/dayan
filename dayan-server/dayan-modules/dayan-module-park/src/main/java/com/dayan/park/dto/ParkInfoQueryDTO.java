package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构主信息查询入参（分页 + 多条件）。
 */
@Data
public class ParkInfoQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String parkCode;
    private String fullName;
    private String supplierCode;
    private Integer abilityType;
    private Integer natureType;
    private Integer dayanLevel;
    /** 运营状态：0=待审核 / 1=已上线 / 2=已下架 / 3=暂停营业 */
    private Integer operateStatus;
    /** 是否已发布：1=已发布 / 0=未发布 */
    private Integer isPublished;
    private Integer isHot;
}
