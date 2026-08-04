package com.dayan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.system.entity.SystemStateMachine;
import org.apache.ibatis.annotations.Mapper;

/**
 * system_state_machine 数据访问层。
 */
@Mapper
public interface SystemStateMachineMapper extends BaseMapper<SystemStateMachine> {
}
