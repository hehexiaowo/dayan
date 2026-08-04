package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构视频更新入参（按字段非空更新）。
 */
@Data
public class ParkMediaVideoUpdateDTO {

    private String videoUrl;
    private String coverUrl;
    private String videoName;
    private Integer videoType;
    private String videoDescription;
    private Integer duration;
    private Integer fileSize;
    private Integer sortOrder;
    private Integer status;
}
