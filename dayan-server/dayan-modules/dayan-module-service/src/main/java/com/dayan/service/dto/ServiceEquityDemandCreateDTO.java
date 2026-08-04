package com.dayan.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 需求收集（service_equity_demand）创建入参。
 *
 * <p>demandCode(DM+10) 由服务端生成。demandType 5 类需求（1机构入住/2日间照料/3居家护理/
 * 4场景活动/5旅居）。校验 budgetMin ≤ budgetMax。
 */
@Data
public class ServiceEquityDemandCreateDTO {

    @NotBlank(message = "会话编码不能为空")
    private String sessionCode;

    @NotBlank(message = "客户编码不能为空")
    private String clientCode;

    private String butlerCode;

    /** 需求类型（1=机构入住, 2=日间照料, 3=居家护理, 4=场景活动, 5=旅居） */
    private Integer demandType;

    private String usePersonName;
    private Integer usePersonAge;
    private Integer usePersonGender;
    private String healthSummary;
    private Integer careLevelNeed;
    private String cityPreference;
    private String areaPreference;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String roomPreference;
    private String foodPreference;
    private String specialNeeds;
    private LocalDate expectedTime;
    private Integer contactPreference;
    private Integer collectMethod;
    private String demandSummary;
    private String demandImages;
    private String remark;
}
