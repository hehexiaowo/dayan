package com.dayan.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.agent.entity.AgentLead;
import com.dayan.agent.entity.AgentLeadTrace;
import com.dayan.agent.mapper.AgentLeadMapper;
import com.dayan.agent.mapper.AgentLeadTraceMapper;
import com.dayan.agent.service.AgentLeadTraceService;
import com.dayan.agent.vo.AgentLeadTraceVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 线索互动追踪服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentLeadTraceServiceImpl implements AgentLeadTraceService {

    private final AgentLeadMapper leadMapper;
    private final AgentLeadTraceMapper traceMapper;

    private static final DateTimeFormatter CODE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String trackVisit(String agentCode, int shareType, String bizCode,
                             String bizTitle, String visitorToken, String visitorSource) {
        // 1. 生成或复用 visitorToken
        String token = StringUtils.hasText(visitorToken) ? visitorToken : UUID.randomUUID().toString().replace("-", "");

        // 2. 按 (agent_code + visitor_token) 查找现有线索
        AgentLead lead = leadMapper.selectOne(new LambdaQueryWrapper<AgentLead>()
                .eq(AgentLead::getAgentCode, agentCode)
                .eq(AgentLead::getVisitorToken, token)
                .last("LIMIT 1"));

        // 3. 不存在则创建新线索
        if (lead == null) {
            lead = createLeadFromVisit(agentCode, token, visitorSource);
            log.info("[Trace] 新访客线索: leadCode={}, agentCode={}, token={}", lead.getLeadCode(), agentCode, token);
        }

        // 4. 创建互动记录
        AgentLeadTrace trace = new AgentLeadTrace();
        trace.setLeadId(lead.getId());
        trace.setAgentCode(agentCode);
        trace.setChannelCode(lead.getChannelCode());
        trace.setTraceType(shareType);
        trace.setBizCode(bizCode != null ? bizCode : "");
        trace.setBizTitle(bizTitle);
        trace.setTraceTime(LocalDateTime.now());
        traceMapper.insert(trace);

        // 5. 更新线索互动统计
        lead.setLastTraceTime(LocalDateTime.now());
        lead.setTraceCount((lead.getTraceCount() != null ? lead.getTraceCount() : 0) + 1);
        leadMapper.updateById(lead);

        return token;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveContact(String visitorToken, String phone, String name) {
        if (!StringUtils.hasText(visitorToken) || !StringUtils.hasText(phone)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "访客令牌和手机号不能为空");
        }

        AgentLead lead = leadMapper.selectOne(new LambdaQueryWrapper<AgentLead>()
                .eq(AgentLead::getVisitorToken, visitorToken)
                .last("LIMIT 1"));

        if (lead == null) {
            log.warn("[Trace] 留资失败：未找到 visitorToken={}", visitorToken);
            return;
        }

        lead.setPhone(phone);
        if (StringUtils.hasText(name)) {
            lead.setName(name);
        }
        leadMapper.updateById(lead);
        log.info("[Trace] 客户留资: leadCode={}, phone={}", lead.getLeadCode(), phone);
    }

    @Override
    public List<AgentLeadTraceVO> listByLeadId(Long leadId, String agentCode) {
        // 归属校验
        AgentLead lead = leadMapper.selectById(leadId);
        if (lead == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "线索不存在");
        }
        if (!agentCode.equals(lead.getAgentCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此线索");
        }

        List<AgentLeadTrace> traces = traceMapper.selectList(new LambdaQueryWrapper<AgentLeadTrace>()
                .eq(AgentLeadTrace::getLeadId, leadId)
                .orderByDesc(AgentLeadTrace::getTraceTime)
                .last("LIMIT 50"));

        return traces.stream().map(this::toVO).toList();
    }

    // ===== 内部方法 =====

    /**
     * 创建访客线索（自动生成，source_type=2 分享扫码）。
     */
    private AgentLead createLeadFromVisit(String agentCode, String visitorToken, String visitorSource) {
        // 查代理人的 channelCode
        AgentLead proto = leadMapper.selectOne(new LambdaQueryWrapper<AgentLead>()
                .eq(AgentLead::getAgentCode, agentCode)
                .last("LIMIT 1"));
        String channelCode = (proto != null && proto.getChannelCode() != null) ? proto.getChannelCode() : "";

        AgentLead lead = new AgentLead();
        lead.setAgentCode(agentCode);
        lead.setChannelCode(channelCode);
        lead.setLeadCode(generateLeadCode(channelCode));
        lead.setName("匿名访客");
        lead.setGender(0);
        lead.setLeadStatus(1); // 新线索
        lead.setSourceType(2); // 分享扫码
        lead.setVisitorToken(visitorToken);
        lead.setVisitorSource(StringUtils.hasText(visitorSource) ? visitorSource : "unknown");
        lead.setTraceCount(0);
        leadMapper.insert(lead);
        return lead;
    }

    /**
     * 生成线索编码：LD + yyyyMMdd + 4位序号。
     */
    private String generateLeadCode(String channelCode) {
        String dateStr = LocalDateTime.now().format(CODE_DATE_FMT);
        Long count = leadMapper.selectCount(new LambdaQueryWrapper<AgentLead>()
                .eq(AgentLead::getChannelCode, channelCode != null ? channelCode : "")
                .likeRight(AgentLead::getLeadCode, "LD" + dateStr));
        long seq = (count == null ? 0 : count) + 1;
        return String.format("LD%s%04d", dateStr, seq);
    }

    private AgentLeadTraceVO toVO(AgentLeadTrace trace) {
        AgentLeadTraceVO vo = new AgentLeadTraceVO();
        vo.setId(trace.getId());
        vo.setTraceType(trace.getTraceType());
        vo.setBizCode(trace.getBizCode());
        vo.setBizTitle(trace.getBizTitle());
        vo.setTraceTime(trace.getTraceTime());
        return vo;
    }
}
