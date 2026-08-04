package com.dayan.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 客户收货地址创建入参。
 */
@Data
public class ClientAddressCreateDTO {

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 64)
    private String clientCode;

    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 64)
    private String receiverName;

    @NotBlank(message = "收货人电话不能为空")
    @Size(max = 32)
    private String receiverPhone;

    private String provinceCode;
    private String cityCode;
    private String districtCode;

    @Size(max = 256)
    private String detailAddress;

    @Size(max = 512)
    private String fullAddress;

    /** 是否默认地址：1=是 0=否 */
    private Integer isDefault;

    @Size(max = 32)
    private String tag;
}
