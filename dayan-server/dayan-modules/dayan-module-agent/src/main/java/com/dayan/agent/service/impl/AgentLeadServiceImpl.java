package com.dayan.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.agent.dto.AgentLeadCreateDTO;
import com.dayan.agent.dto.AgentLeadQueryDTO;
import com.dayan.agent.dto.AgentLeadUpdateDTO;
import com.dayan.agent.entity.AgentLead;
import com.dayan.agent.mapper.AgentLeadMapper;
import com.dayan.agent.service.AgentLeadService;
import com.dayan.agent.vo.AgentLeadVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.mybatis.context.ContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 代理人线索服务实现。
 *
 * <p>agent_lead 为渠道分片表，所有查询强制按 agentCode（当前登录代理人）+ channelCode 过滤，
 * 确保代理人只能操作自己的线索。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentLeadServiceImpl implements AgentLeadService {

    private final AgentLeadMapper leadMapper;

    private static final DateTimeFormatter CODE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public PageResult<AgentLeadVO> page(AgentLeadQueryDTO query) {
        String agentCode = requireCurrentAgentCode();

        LambdaQueryWrapper<AgentLead> wrapper = new LambdaQueryWrapper<AgentLead>()
                .eq(AgentLead::getAgentCode, agentCode)
                .orderByDesc(AgentLead::getLastTraceTime)
                .orderByDesc(AgentLead::getCreatedAt);

        // keyword: 模糊搜索姓名/手机号
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(AgentLead::getName, kw)
                    .or().like(AgentLead::getPhone, kw));
        }
        if (query.getLeadStatus() != null) {
            wrapper.eq(AgentLead::getLeadStatus, query.getLeadStatus());
        }
        if (query.getSourceType() != null) {
            wrapper.eq(AgentLead::getSourceType, query.getSourceType());
        }
        if (query.getIntentionLevel() != null) {
            wrapper.eq(AgentLead::getIntentionLevel, query.getIntentionLevel());
        }

        Page<AgentLead> page = leadMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<AgentLeadVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AgentLeadCreateDTO dto) {
        String agentCode = requireCurrentAgentCode();
        String channelCode = ContextHolder.getChannelCode();

        AgentLead lead = new AgentLead();
        lead.setAgentCode(agentCode);
        lead.setChannelCode(channelCode != null ? channelCode : "");
        lead.setLeadCode(generateLeadCode(channelCode));
        lead.setName(dto.getName());
        lead.setPhone(dto.getPhone());
        lead.setGender(dto.getGender() != null ? dto.getGender() : 0);
        lead.setAge(dto.getAge());
        lead.setLeadStatus(1); // 新线索
        lead.setSourceType(dto.getSourceType() != null ? dto.getSourceType() : 1);
        lead.setIntentionLevel(dto.getIntentionLevel());
        lead.setInterestType(dto.getInterestType());
        lead.setRegion(dto.getRegion());
        lead.setRemark(dto.getRemark());

        leadMapper.insert(lead);
        log.info("[Lead] 新增线索: leadCode={}, agentCode={}", lead.getLeadCode(), agentCode);
        return lead.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long leadId, AgentLeadUpdateDTO dto) {
        String agentCode = requireCurrentAgentCode();

        AgentLead lead = leadMapper.selectById(leadId);
        if (lead == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "线索不存在");
        }
        // 归属校验：只能修改自己的线索
        if (!agentCode.equals(lead.getAgentCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改此线索");
        }

        // 选择性更新
        if (dto.getName() != null) lead.setName(dto.getName());
        if (dto.getPhone() != null) lead.setPhone(dto.getPhone());
        if (dto.getGender() != null) lead.setGender(dto.getGender());
        if (dto.getAge() != null) lead.setAge(dto.getAge());
        if (dto.getLeadStatus() != null) {
            lead.setLeadStatus(dto.getLeadStatus());
            // 状态变为「跟进中」时更新最后跟进时间
            if (dto.getLeadStatus() == 2) {
                lead.setLastFollowTime(LocalDateTime.now());
            }
        }
        if (dto.getIntentionLevel() != null) lead.setIntentionLevel(dto.getIntentionLevel());
        if (dto.getInterestType() != null) lead.setInterestType(dto.getInterestType());
        if (dto.getRegion() != null) lead.setRegion(dto.getRegion());
        if (dto.getRemark() != null) lead.setRemark(dto.getRemark());

        leadMapper.updateById(lead);
        log.info("[Lead] 更新线索: leadId={}, leadStatus={}", leadId, lead.getLeadStatus());
    }

    @Override
    public AgentLeadVO detail(Long leadId) {
        String agentCode = requireCurrentAgentCode();

        AgentLead lead = leadMapper.selectById(leadId);
        if (lead == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "线索不存在");
        }
        if (!agentCode.equals(lead.getAgentCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此线索");
        }
        return toVO(lead);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long leadId) {
        String agentCode = requireCurrentAgentCode();

        AgentLead lead = leadMapper.selectById(leadId);
        if (lead == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "线索不存在");
        }
        if (!agentCode.equals(lead.getAgentCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此线索");
        }
        // BaseEntity @TableLogic 自动软删除（deleted=1）
        leadMapper.deleteById(leadId);
        log.info("[Lead] 删除线索: leadId={}, leadCode={}", leadId, lead.getLeadCode());
    }

    // ===== 内部方法 =====

    /**
     * 从 Sa-Token 上下文获取当前代理人编码。
     */
    private String requireCurrentAgentCode() {
        String agentCode = ContextHolder.getAccountCode();
        if (!StringUtils.hasText(agentCode)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return agentCode;
    }

    /**
     * 生成线索编码：LD + yyyyMMdd + 4位序号（渠道内自增）。
     * 序号取当天该渠道已有线索数 + 1，并发量低时足够可靠。
     */
    private String generateLeadCode(String channelCode) {
        String dateStr = LocalDateTime.now().format(CODE_DATE_FMT);
        Long todayCount = leadMapper.selectCount(new LambdaQueryWrapper<AgentLead>()
                .eq(AgentLead::getChannelCode, channelCode != null ? channelCode : "")
                .likeRight(AgentLead::getLeadCode, "LD" + dateStr));
        long seq = (todayCount == null ? 0 : todayCount) + 1;
        return String.format("LD%s%04d", dateStr, seq);
    }

    private AgentLeadVO toVO(AgentLead lead) {
        AgentLeadVO vo = new AgentLeadVO();
        vo.setId(lead.getId());
        vo.setLeadCode(lead.getLeadCode());
        vo.setAgentCode(lead.getAgentCode());
        vo.setChannelCode(lead.getChannelCode());
        vo.setName(lead.getName());
        vo.setPhone(lead.getPhone());
        vo.setGender(lead.getGender());
        vo.setAge(lead.getAge());
        vo.setLeadStatus(lead.getLeadStatus());
        vo.setSourceType(lead.getSourceType());
        vo.setSourceRef(lead.getSourceRef());
        vo.setVisitorToken(lead.getVisitorToken());
        vo.setVisitorSource(lead.getVisitorSource());
        vo.setWxNickname(lead.getWxNickname());
        vo.setWxAvatar(lead.getWxAvatar());
        vo.setLastTraceTime(lead.getLastTraceTime());
        vo.setLastTraceType(lead.getLastTraceType());
        vo.setTraceCount(lead.getTraceCount());
        vo.setIntentionLevel(lead.getIntentionLevel());
        vo.setInterestType(lead.getInterestType());
        vo.setRegion(lead.getRegion());
        vo.setLastFollowTime(lead.getLastFollowTime());
        vo.setConvertedClientCode(lead.getConvertedClientCode());
        vo.setConvertedAt(lead.getConvertedAt());
        vo.setRemark(lead.getRemark());
        vo.setCreatedAt(lead.getCreatedAt());
        vo.setUpdatedAt(lead.getUpdatedAt());
        return vo;
    }
}
