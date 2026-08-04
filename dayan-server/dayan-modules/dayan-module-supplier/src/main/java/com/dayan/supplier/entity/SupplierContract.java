package com.dayan.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 表 supplier_contract 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supplier_contract")
public class SupplierContract extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 合同编号 */
    private String contractCode;

    /** 合同名称 */
    private String contractName;

    /** 供应商编码 */
    private String supplierCode;

    /** 签约组织编码 */
    private String organCode;

    /** 合同类型 */
    private Integer contractType;

    /** 签约日期 */
    private LocalDate signDate;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 到期日期 */
    private LocalDate expireDate;

    /** 合同金额 */
    private BigDecimal contractAmount;

    /** 佣金比例 */
    private BigDecimal commissionRate;

    /** 结算周期 */
    private Integer settlementCycle;

    /** 合同条款 */
    private String terms;

    /** 合同附件URL */
    private String attachmentUrls;

    /** 签约人 */
    private String signPerson;

    /** 签约盖章图片URL */
    private String signSealImage;

    /** 是否自动续约 */
    private Integer isAutoRenew;

    /** 续约次数 */
    private Integer renewCount;

    /** 原合同编码 */
    private String parentContractCode;

    /** 状态 */
    private Integer status;

    /** 审核备注 */
    private String auditRemark;

    /** 备注 */
    private String remark;
}
