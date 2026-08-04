package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * supplier_info 表只读视图 POJO（跨模块轻量引用）。
 *
 * <p>park 模块不依赖 dayan-module-supplier（避免循环依赖），但需校验
 * {@code park_info.supplier_code} 关联的供应商存在且 status=2（已通过）。
 * supplier_info 为平台共享表（{@code DayanTenantHandler} 忽略 supplier_ 前缀），
 * 多模块映射同一张物理表不冲突，故在此建立最小只读映射，仅含校验所需字段。
 */
@Data
@TableName("supplier_info")
public class SupplierInfoView {

    /** 主键 */
    private Long id;

    /** 供应商编码 */
    private String supplierCode;

    /** 审核状态：1=待审核 / 2=已通过 / 3=已驳回 */
    private Integer status;
}
