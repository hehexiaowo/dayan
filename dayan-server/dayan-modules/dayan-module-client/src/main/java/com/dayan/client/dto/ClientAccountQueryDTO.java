package com.dayan.client.dto;

import lombok.Data;

/**
 * 客户账号查询入参。
 */
@Data
public class ClientAccountQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    /** 必填：渠道编码（按渠道隔离） */
    private String channelCode;

    private String clientCode;
    private String username;
    private String phone;
    private Integer accountStatus;
}
