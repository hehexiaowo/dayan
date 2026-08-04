package com.dayan.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应商合同创建入参。
 *
 * <p>{@code contractCode} 由系统生成（HT 前缀 + 5 位）。
 * 日期校验：{@code effectiveDate < expireDate}。
 */
@Data
public class SupplierContractCreateDTO {

    @NotBlank(message = "合同名称不能为空")
    private String contractName;

    @NotBlank(message = "供应商编码不能为空")
    private String supplierCode;

    private String organCode;

    /** 合同类型：1=机构入驻 / 2=商品供应 / 3=服务供应 / 4=渠道合作 */
    private Integer contractType;

    private LocalDate signDate;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private BigDecimal contractAmount;
    private BigDecimal commissionRate;

    /** 结算周期：1=月结 / 2=季结 / 3=半年结 / 4=年结 */
    private Integer settlementCycle;

    private String terms;
    private String attachmentUrls;
    private String signPerson;
    private String signSealImage;
    private Integer isAutoRenew;

    /** 续约时指向原合同编码；为空表示新签 */
    private String parentContractCode;

    private Integer status;
    private String auditRemark;
    private String remark;
}
