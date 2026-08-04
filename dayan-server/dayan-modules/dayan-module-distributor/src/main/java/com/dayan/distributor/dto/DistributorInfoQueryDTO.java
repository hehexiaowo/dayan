package com.dayan.distributor.dto;

import lombok.Data;

/**
 * 分销商查询入参。
 *
 * <p>{@code distributor_info} 为平台共享表，查询不带 channel_code 隔离条件。
 */
@Data
public class DistributorInfoQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    /** 分销商编码（精确） */
    private String distributorCode;
    /** 分销商全称（模糊） */
    private String fullName;
    /** 主体类型（1=企业, 2=个人） */
    private Integer subjectType;
    /** 统一社会信用代码（精确） */
    private String unifiedCreditCode;
    /** 联系电话（模糊） */
    private String phone;
    /** 状态（0=待审核, 1=已合作, 2=已暂停, 3=已终止） */
    private Integer status;
}
