package com.dayan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.system.entity.SystemMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * system_message 数据访问层。
 */
@Mapper
public interface SystemMessageMapper extends BaseMapper<SystemMessage> {
}
