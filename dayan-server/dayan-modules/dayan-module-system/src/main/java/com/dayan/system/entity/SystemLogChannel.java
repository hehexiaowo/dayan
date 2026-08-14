package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.system.enums.SystemLogSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 system_log_channel 对应实体（渠道端日志）。
 *
 * <p>39 号迁移起重建为四端统一 schema（继承 {@link SystemLogEntry}）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_log_channel")
public class SystemLogChannel extends SystemLogEntry {

    @Override
    public SystemLogSource logSource() {
        return SystemLogSource.CHANNEL;
    }
}
