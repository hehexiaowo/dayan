package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 机构文件创建入参。
 */
@Data
public class ParkMediaFileCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "文件URL不能为空")
    @Size(max = 500)
    private String fileUrl;

    @Size(max = 200)
    private String fileName;
    private Integer fileType;
    private String fileFormat;
    private Integer fileSize;
    private String fileDescription;
    private Integer sortOrder;
    private Integer status;
}
