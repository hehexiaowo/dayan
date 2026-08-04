package com.dayan.park.dto;

import lombok.Data;

/**
 * 房型价格（park_room_price）查询入参。
 */
@Data
public class ParkRoomPriceQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private String roomTypeCode;
    private Integer priceType;
    private Integer isCurrent;
    private Integer status;
}
