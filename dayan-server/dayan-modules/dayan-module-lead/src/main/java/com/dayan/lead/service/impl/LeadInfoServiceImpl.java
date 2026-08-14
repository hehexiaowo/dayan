package com.dayan.lead.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.lead.entity.LeadInfo;
import com.dayan.lead.mapper.LeadInfoMapper;
import com.dayan.lead.service.LeadInfoService;
import com.dayan.lead.vo.LeadInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 访客线索查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class LeadInfoServiceImpl implements LeadInfoService {

    private final LeadInfoMapper leadInfoMapper;

    @Override
    public PageResult<LeadInfoVO> page(String channelCode, String keyword, Boolean onlyWithPhone,
                                       boolean excludeClaimedByAgent, long current, long size) {
        LambdaQueryWrapper<LeadInfo> wrapper = new LambdaQueryWrapper<LeadInfo>()
                .eq(StringUtils.hasText(channelCode), LeadInfo::getChannelCode, channelCode)
                .orderByDesc(LeadInfo::getLastInteractTime)
                .orderByDesc(LeadInfo::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(LeadInfo::getName, kw)
                    .or().like(LeadInfo::getPhone, kw)
                    .or().like(LeadInfo::getWxNickname, kw));
        }
        if (Boolean.TRUE.equals(onlyWithPhone)) {
            wrapper.isNotNull(LeadInfo::getPhone);
        }
        if (excludeClaimedByAgent) {
            // 线索池：排除已被认领进 agent_lead 的（agent_lead.visitor_lead_code 反向关联）
            wrapper.apply("NOT EXISTS (SELECT 1 FROM agent_lead al "
                    + "WHERE al.visitor_lead_code = lead_info.lead_code AND al.deleted = 0)");
        }
        Page<LeadInfo> page = leadInfoMapper.selectPage(new Page<>(current, size), wrapper);
        List<LeadInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(current, size, page.getTotal(), records);
    }

    @Override
    public LeadInfoVO getByLeadCode(String leadCode) {
        LeadInfo lead = leadInfoMapper.selectOne(new LambdaQueryWrapper<LeadInfo>()
                .eq(LeadInfo::getLeadCode, leadCode)
                .last("LIMIT 1"));
        if (lead == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "线索不存在: " + leadCode);
        }
        return toVO(lead);
    }

    @Override
    public LeadInfo getByVisitorToken(String visitorToken) {
        if (!StringUtils.hasText(visitorToken)) {
            return null;
        }
        return leadInfoMapper.selectOne(new LambdaQueryWrapper<LeadInfo>()
                .eq(LeadInfo::getVisitorToken, visitorToken)
                .last("LIMIT 1"));
    }

    private LeadInfoVO toVO(LeadInfo entity) {
        LeadInfoVO vo = new LeadInfoVO();
        vo.setId(entity.getId());
        vo.setLeadCode(entity.getLeadCode());
        vo.setVisitorToken(entity.getVisitorToken());
        vo.setChannelCode(entity.getChannelCode());
        vo.setOpenid(entity.getOpenid());
        vo.setPhone(entity.getPhone());
        vo.setName(entity.getName());
        vo.setWxNickname(entity.getWxNickname());
        vo.setWxAvatar(entity.getWxAvatar());
        vo.setVisitorSource(entity.getVisitorSource());
        vo.setSourceType(entity.getSourceType());
        vo.setSourceCode(entity.getSourceCode());
        vo.setClientCode(entity.getClientCode());
        vo.setLastInteractTime(entity.getLastInteractTime());
        vo.setLastInteractType(entity.getLastInteractType());
        vo.setInteractCount(entity.getInteractCount());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
