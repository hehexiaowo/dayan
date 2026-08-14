package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.system.enums.SystemLogSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 system_log_organ 对应实体（管理后台日志：admin 端全部操作/登录日志）。
 *
 * <p>39 号迁移起重建为四端统一 schema（继承 {@link SystemLogEntry}），
 * supplier/distributor/system/unknown 账号的日志也落入本表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_log_organ")
public class SystemLogOrgan extends SystemLogEntry {

    @Override
    public SystemLogSource logSource() {
        return SystemLogSource.ORGAN;
    }
}
