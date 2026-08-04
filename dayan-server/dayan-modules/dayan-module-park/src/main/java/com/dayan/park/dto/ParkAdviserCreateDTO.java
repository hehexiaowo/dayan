package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 机构顾问创建入参。
 *
 * <p>isPrimary=1 时，同 parkCode 下其他顾问的 isPrimary 会被自动置 0（主顾问唯一）。
 */
@Data
public class ParkAdviserCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "顾问姓名不能为空")
    @Size(max = 100)
    private String adviserName;

    private String adviserTitle;
    private String adviserImage;
    private String adviserContent;
    private String contactPhone;
    /** 是否首席顾问（1=是） */
    private Integer isPrimary;
    private Integer sortOrder;
    private Integer status;
}
