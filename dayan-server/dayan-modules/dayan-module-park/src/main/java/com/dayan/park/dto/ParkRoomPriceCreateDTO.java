package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 房型价格（park_room_price）创建入参。
 *
 * <p>校验：effectiveDate &lt; expireDate；isCurrent=1 时同 roomTypeCode 下唯一。
 */
@Data
public class ParkRoomPriceCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "房型编码不能为空")
    private String roomTypeCode;

    /** 价格类型：1=月 / 2=季 / 3=年 / 4=临时 */
    private Integer priceType;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private BigDecimal discountRate;
    private String priceDescription;
    private String includesItems;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    /** 是否当前生效价格（1=是） */
    private Integer isCurrent;
    private Integer isPromotion;
    private String promotionDescription;
    private String priceChangeReason;
    private Integer sortOrder;
    private Integer status;
}
