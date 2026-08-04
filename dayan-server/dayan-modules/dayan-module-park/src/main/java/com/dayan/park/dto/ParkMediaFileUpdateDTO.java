package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构文件更新入参。
 */
@Data
public class ParkMediaFileUpdateDTO {

    private String fileUrl;
    private String fileName;
    private Integer fileType;
    private String fileFormat;
    private Integer fileSize;
    private String fileDescription;
    private Integer sortOrder;
    private Integer status;
}
