package com.dayan.park.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构文件 VO。
 */
@Data
public class ParkMediaFileVO {

    private Long id;
    private String parkCode;
    private String fileUrl;
    private String fileName;
    private Integer fileType;
    private String fileFormat;
    private Integer fileSize;
    private String fileDescription;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
