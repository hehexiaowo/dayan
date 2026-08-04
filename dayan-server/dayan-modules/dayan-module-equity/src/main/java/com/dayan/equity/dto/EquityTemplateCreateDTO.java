package com.dayan.equity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 权益模板创建入参。
 *
 * <p>{@code templateCode} 由系统生成（ET+5 位，{@code SequenceProvider}）。
 */
@Data
public class EquityTemplateCreateDTO {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100)
    private String templateName;

    /** 权益类型：1=机构入住/2=机构参观/3=场景活动/4=居家护理/5=健康检测/6=课程学习/7=旅居体验 */
    @NotNull(message = "权益类型不能为空")
    private Integer equityType;

    /** 权益等级：1=基础/2=标准/3=高级/4=尊享/5=定制 */
    private Integer equityLevel;

    private BigDecimal equityValue;
    private BigDecimal costPrice;
    private String contentDescription;
    private String serviceItems;
    private String applicableParks;
    private String applicableCities;

    /** 激活后有效天数 */
    private Integer validDays;
    /** 库存有效期天数 */
    private Integer shelfLifeDays;

    private Integer isTransferable;
    private Integer isStackable;
    private Integer maxUseCount;

    private String coverImage;
    private String cardDesignUrl;
    private String terms;
    private Integer sortOrder;

    /** 状态：0=停用/1=启用/2=已下架（默认 1 启用） */
    private Integer status;

    private String remark;
}
