package com.dayan.client.dto;

import lombok.Data;

/**
 * 客户信息查询入参。
 */
@Data
public class ClientInfoQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    /** 必填：渠道编码（按渠道隔离） */
    private String channelCode;

    private String clientCode;
    private String fullName;
    private String phone;
    private Integer gender;
    private Integer clientLevel;
    private Integer isVip;
    private Integer status;
    private Integer sourceType;
}
