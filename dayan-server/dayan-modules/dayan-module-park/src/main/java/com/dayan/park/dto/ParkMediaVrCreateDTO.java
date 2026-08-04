package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 机构 VR 创建入参。
 *
 * <p>VR 三格式（vrType：1=全景图/2=3D模型/3=视频）。
 */
@Data
public class ParkMediaVrCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "VR链接不能为空")
    @Size(max = 500)
    private String vrUrl;

    private String vrProvider;
    @Size(max = 200)
    private String vrName;
    /** VR 类型：1=全景图/2=3D模型/3=视频 */
    private Integer vrType;
    private String thumbnailUrl;
    private String vrDescription;
    private Integer sortOrder;
    private Integer status;
}
