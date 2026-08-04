package com.dayan.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 渠道信息创建入参。
 *
 * <p>{@code channelCode} 由系统生成（CH 前缀），{@code parentCode} 为空表示顶级渠道。
 */
@Data
public class ChannelInfoCreateDTO {

    @NotBlank(message = "渠道名称不能为空")
    @Size(max = 200)
    private String fullName;

    @Size(max = 50)
    private String shortName;

    /** 渠道类型：1=平台, 2=渠道商, 3=分销商 等 */
    private Integer channelType;

    /** 上级渠道编码，为空表示顶级 */
    private String parentCode;

    @Size(max = 50)
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
    private Integer sortOrder;
    private String remark;
}
