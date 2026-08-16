package com.dayan.channel.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 渠道信息更新入参（{@code channelCode} 不可改，由路径参数提供）。
 */
@Data
public class ChannelInfoUpdateDTO {

    private String fullName;
    private String shortName;
    private Integer channelType;
    /** 移动渠道时指定新的父渠道编码 */
    private String parentCode;
    private String unifiedCreditCode;
    private String legalPerson;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String address;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String logoUrl;
    private String description;
    private String distributorCode;
    private LocalDate cooperationStartDate;
    private Integer settlementCycle;
    private String featureConfig;
    private Integer status;
    private Integer auditStatus;
    /** 管理配置能力：0=业务型，1=管理型 */
    private Integer canManage;
    private Integer sortOrder;
    private String remark;
}
