package com.dayan.park.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构视频 VO。
 */
@Data
public class ParkMediaVideoVO {

    private Long id;
    private String parkCode;
    private String videoUrl;
    private String coverUrl;
    private String videoName;
    private Integer videoType;
    private String videoDescription;
    private Integer duration;
    private Integer fileSize;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
