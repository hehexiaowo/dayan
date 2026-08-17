package com.dayan.scene.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * park_info 表只读视图 POJO（跨模块轻量引用）。
 *
 * <p>scene 模块不依赖 dayan-module-park（避免循环依赖、保持解耦，参考 park 模块
 * {@code SupplierInfoView} / service 模块 {@code ButlerInfoView} 模式）。
 * 展示场景时需按 parkCode 取机构名称填 {@code parkName}。
 *
 * <p>park_info 为平台共享表（{@code DayanTenantHandler} 忽略 park_ 前缀），
 * 多模块映射同一张物理表不冲突，故在此建立最小只读映射。
 */
@Data
@TableName("park_info")
public class ParkInfoView {

    /** 机构编码 */
    private String parkCode;

    /** 机构名称 */
    private String fullName;
}
