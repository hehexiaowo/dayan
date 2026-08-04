package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 房型（park_room_type）创建入参。
 *
 * <p>校验：{@code totalRooms >= availableRooms}（若两者皆提供）。
 */
@Data
public class ParkRoomTypeCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "房型编码不能为空")
    @Size(max = 50)
    private String roomTypeCode;

    @NotBlank(message = "房型名称不能为空")
    @Size(max = 200)
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
}
