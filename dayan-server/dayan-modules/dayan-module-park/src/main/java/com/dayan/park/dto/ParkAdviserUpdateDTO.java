package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构顾问更新入参。
 */
@Data
public class ParkAdviserUpdateDTO {

    private String adviserName;
    private String adviserTitle;
    private String adviserImage;
    private String adviserContent;
    private String contactPhone;
    private Integer isPrimary;
    private Integer sortOrder;
    private Integer status;
}
