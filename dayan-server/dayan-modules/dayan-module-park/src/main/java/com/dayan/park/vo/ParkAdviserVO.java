package com.dayan.park.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构顾问 VO。
 */
@Data
public class ParkAdviserVO {

    private Long id;
    private String parkCode;
    private String adviserName;
    private String adviserTitle;
    private String adviserImage;
    private String adviserContent;
    private String contactPhone;
    private Integer isPrimary;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
