package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.system.enums.SystemLogSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 system_log_client 对应实体（客户端日志）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_log_client")
public class SystemLogClient extends SystemLogEntry {

    @Override
    public SystemLogSource logSource() {
        return SystemLogSource.CLIENT;
    }
}
