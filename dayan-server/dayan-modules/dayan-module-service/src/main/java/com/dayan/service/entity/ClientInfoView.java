package com.dayan.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * client_info 表只读视图 POJO（跨模块轻量引用）。
 *
 * <p>service 模块不依赖 dayan-module-client（避免循环依赖、保持解耦，参考本模块
 * {@code ButlerInfoView} 模式）。展示会话时需按 clientCode 取客户姓名
 * {@code clientInfoViewMapper} 填 {@code clientName}。
 *
 * <p>注意：client_info 含 channel_code 列（租户分片表），查询时由
 * {@code TenantLineInnerInterceptor} 自动追加 channel_code 条件（admin/system 上下文放行全量）。
 */
@Data
@TableName("client_info")
public class ClientInfoView {

    /** 客户编码 */
    private String clientCode;

    /** 客户姓名 */
    private String fullName;
}
