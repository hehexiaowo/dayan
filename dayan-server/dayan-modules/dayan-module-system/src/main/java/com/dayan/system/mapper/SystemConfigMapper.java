package com.dayan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.system.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * system_config 数据访问层。
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {
}
