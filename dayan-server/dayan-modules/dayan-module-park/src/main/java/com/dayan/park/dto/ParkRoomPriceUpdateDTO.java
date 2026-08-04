package com.dayan.park.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 房型价格（park_room_price）更新入参。
 */
@Data
public class ParkRoomPriceUpdateDTO {

    private Integer priceType;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private BigDecimal discountRate;
    private String priceDescription;
    private String includesItems;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private Integer isCurrent;
    private Integer isPromotion;
    private String promotionDescription;
    private String priceChangeReason;
    private Integer sortOrder;
    private Integer status;
}
