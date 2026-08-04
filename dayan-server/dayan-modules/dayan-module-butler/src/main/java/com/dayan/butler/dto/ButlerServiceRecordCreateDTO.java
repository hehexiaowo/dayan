package com.dayan.butler.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 管家服务记录创建入参。
 */
@Data
public class ButlerServiceRecordCreateDTO {

    @NotBlank(message = "管家编码不能为空")
    private String butlerCode;

    @NotBlank(message = "客户编码不能为空")
    private String clientCode;

    /** 服务类型 */
    private Integer serviceType;

    private String serviceTitle;

    private LocalDate serviceDate;

    /** 状态，默认 1 */
    private Integer status;

    /** 沟通方式：1=电话 / 2=企微 / 3=微信 / 4=上门 / 5=其他 */
    private Integer communicateWay;

    private String remark;
}
