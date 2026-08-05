package com.dayan.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.statemachine.StateMachineEngine;
import com.dayan.system.entity.SystemStateMachine;
import com.dayan.system.mapper.SystemStateMachineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 状态机规则配置服务。
 *
 * <p>负责 system_state_machine 表的 CRUD，写操作后刷新引擎缓存，
 * 保证运营在 Admin 端的配置变更立即对业务生效。
 */
@Service
@RequiredArgsConstructor
public class SystemStateMachineService {

    private final SystemStateMachineMapper stateMachineMapper;
    private final StateMachineEngine stateMachineEngine;

    /**
     * 分页查询（按 machineCode / bizType 过滤）。
     */
    public PageResult<SystemStateMachine> page(long current, long size, String machineCode, String bizType) {
        LambdaQueryWrapper<SystemStateMachine> wrapper = new LambdaQueryWrapper<SystemStateMachine>()
                .orderByAsc(SystemStateMachine::getSortOrder)
                .orderByAsc(SystemStateMachine::getFromState);
        if (machineCode != null && !machineCode.isEmpty()) {
            wrapper.eq(SystemStateMachine::getMachineCode, machineCode);
        }
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(SystemStateMachine::getBizType, bizType);
        }
        Page<SystemStateMachine> page = stateMachineMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(current, size, page.getTotal(), page.getRecords());
    }

    /**
     * 按 id 查单条。
     */
    public SystemStateMachine getById(Long id) {
        SystemStateMachine entity = stateMachineMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "状态机规则不存在: " + id);
        }
        return entity;
    }

    /**
     * 新增规则。
     *
     * <p>唯一性校验对应 DDL 唯一键 uk_machine_from_event(machine_code, from_state, from_sub_state, event_code)。
     * 新增后刷新该 machineCode 的缓存。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemStateMachine entity) {
        checkUnique(entity);
        stateMachineMapper.insert(entity);
        stateMachineEngine.refreshRules(entity.getMachineCode());
        return entity.getId();
    }

    /**
     * 修改规则。
     *
     * <p>若 machineCode 被修改，旧域和新域的缓存都要刷新（旧域规则可能减少，新域规则增加）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemStateMachine entity) {
        SystemStateMachine existing = stateMachineMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "状态机规则不存在: " + id);
        }
        String oldMachineCode = existing.getMachineCode();
        entity.setId(id);
        stateMachineMapper.updateById(entity);
        // 当前 machineCode 域一定刷新
        stateMachineEngine.refreshRules(entity.getMachineCode());
        // 若 machineCode 变更，旧域也要刷新（旧域规则减少）
        if (entity.getMachineCode() != null && !entity.getMachineCode().equals(oldMachineCode)) {
            stateMachineEngine.refreshRules(oldMachineCode);
        }
    }

    /**
     * 删除规则。
     *
     * <p>删除前先查出 machineCode，删除后刷新该域缓存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SystemStateMachine existing = stateMachineMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "状态机规则不存在: " + id);
        }
        stateMachineMapper.deleteById(id);
        stateMachineEngine.refreshRules(existing.getMachineCode());
    }

    /**
     * 唯一性校验：(machineCode, fromState, fromSubState, eventCode) 组合不可重复。
     */
    private void checkUnique(SystemStateMachine entity) {
        LambdaQueryWrapper<SystemStateMachine> wrapper = new LambdaQueryWrapper<SystemStateMachine>()
                .eq(SystemStateMachine::getMachineCode, entity.getMachineCode())
                .eq(SystemStateMachine::getFromState, entity.getFromState())
                .eq(SystemStateMachine::getEventCode, entity.getEventCode());
        // fromSubState 可能为 null，MyBatis-Plus 的 eq(null) 会生成 IS NULL
        if (entity.getFromSubState() == null || entity.getFromSubState().isEmpty()) {
            wrapper.and(w -> w.isNull(SystemStateMachine::getFromSubState)
                    .or().eq(SystemStateMachine::getFromSubState, ""));
        } else {
            wrapper.eq(SystemStateMachine::getFromSubState, entity.getFromSubState());
        }
        Long count = stateMachineMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "规则已存在: machineCode=" + entity.getMachineCode()
                            + ", fromState=" + entity.getFromState()
                            + ", eventCode=" + entity.getEventCode());
        }
    }
}
