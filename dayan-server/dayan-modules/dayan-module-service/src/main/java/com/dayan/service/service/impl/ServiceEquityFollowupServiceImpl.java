package com.dayan.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.ServiceEquityFollowupCreateDTO;
import com.dayan.service.dto.ServiceEquityFollowupQueryDTO;
import com.dayan.service.dto.ServiceEquityFollowupUpdateDTO;
import com.dayan.service.entity.ServiceEquityFollowup;
import com.dayan.service.mapper.ServiceEquityFollowupMapper;
import com.dayan.service.service.ServiceEquityFollowupService;
import com.dayan.service.vo.ServiceEquityFollowupVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 回访品控（service_equity_followup）服务实现。
 *
 * <p>followupCode 生成：{@code "FU" + format(%010d, seq)}。
 * 4 维满意度（serviceSatisfaction/parkSatisfaction/butlerSatisfaction/overallSatisfaction，1-5），
 * 任一 <3 时 isFollowupNeeded=1 + nextFollowupDate（未显式提供则默认回访日 +7 天）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceEquityFollowupServiceImpl implements ServiceEquityFollowupService {

    private static final String FU_PREFIX = "FU";
    private static final String FU_SEQ_KEY = "code:seq:FU:0";
    private static final int FU_SEQ_WIDTH = 10;

    /** 满意度阈值：低于此值需跟进 */
    private static final int SATISFACTION_THRESHOLD = 3;
    /** 默认下次回访间隔（天） */
    private static final int DEFAULT_NEXT_FOLLOWUP_DAYS = 7;

    private final ServiceEquityFollowupMapper followupMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<ServiceEquityFollowupVO> page(ServiceEquityFollowupQueryDTO query) {
        LambdaQueryWrapper<ServiceEquityFollowup> wrapper = buildWrapper(query);
        Page<ServiceEquityFollowup> page = followupMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ServiceEquityFollowupVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ServiceEquityFollowupVO> listBySession(String sessionCode) {
        return followupMapper.selectList(new LambdaQueryWrapper<ServiceEquityFollowup>()
                .eq(ServiceEquityFollowup::getSessionCode, sessionCode)
                .orderByDesc(ServiceEquityFollowup::getFollowupDate)
                .orderByDesc(ServiceEquityFollowup::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ServiceEquityFollowupVO getDetail(String followupCode) {
        return toVO(requireFollowupByCode(followupCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ServiceEquityFollowupCreateDTO dto) {
        validateSatisfaction(dto.getServiceSatisfaction(), "服务满意度");
        validateSatisfaction(dto.getParkSatisfaction(), "机构满意度");
        validateSatisfaction(dto.getButlerSatisfaction(), "管家满意度");
        validateSatisfaction(dto.getOverallSatisfaction(), "综合满意度");

        ServiceEquityFollowup entity = new ServiceEquityFollowup();
        String followupCode = generateFollowupCode();
        entity.setFollowupCode(followupCode);
        entity.setSessionCode(dto.getSessionCode());
        entity.setArrangeCode(dto.getArrangeCode());
        entity.setClientCode(dto.getClientCode());
        entity.setButlerCode(dto.getButlerCode());
        entity.setFollowupType(dto.getFollowupType() == null ? 1 : dto.getFollowupType());
        entity.setFollowupMethod(dto.getFollowupMethod() == null ? 1 : dto.getFollowupMethod());
        entity.setFollowupDate(dto.getFollowupDate());
        entity.setFollowupTime(LocalDateTime.now());
        entity.setServiceSatisfaction(dto.getServiceSatisfaction());
        entity.setParkSatisfaction(dto.getParkSatisfaction());
        entity.setButlerSatisfaction(dto.getButlerSatisfaction());
        entity.setOverallSatisfaction(dto.getOverallSatisfaction());
        entity.setServiceEvaluation(dto.getServiceEvaluation());
        entity.setImprovementSuggestions(dto.getImprovementSuggestions());
        entity.setComplaints(dto.getComplaints());
        entity.setComplaintHandle(dto.getComplaintHandle());

        // 任一满意度 <3 → 需跟进
        boolean needFollowup = isLowSatisfaction(dto.getServiceSatisfaction())
                || isLowSatisfaction(dto.getParkSatisfaction())
                || isLowSatisfaction(dto.getButlerSatisfaction())
                || isLowSatisfaction(dto.getOverallSatisfaction());
        entity.setIsFollowupNeeded(needFollowup ? 1 : 0);
        entity.setFollowupPlan(dto.getFollowupPlan());
        // 需跟进且未显式提供下次回访日 → 默认回访日 +7 天
        if (needFollowup && dto.getNextFollowupDate() == null && dto.getFollowupDate() != null) {
            entity.setNextFollowupDate(dto.getFollowupDate().plusDays(DEFAULT_NEXT_FOLLOWUP_DAYS));
        } else {
            entity.setNextFollowupDate(dto.getNextFollowupDate());
        }
        entity.setIsResolved(dto.getIsResolved());
        entity.setStatus(2);
        entity.setRemark(dto.getRemark());
        followupMapper.insert(entity);
        log.info("创建回访成功: sessionCode={}, followupCode={}, needFollowup={}",
                dto.getSessionCode(), followupCode, needFollowup);
        return followupCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String followupCode, ServiceEquityFollowupUpdateDTO dto) {
        ServiceEquityFollowup existing = requireFollowupByCode(followupCode);
        validateSatisfaction(dto.getServiceSatisfaction(), "服务满意度");
        validateSatisfaction(dto.getParkSatisfaction(), "机构满意度");
        validateSatisfaction(dto.getButlerSatisfaction(), "管家满意度");
        validateSatisfaction(dto.getOverallSatisfaction(), "综合满意度");

        ServiceEquityFollowup update = new ServiceEquityFollowup();
        update.setId(existing.getId());
        if (dto.getFollowupType() != null) update.setFollowupType(dto.getFollowupType());
        if (dto.getFollowupMethod() != null) update.setFollowupMethod(dto.getFollowupMethod());
        if (dto.getFollowupDate() != null) update.setFollowupDate(dto.getFollowupDate());
        if (dto.getServiceSatisfaction() != null) update.setServiceSatisfaction(dto.getServiceSatisfaction());
        if (dto.getParkSatisfaction() != null) update.setParkSatisfaction(dto.getParkSatisfaction());
        if (dto.getButlerSatisfaction() != null) update.setButlerSatisfaction(dto.getButlerSatisfaction());
        if (dto.getOverallSatisfaction() != null) update.setOverallSatisfaction(dto.getOverallSatisfaction());
        if (dto.getServiceEvaluation() != null) update.setServiceEvaluation(dto.getServiceEvaluation());
        if (dto.getImprovementSuggestions() != null) update.setImprovementSuggestions(dto.getImprovementSuggestions());
        if (dto.getComplaints() != null) update.setComplaints(dto.getComplaints());
        if (dto.getComplaintHandle() != null) update.setComplaintHandle(dto.getComplaintHandle());
        if (dto.getIsFollowupNeeded() != null) update.setIsFollowupNeeded(dto.getIsFollowupNeeded());
        if (dto.getFollowupPlan() != null) update.setFollowupPlan(dto.getFollowupPlan());
        if (dto.getNextFollowupDate() != null) update.setNextFollowupDate(dto.getNextFollowupDate());
        if (dto.getIsResolved() != null) update.setIsResolved(dto.getIsResolved());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        followupMapper.updateById(update);
        log.info("更新回访成功: followupCode={}", followupCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String followupCode) {
        ServiceEquityFollowup existing = requireFollowupByCode(followupCode);
        followupMapper.deleteById(existing.getId());
        log.info("删除回访成功: followupCode={}", followupCode);
    }

    // ====== 内部方法 ======

    private void validateSatisfaction(Integer value, String label) {
        if (value != null && (value < 1 || value > 5)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, label + "评分范围 1-5");
        }
    }

    private boolean isLowSatisfaction(Integer value) {
        return value != null && value < SATISFACTION_THRESHOLD;
    }

    @SuppressWarnings("unused")
    private LocalDate defaultNextFollowup(LocalDate base) {
        return base == null ? null : base.plusDays(DEFAULT_NEXT_FOLLOWUP_DAYS);
    }

    private LambdaQueryWrapper<ServiceEquityFollowup> buildWrapper(ServiceEquityFollowupQueryDTO query) {
        LambdaQueryWrapper<ServiceEquityFollowup> wrapper = new LambdaQueryWrapper<ServiceEquityFollowup>()
                .orderByDesc(ServiceEquityFollowup::getFollowupDate)
                .orderByDesc(ServiceEquityFollowup::getId);
        if (query.getSessionCode() != null && !query.getSessionCode().isEmpty()) {
            wrapper.eq(ServiceEquityFollowup::getSessionCode, query.getSessionCode());
        }
        if (query.getFollowupCode() != null && !query.getFollowupCode().isEmpty()) {
            wrapper.eq(ServiceEquityFollowup::getFollowupCode, query.getFollowupCode());
        }
        if (query.getArrangeCode() != null && !query.getArrangeCode().isEmpty()) {
            wrapper.eq(ServiceEquityFollowup::getArrangeCode, query.getArrangeCode());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(ServiceEquityFollowup::getClientCode, query.getClientCode());
        }
        if (query.getButlerCode() != null && !query.getButlerCode().isEmpty()) {
            wrapper.eq(ServiceEquityFollowup::getButlerCode, query.getButlerCode());
        }
        if (query.getFollowupType() != null) {
            wrapper.eq(ServiceEquityFollowup::getFollowupType, query.getFollowupType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ServiceEquityFollowup::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ServiceEquityFollowup requireFollowupByCode(String followupCode) {
        ServiceEquityFollowup entity = followupMapper.selectOne(
                new LambdaQueryWrapper<ServiceEquityFollowup>()
                        .eq(ServiceEquityFollowup::getFollowupCode, followupCode));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "回访不存在: followupCode=" + followupCode);
        }
        return entity;
    }

    private String generateFollowupCode() {
        long seq = sequenceProvider.next(FU_SEQ_KEY);
        return FU_PREFIX + String.format("%0" + FU_SEQ_WIDTH + "d", seq);
    }

    private ServiceEquityFollowupVO toVO(ServiceEquityFollowup entity) {
        ServiceEquityFollowupVO vo = new ServiceEquityFollowupVO();
        vo.setId(entity.getId());
        vo.setSessionCode(entity.getSessionCode());
        vo.setArrangeCode(entity.getArrangeCode());
        vo.setClientCode(entity.getClientCode());
        vo.setButlerCode(entity.getButlerCode());
        vo.setFollowupCode(entity.getFollowupCode());
        vo.setFollowupType(entity.getFollowupType());
        vo.setFollowupMethod(entity.getFollowupMethod());
        vo.setFollowupDate(entity.getFollowupDate());
        vo.setFollowupTime(entity.getFollowupTime());
        vo.setServiceSatisfaction(entity.getServiceSatisfaction());
        vo.setParkSatisfaction(entity.getParkSatisfaction());
        vo.setButlerSatisfaction(entity.getButlerSatisfaction());
        vo.setOverallSatisfaction(entity.getOverallSatisfaction());
        vo.setServiceEvaluation(entity.getServiceEvaluation());
        vo.setImprovementSuggestions(entity.getImprovementSuggestions());
        vo.setComplaints(entity.getComplaints());
        vo.setComplaintHandle(entity.getComplaintHandle());
        vo.setIsFollowupNeeded(entity.getIsFollowupNeeded());
        vo.setFollowupPlan(entity.getFollowupPlan());
        vo.setNextFollowupDate(entity.getNextFollowupDate());
        vo.setIsResolved(entity.getIsResolved());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
