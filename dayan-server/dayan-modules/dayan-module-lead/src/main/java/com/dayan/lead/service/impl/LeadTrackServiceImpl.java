package com.dayan.lead.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.lead.entity.LeadContentReadRecord;
import com.dayan.lead.entity.LeadInfo;
import com.dayan.lead.entity.LeadPosterViewRecord;
import com.dayan.lead.entity.LeadToolUseRecord;
import com.dayan.lead.mapper.LeadContentReadRecordMapper;
import com.dayan.lead.mapper.LeadInfoMapper;
import com.dayan.lead.mapper.LeadPosterViewRecordMapper;
import com.dayan.lead.mapper.LeadToolUseRecordMapper;
import com.dayan.lead.service.LeadTrackService;
import com.dayan.lead.vo.LeadTraceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 访客线索追踪服务实现。
 *
 * <p>线索编码生成：{@code "VL" + String.format("%05d", sequenceProvider.next("code:seq:VL:0"))}。
 * 线索归属渠道（首触渠道），互动明细按类型拆三张表，lead_info 只保留聚合字段。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadTrackServiceImpl implements LeadTrackService {

    /** 线索编码前缀 */
    private static final String CODE_PREFIX = "VL";
    /** 序列键 */
    private static final String SEQ_KEY = "code:seq:VL:0";

    private final LeadInfoMapper leadInfoMapper;
    private final LeadContentReadRecordMapper contentReadMapper;
    private final LeadToolUseRecordMapper toolUseMapper;
    private final LeadPosterViewRecordMapper posterViewMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String track(String channelCode, String agentCode, int interactType, String bizCode,
                        String bizTitle, String visitorToken, String visitorSource) {
        // 渠道归属缺失（无分享人的直接访问）无法建档，仅回令牌不落库
        if (!StringUtils.hasText(channelCode)) {
            log.warn("[Lead] track 缺少渠道归属，跳过建档: agentCode={}, bizCode={}", agentCode, bizCode);
            return StringUtils.hasText(visitorToken)
                    ? visitorToken : UUID.randomUUID().toString().replace("-", "");
        }

        // 1. 生成或复用 visitorToken，查/建 lead_info
        String token = StringUtils.hasText(visitorToken)
                ? visitorToken : UUID.randomUUID().toString().replace("-", "");
        LeadInfo lead = leadInfoMapper.selectOne(new LambdaQueryWrapper<LeadInfo>()
                .eq(LeadInfo::getVisitorToken, token)
                .last("LIMIT 1"));
        if (lead == null) {
            lead = new LeadInfo();
            lead.setLeadCode(CODE_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY)));
            lead.setVisitorToken(token);
            lead.setChannelCode(channelCode);
            lead.setVisitorSource(StringUtils.hasText(visitorSource) ? visitorSource : "unknown");
            lead.setSourceType(interactType);
            lead.setSourceCode(bizCode);
            lead.setInteractCount(0);
            leadInfoMapper.insert(lead);
            log.info("[Lead] 新访客线索: leadCode={}, channelCode={}, token={}",
                    lead.getLeadCode(), channelCode, token);
        }

        // 2. 写入互动明细（按类型拆表）
        LocalDateTime now = LocalDateTime.now();
        switch (interactType) {
            case 2 -> {
                LeadToolUseRecord record = new LeadToolUseRecord();
                record.setLeadCode(lead.getLeadCode());
                record.setVisitorToken(token);
                record.setChannelCode(channelCode);
                record.setAgentCode(agentCode);
                record.setToolCode(bizCode != null ? bizCode : "");
                record.setToolName(bizTitle);
                toolUseMapper.insert(record);
            }
            case 3 -> {
                LeadPosterViewRecord record = new LeadPosterViewRecord();
                record.setLeadCode(lead.getLeadCode());
                record.setVisitorToken(token);
                record.setChannelCode(channelCode);
                record.setAgentCode(agentCode);
                record.setTemplateCode(bizCode != null ? bizCode : "");
                record.setPosterTitle(bizTitle);
                posterViewMapper.insert(record);
            }
            default -> {
                LeadContentReadRecord record = new LeadContentReadRecord();
                record.setLeadCode(lead.getLeadCode());
                record.setVisitorToken(token);
                record.setChannelCode(channelCode);
                record.setAgentCode(agentCode);
                record.setContentCode(bizCode != null ? bizCode : "");
                record.setContentTitle(bizTitle);
                contentReadMapper.insert(record);
            }
        }

        // 3. 更新聚合字段（计数原子自增，防并发丢更新）
        leadInfoMapper.update(null, new LambdaUpdateWrapper<LeadInfo>()
                .eq(LeadInfo::getId, lead.getId())
                .set(LeadInfo::getLastInteractTime, now)
                .set(LeadInfo::getLastInteractType, interactType)
                .setSql("interact_count = interact_count + 1"));

        return token;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveContact(String visitorToken, String phone, String name) {
        if (!StringUtils.hasText(visitorToken) || !StringUtils.hasText(phone)) {
            return;
        }
        LeadInfo lead = leadInfoMapper.selectOne(new LambdaQueryWrapper<LeadInfo>()
                .eq(LeadInfo::getVisitorToken, visitorToken)
                .last("LIMIT 1"));
        if (lead == null) {
            log.warn("[Lead] 留资失败：未找到 visitorToken={}", visitorToken);
            return;
        }
        LeadInfo update = new LeadInfo();
        update.setId(lead.getId());
        update.setPhone(phone);
        if (StringUtils.hasText(name)) {
            update.setName(name);
        }
        leadInfoMapper.updateById(update);
        log.info("[Lead] 访客留资: leadCode={}, phone={}", lead.getLeadCode(), phone);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindClient(String visitorToken, String clientCode) {
        if (!StringUtils.hasText(visitorToken) || !StringUtils.hasText(clientCode)) {
            return;
        }
        leadInfoMapper.update(null, new LambdaUpdateWrapper<LeadInfo>()
                .eq(LeadInfo::getVisitorToken, visitorToken)
                .isNull(LeadInfo::getClientCode)
                .set(LeadInfo::getClientCode, clientCode));
    }

    @Override
    public List<LeadTraceVO> listTraces(String visitorToken, int limit) {
        if (!StringUtils.hasText(visitorToken)) {
            return List.of();
        }
        List<LeadTraceVO> traces = new ArrayList<>();
        contentReadMapper.selectList(new LambdaQueryWrapper<LeadContentReadRecord>()
                .eq(LeadContentReadRecord::getVisitorToken, visitorToken)).forEach(r -> {
            LeadTraceVO vo = new LeadTraceVO();
            vo.setId(r.getId());
            vo.setTraceType(1);
            vo.setBizCode(r.getContentCode());
            vo.setBizTitle(r.getContentTitle());
            vo.setTraceTime(r.getCreatedAt());
            traces.add(vo);
        });
        toolUseMapper.selectList(new LambdaQueryWrapper<LeadToolUseRecord>()
                .eq(LeadToolUseRecord::getVisitorToken, visitorToken)).forEach(r -> {
            LeadTraceVO vo = new LeadTraceVO();
            vo.setId(r.getId());
            vo.setTraceType(2);
            vo.setBizCode(r.getToolCode());
            vo.setBizTitle(r.getToolName());
            vo.setTraceTime(r.getCreatedAt());
            traces.add(vo);
        });
        posterViewMapper.selectList(new LambdaQueryWrapper<LeadPosterViewRecord>()
                .eq(LeadPosterViewRecord::getVisitorToken, visitorToken)).forEach(r -> {
            LeadTraceVO vo = new LeadTraceVO();
            vo.setId(r.getId());
            vo.setTraceType(3);
            vo.setBizCode(r.getTemplateCode());
            vo.setBizTitle(r.getPosterTitle());
            vo.setTraceTime(r.getCreatedAt());
            traces.add(vo);
        });
        return traces.stream()
                .sorted(Comparator.comparing(LeadTraceVO::getTraceTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit > 0 ? limit : 50)
                .toList();
    }
}
