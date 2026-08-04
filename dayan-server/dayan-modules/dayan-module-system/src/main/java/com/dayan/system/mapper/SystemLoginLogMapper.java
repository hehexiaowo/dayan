package com.dayan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.system.entity.SystemLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * system_login_log 数据访问层。
 */
@Mapper
public interface SystemLoginLogMapper extends BaseMapper<SystemLoginLog> {
}
