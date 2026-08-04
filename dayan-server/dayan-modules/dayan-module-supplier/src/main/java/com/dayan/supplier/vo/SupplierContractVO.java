package com.dayan.supplier.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商合同视图对象。
 */
@Data
public class SupplierContractVO {

    private Long id;
    private String contractCode;
    private String contractName;
    private String supplierCode;
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
    private Integer renewCount;
    private String parentContractCode;
    private Integer status;
    private String auditRemark;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
