package com.dayan.butler.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 管家服务记录修改入参（非空字段才更新）。
 */
@Data
public class ButlerServiceRecordUpdateDTO {

    private Integer serviceType;
    private String serviceTitle;
    private LocalDate serviceDate;
    private Integer status;
    private Integer communicateWay;
    private String remark;
}
