package com.dayan.system.statemachine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.statemachine.StateRule;
import com.dayan.common.core.statemachine.StateRuleLoader;
import com.dayan.system.entity.SystemStateMachine;
import com.dayan.system.mapper.SystemStateMachineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 状态机规则数据源实现（system 域）。
 *
 * <p>读取 {@code system_state_machine} 表，转换为通用 {@link StateRule}，
 * 供 {@code DefaultStateMachineEngine} 加载到 Redis 缓存。
 */
@Component
@RequiredArgsConstructor
public class SystemStateRuleLoader implements StateRuleLoader {

    private final SystemStateMachineMapper stateMachineMapper;

    @Override
    public List<StateRule> loadByDomain(String domain) {
        List<SystemStateMachine> rows = stateMachineMapper.selectList(
                new LambdaQueryWrapper<SystemStateMachine>()
                        .eq(SystemStateMachine::getMachineCode, domain)
                        .eq(SystemStateMachine::getStatus, 1));
        return rows.stream().map(this::toRule).collect(Collectors.toList());
    }

    @Override
    public List<StateRule> loadAll() {
        List<SystemStateMachine> rows = stateMachineMapper.selectList(
                new LambdaQueryWrapper<SystemStateMachine>()
                        .eq(SystemStateMachine::getStatus, 1));
        return rows.stream().map(this::toRule).collect(Collectors.toList());
    }

    private StateRule toRule(SystemStateMachine sm) {
        // 字段映射：machine_code→domain, from_state→fromStatus, event_code→event, to_state→toStatus
        return new StateRule(
                sm.getMachineCode(),
                sm.getFromState(),
                sm.getEventCode(),
                sm.getToState(),
                sm.getRemark());
    }
}
