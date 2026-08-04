package com.dayan.channel.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 渠道信息 VO（含 children 用于树形展示）。
 */
@Data
public class ChannelInfoVO {

    private Long id;
    private String channelCode;
    private String fullName;
    private String shortName;
    private Integer channelType;
    private String parentCode;
    private String ancestors;
    private Integer level;
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
    private Integer agentCount;
    private BigDecimal totalOrderAmount;
    private LocalDate cooperationStartDate;
    private String distributorCode;
    private Integer settlementCycle;
    private String featureConfig;
    private Integer sortOrder;
    private Integer status;
    private Integer auditStatus;
    private String remark;
    private LocalDateTime createdAt;

    /** 子渠道列表（树形结构） */
    private List<ChannelInfoVO> children = new ArrayList<>();
}
