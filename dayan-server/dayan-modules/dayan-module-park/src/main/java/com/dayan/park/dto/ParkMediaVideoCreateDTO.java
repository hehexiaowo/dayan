package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 机构视频创建入参。
 */
@Data
public class ParkMediaVideoCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "视频URL不能为空")
    @Size(max = 500)
    private String videoUrl;

    @Size(max = 500)
    private String coverUrl;
    @Size(max = 200)
    private String videoName;
    private Integer videoType;
    private String videoDescription;
    private Integer duration;
    private Integer fileSize;
    private Integer sortOrder;
    private Integer status;
}
