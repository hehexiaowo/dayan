package com.dayan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.system.entity.SystemOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * system_operation_log 数据访问层。
 */
@Mapper
public interface SystemOperationLogMapper extends BaseMapper<SystemOperationLog> {
}
