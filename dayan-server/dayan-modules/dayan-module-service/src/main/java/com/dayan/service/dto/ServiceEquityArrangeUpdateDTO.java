package com.dayan.service.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 全程安排（service_equity_arrange）更新入参（按 id 更新）。
 */
@Data
public class ServiceEquityArrangeUpdateDTO {

    private String solutionCode;
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
    private String progressNotes;
    private Integer status;
    private String cancelReason;
    private String remark;
}
