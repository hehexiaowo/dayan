package com.dayan.supplier.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应商合同更新入参（{@code contractCode} 不可改，由路径参数提供）。
 */
@Data
public class SupplierContractUpdateDTO {

    private String contractName;
    private String organCode;
    private Integer contractType;
    private LocalDate signDate;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private BigDecimal contractAmount;
    private BigDecimal commissionRate;
    private Integer settlementCycle;
    private String terms;
    private String attachmentUrls;
    private String signPerson;
    private String signSealImage;
    private Integer isAutoRenew;
    private Integer status;
    private String auditRemark;
    private String remark;
}
