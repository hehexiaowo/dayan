package com.dayan.park.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 房型价格 VO。
 */
@Data
public class ParkRoomPriceVO {

    private Long id;
    private String parkCode;
    private String roomTypeCode;
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
    private LocalDateTime createdAt;
}
