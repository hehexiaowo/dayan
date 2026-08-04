package com.dayan.butler.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管家服务记录 VO。
 */
@Data
public class ButlerServiceRecordVO {

    private Long id;
    /** 管家编码 */
    private String butlerCode;
    /** 客户编码 */
    private String clientCode;
    /** 服务类型 */
    private Integer serviceType;
    /** 服务标题 */
    private String serviceTitle;
    /** 服务日期 */
    private LocalDate serviceDate;
    /** 状态 */
    private Integer status;
    /** 沟通方式：1=电话 / 2=企微 / 3=微信 / 4=上门 / 5=其他 */
    private Integer communicateWay;
    /** 备注 */
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
