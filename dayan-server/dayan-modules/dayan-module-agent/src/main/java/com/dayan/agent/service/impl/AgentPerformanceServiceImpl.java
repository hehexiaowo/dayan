package com.dayan.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.agent.dto.AgentPerformanceCreateDTO;
import com.dayan.agent.dto.AgentPerformanceQueryDTO;
import com.dayan.agent.entity.AgentPerformance;
import com.dayan.agent.mapper.AgentPerformanceMapper;
import com.dayan.agent.service.AgentPerformanceService;
import com.dayan.agent.vo.AgentPerformanceSummaryVO;
import com.dayan.agent.vo.AgentPerformanceVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.mybatis.context.ContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 代理人业绩（agent_performance）服务实现。
 *
 * <p>渠道隔离：agent_performance 含 channel_code，由租户拦截器自动追加条件；
 * 同一 agent_code + period_type + period_value 唯一（uk_agent_period 兜底）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentPerformanceServiceImpl implements AgentPerformanceService {

    private final AgentPerformanceMapper performanceMapper;

    @Override
    public PageResult<AgentPerformanceVO> page(AgentPerformanceQueryDTO query) {
        LambdaQueryWrapper<AgentPerformance> wrapper = new LambdaQueryWrapper<AgentPerformance>()
                .orderByDesc(AgentPerformance::getPeriodValue)
                .orderByDesc(AgentPerformance::getId);
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.eq(AgentPerformance::getChannelCode, query.getChannelCode());
        }
        if (query.getAgentCode() != null && !query.getAgentCode().isEmpty()) {
            wrapper.eq(AgentPerformance::getAgentCode, query.getAgentCode());
        }
        if (query.getPeriodType() != null) {
            wrapper.eq(AgentPerformance::getPeriodType, query.getPeriodType());
        }
        if (query.getPeriodValue() != null && !query.getPeriodValue().isEmpty()) {
            wrapper.eq(AgentPerformance::getPeriodValue, query.getPeriodValue());
        }
        Page<AgentPerformance> page = performanceMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<AgentPerformanceVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(AgentPerformanceCreateDTO dto) {
        String channelCode = pickChannelCode(dto.getChannelCode());

        // 同 agent + period 唯一校验
        Long count = performanceMapper.selectCount(new LambdaQueryWrapper<AgentPerformance>()
                .eq(AgentPerformance::getAgentCode, dto.getAgentCode())
                .eq(AgentPerformance::getPeriodType, dto.getPeriodType())
                .eq(AgentPerformance::getPeriodValue, dto.getPeriodValue()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "该周期业绩已存在: agentCode=" + dto.getAgentCode()
                            + ", period=" + dto.getPeriodValue());
        }

        AgentPerformance entity = new AgentPerformance();
        entity.setAgentCode(dto.getAgentCode());
        entity.setChannelCode(channelCode);
        entity.setPeriodType(dto.getPeriodType());
        entity.setPeriodValue(dto.getPeriodValue());
        entity.setEquityGrantCount(dto.getEquityGrantCount() == null ? 0 : dto.getEquityGrantCount());
        entity.setEquityGrantAmount(dto.getEquityGrantAmount() == null ? BigDecimal.ZERO : dto.getEquityGrantAmount());
        entity.setSceneOrderCount(dto.getSceneOrderCount() == null ? 0 : dto.getSceneOrderCount());
        entity.setSceneOrderAmount(dto.getSceneOrderAmount() == null ? BigDecimal.ZERO : dto.getSceneOrderAmount());
        entity.setCourseOrderCount(dto.getCourseOrderCount() == null ? 0 : dto.getCourseOrderCount());
        entity.setCourseOrderAmount(dto.getCourseOrderAmount() == null ? BigDecimal.ZERO : dto.getCourseOrderAmount());
        performanceMapper.insert(entity);
        log.info("创建代理人业绩: agentCode={}, period={}", dto.getAgentCode(), dto.getPeriodValue());
    }

    @Override
    public List<AgentPerformanceVO> listByAgent(String agentCode) {
        List<AgentPerformance> list = performanceMapper.selectList(new LambdaQueryWrapper<AgentPerformance>()
                .eq(AgentPerformance::getAgentCode, agentCode)
                .orderByDesc(AgentPerformance::getPeriodValue));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public AgentPerformanceSummaryVO summary(String agentCode) {
        List<AgentPerformance> list = performanceMapper.selectList(new LambdaQueryWrapper<AgentPerformance>()
                .eq(AgentPerformance::getAgentCode, agentCode));
        AgentPerformanceSummaryVO vo = new AgentPerformanceSummaryVO();
        vo.setAgentCode(agentCode);
        vo.setRecordCount((long) list.size());
        long equityCount = 0L;
        long sceneCount = 0L;
        long courseCount = 0L;
        BigDecimal equityAmount = BigDecimal.ZERO;
        BigDecimal sceneAmount = BigDecimal.ZERO;
        BigDecimal courseAmount = BigDecimal.ZERO;
        for (AgentPerformance p : list) {
            if (p.getEquityGrantCount() != null) equityCount += p.getEquityGrantCount();
            if (p.getSceneOrderCount() != null) sceneCount += p.getSceneOrderCount();
            if (p.getCourseOrderCount() != null) courseCount += p.getCourseOrderCount();
            if (p.getEquityGrantAmount() != null) equityAmount = equityAmount.add(p.getEquityGrantAmount());
            if (p.getSceneOrderAmount() != null) sceneAmount = sceneAmount.add(p.getSceneOrderAmount());
            if (p.getCourseOrderAmount() != null) courseAmount = courseAmount.add(p.getCourseOrderAmount());
        }
        vo.setTotalEquityGrantCount(equityCount);
        vo.setTotalSceneOrderCount(sceneCount);
        vo.setTotalCourseOrderCount(courseCount);
        vo.setTotalEquityGrantAmount(equityAmount);
        vo.setTotalSceneOrderAmount(sceneAmount);
        vo.setTotalCourseOrderAmount(courseAmount);
        return vo;
    }

    private String pickChannelCode(String fromDto) {
        if (fromDto != null && !fromDto.isEmpty()) {
            return fromDto;
        }
        String ctx = ContextHolder.getChannelCode();
        if (ctx == null || ctx.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "渠道编码不能为空");
        }
        return ctx;
    }

    private AgentPerformanceVO toVO(AgentPerformance entity) {
        AgentPerformanceVO vo = new AgentPerformanceVO();
        vo.setId(entity.getId());
        vo.setAgentCode(entity.getAgentCode());
        vo.setChannelCode(entity.getChannelCode());
        vo.setPeriodType(entity.getPeriodType());
        vo.setPeriodValue(entity.getPeriodValue());
        vo.setEquityGrantCount(entity.getEquityGrantCount());
        vo.setEquityGrantAmount(entity.getEquityGrantAmount());
        vo.setSceneOrderCount(entity.getSceneOrderCount());
        vo.setSceneOrderAmount(entity.getSceneOrderAmount());
        vo.setCourseOrderCount(entity.getCourseOrderCount());
        vo.setCourseOrderAmount(entity.getCourseOrderAmount());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
