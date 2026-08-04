package com.dayan.park.dto;

import lombok.Data;

/**
 * 房型（park_room_type）查询入参。
 */
@Data
public class ParkRoomTypeQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private String roomTypeCode;
    private String roomTypeName;
    private Integer roomCategory;
    private Integer stayType;
    private Integer status;
}
