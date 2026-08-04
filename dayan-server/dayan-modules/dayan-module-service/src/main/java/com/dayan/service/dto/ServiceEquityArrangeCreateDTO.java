package com.dayan.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 全程安排（service_equity_arrange）创建入参。
 *
 * <p>arrangeCode(AR+10) 由服务端生成。arrangeType 6 类（1参观预约/2入住安排/3活动报名/
 * 4服务预约/5交通安排/6其他）。校验 arrangeTimeStart < arrangeTimeEnd。
 */
@Data
public class ServiceEquityArrangeCreateDTO {

    @NotBlank(message = "会话编码不能为空")
    private String sessionCode;

    /** 关联方案编码（可空） */
    private String solutionCode;

    @NotBlank(message = "客户编码不能为空")
    private String clientCode;

    private String butlerCode;

    /** 安排类型（1=参观预约, 2=入住安排, 3=活动报名, 4=服务预约, 5=交通安排, 6=其他） */
    private Integer arrangeType;

    private String parkCode;
    private String parkFullName;
    private LocalDate arrangeDate;
    private LocalTime arrangeTimeStart;
    private LocalTime arrangeTimeEnd;
    private String arrangeAddress;
    private String contactPerson;
    private String contactPhone;
    private Integer participantCount;
    private String prepareItems;
    private String remark;
}
