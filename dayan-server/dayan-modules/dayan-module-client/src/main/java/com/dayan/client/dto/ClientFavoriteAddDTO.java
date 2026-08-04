package com.dayan.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 客户收藏新增入参。
 */
@Data
public class ClientFavoriteAddDTO {

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 64)
    private String clientCode;

    @NotNull(message = "收藏对象类型不能为空")
    private Integer targetType;

    @NotBlank(message = "收藏对象编码不能为空")
    @Size(max = 64)
    private String targetCode;

    @Size(max = 200)
    private String targetName;

    @Size(max = 255)
    private String remark;
}
