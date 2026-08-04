package com.dayan.park.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房型 VO。
 */
@Data
public class ParkRoomTypeVO {

    private Long id;
    private String parkCode;
    private String roomTypeCode;
    private String roomTypeName;
    private Integer stayType;
    private String buildingName;
    private String floor;
    private Integer roomCategory;
    private BigDecimal area;
    private String orientation;
    private Integer bedCount;
    private Integer totalRooms;
    private Integer availableRooms;

    private Integer hasBathroom;
    private Integer hasKitchen;
    private Integer hasBalcony;
    private Integer hasTv;
    private Integer hasAircon;
    private Integer hasFridge;
    private Integer hasWasher;
    private Integer hasWifi;
    private Integer hasEmergency;
    private Integer hasMonitor;

    private String facilities;
    private String description;
    private String coverImage;
    private String images;
    private Integer sortOrder;
    private Integer status;
    private String designDescription;
    private String designImage;
    private String additionalImages;
    private LocalDateTime createdAt;
}
