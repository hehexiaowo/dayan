package com.dayan.client.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户收货地址 VO。
 */
@Data
public class ClientAddressVO {

    private Long id;
    private String clientCode;
    private String receiverName;
    private String receiverPhone;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String detailAddress;
    private String fullAddress;
    private Integer isDefault;
    private String tag;
    private LocalDateTime createdAt;
}
