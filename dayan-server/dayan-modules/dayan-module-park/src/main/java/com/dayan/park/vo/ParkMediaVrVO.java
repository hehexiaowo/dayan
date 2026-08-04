package com.dayan.park.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构 VR VO。
 */
@Data
public class ParkMediaVrVO {

    private Long id;
    private String parkCode;
    private String vrUrl;
    private String vrProvider;
    private String vrName;
    private Integer vrType;
    private String thumbnailUrl;
    private String vrDescription;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
