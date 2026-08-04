package com.dayan.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * butler_info 表只读视图 POJO（跨模块轻量引用）。
 *
 * <p>service 模块不依赖 dayan-module-butler（避免循环依赖、保持解耦，参考 park 模块
 * {@code SupplierInfoView} 模式）。分配管家时需查 butler 全名填 {@code butlerFullName} 快照、
 * 并校验 {@code status=1}（在职）。butler_info 为平台共享表（{@code DayanTenantHandler}
 * 忽略 butler_ 前缀），多模块映射同一张物理表不冲突，故在此建立最小只读映射。
 */
@Data
@TableName("butler_info")
public class ButlerInfoView {

    /** 主键 */
    private Long id;

    /** 管家编码 */
    private String butlerCode;

    /** 管家姓名 */
    private String fullName;

    /** 状态：0=离职 / 1=在职 */
    private Integer status;
}
