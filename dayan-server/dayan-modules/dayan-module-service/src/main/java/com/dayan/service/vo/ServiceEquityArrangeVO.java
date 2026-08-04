package com.dayan.service.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 全程安排 VO。
 */
@Data
public class ServiceEquityArrangeVO {

    private Long id;
    private String sessionCode;
    private String solutionCode;
    private String clientCode;
    private String butlerCode;
    private String arrangeCode;
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
    private LocalDateTime confirmTime;
    private LocalDateTime completeTime;
    private Integer isConfirmed;
    private Integer status;
    private String cancelReason;
    private String remark;
    private LocalDateTime createdAt;
}
