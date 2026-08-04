package com.dayan.client.dto;

import lombok.Data;

/**
 * 客户收货地址更新入参。
 */
@Data
public class ClientAddressUpdateDTO {

    private String receiverName;
    private String receiverPhone;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String detailAddress;
    private String fullAddress;
    private Integer isDefault;
    private String tag;
}
