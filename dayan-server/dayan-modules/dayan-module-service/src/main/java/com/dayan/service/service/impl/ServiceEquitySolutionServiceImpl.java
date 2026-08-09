package com.dayan.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.ServiceEquitySolutionCreateDTO;
import com.dayan.service.dto.ServiceEquitySolutionQueryDTO;
import com.dayan.service.dto.ServiceEquitySolutionUpdateDTO;
import com.dayan.service.dto.SolutionAcceptDTO;
import com.dayan.service.entity.ServiceEquitySolution;
import com.dayan.service.mapper.ServiceEquitySolutionMapper;
import com.dayan.service.service.ServiceEquitySolutionService;
import com.dayan.service.vo.ServiceEquitySolutionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 方案定制（service_equity_solution）服务实现。
 *
 * <p>solutionCode 生成：{@code "SO" + format(%010d, seq)}。
 * confirm_solution 前须存在 isAccepted=1 的方案（由会话层校验）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceEquitySolutionServiceImpl implements ServiceEquitySolutionService {

    private static final String SO_PREFIX = "SO";
    private static final String SO_SEQ_KEY = "code:seq:SO:0";
    private static final int SO_SEQ_WIDTH = 10;

    private final ServiceEquitySolutionMapper solutionMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<ServiceEquitySolutionVO> page(ServiceEquitySolutionQueryDTO query) {
        LambdaQueryWrapper<ServiceEquitySolution> wrapper = buildWrapper(query);
        Page<ServiceEquitySolution> page = solutionMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ServiceEquitySolutionVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ServiceEquitySolutionVO> listBySession(String sessionCode) {
        return solutionMapper.selectList(new LambdaQueryWrapper<ServiceEquitySolution>()
                .eq(ServiceEquitySolution::getSessionCode, sessionCode)
                .orderByAsc(ServiceEquitySolution::getSolutionType)
                .orderByDesc(ServiceEquitySolution::getCreatedAt)
                .orderByDesc(ServiceEquitySolution::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ServiceEquitySolutionVO getDetail(String solutionCode) {
        return toVO(requireSolutionByCode(solutionCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ServiceEquitySolutionCreateDTO dto) {
        ServiceEquitySolution entity = new ServiceEquitySolution();
        String solutionCode = generateSolutionCode();
        entity.setSolutionCode(solutionCode);
        entity.setSessionCode(dto.getSessionCode());
        entity.setDemandCode(dto.getDemandCode());
        entity.setClientCode(dto.getClientCode());
        entity.setButlerCode(dto.getButlerCode());
        entity.setSolutionName(dto.getSolutionName());
        entity.setSolutionType(dto.getSolutionType() == null ? 1 : dto.getSolutionType());
        entity.setRecommendedParks(dto.getRecommendedParks());
        entity.setPlanSummary(dto.getPlanSummary());
        entity.setServiceItems(dto.getServiceItems());
        entity.setEstimatedCost(dto.getEstimatedCost());
        entity.setCostBreakdown(dto.getCostBreakdown());
        entity.setTimeline(dto.getTimeline());
        entity.setAdvantages(dto.getAdvantages());
        entity.setRisks(dto.getRisks());
        entity.setComparison(dto.getComparison());
        entity.setPresentationMethod(dto.getPresentationMethod());
        entity.setIsAccepted(0);
        entity.setAdjustCount(0);
        entity.setStatus(0);
        entity.setRemark(dto.getRemark());
        solutionMapper.insert(entity);
        log.info("创建方案成功: sessionCode={}, solutionCode={}", dto.getSessionCode(), solutionCode);
        return solutionCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String solutionCode, ServiceEquitySolutionUpdateDTO dto) {
        ServiceEquitySolution existing = requireSolutionByCode(solutionCode);
        ServiceEquitySolution update = new ServiceEquitySolution();
        update.setId(existing.getId());
        if (dto.getSolutionName() != null) update.setSolutionName(dto.getSolutionName());
        if (dto.getSolutionType() != null) update.setSolutionType(dto.getSolutionType());
        if (dto.getRecommendedParks() != null) update.setRecommendedParks(dto.getRecommendedParks());
        if (dto.getPlanSummary() != null) update.setPlanSummary(dto.getPlanSummary());
        if (dto.getServiceItems() != null) update.setServiceItems(dto.getServiceItems());
        if (dto.getEstimatedCost() != null) update.setEstimatedCost(dto.getEstimatedCost());
        if (dto.getCostBreakdown() != null) update.setCostBreakdown(dto.getCostBreakdown());
        if (dto.getTimeline() != null) update.setTimeline(dto.getTimeline());
        if (dto.getAdvantages() != null) update.setAdvantages(dto.getAdvantages());
        if (dto.getRisks() != null) update.setRisks(dto.getRisks());
        if (dto.getComparison() != null) update.setComparison(dto.getComparison());
        if (dto.getPresentationMethod() != null) update.setPresentationMethod(dto.getPresentationMethod());
        if (dto.getClientFeedback() != null) update.setClientFeedback(dto.getClientFeedback());
        if (dto.getIsAccepted() != null) update.setIsAccepted(dto.getIsAccepted());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        solutionMapper.updateById(update);
        log.info("更新方案成功: solutionCode={}", solutionCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(SolutionAcceptDTO dto) {
        ServiceEquitySolution existing = requireSolutionByCode(dto.getSolutionCode());
        ServiceEquitySolution update = new ServiceEquitySolution();
        update.setId(existing.getId());
        update.setIsAccepted(dto.getIsAccepted());
        if (dto.getClientFeedback() != null) update.setClientFeedback(dto.getClientFeedback());
        solutionMapper.updateById(update);
        log.info("方案接受标记更新: solutionCode={}, isAccepted={}", dto.getSolutionCode(), dto.getIsAccepted());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String solutionCode) {
        ServiceEquitySolution existing = requireSolutionByCode(solutionCode);
        solutionMapper.deleteById(existing.getId());
        log.info("删除方案成功: solutionCode={}", solutionCode);
    }

    @Override
    public long countByAccepted(String sessionCode, Integer isAccepted) {
        Long count = solutionMapper.selectCount(new LambdaQueryWrapper<ServiceEquitySolution>()
                .eq(ServiceEquitySolution::getSessionCode, sessionCode)
                .eq(ServiceEquitySolution::getIsAccepted, isAccepted));
        return count == null ? 0 : count;
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ServiceEquitySolution> buildWrapper(ServiceEquitySolutionQueryDTO query) {
        LambdaQueryWrapper<ServiceEquitySolution> wrapper = new LambdaQueryWrapper<ServiceEquitySolution>()
                .orderByDesc(ServiceEquitySolution::getCreatedAt)
                .orderByDesc(ServiceEquitySolution::getId);
        if (query.getSessionCode() != null && !query.getSessionCode().isEmpty()) {
            wrapper.eq(ServiceEquitySolution::getSessionCode, query.getSessionCode());
        }
        if (query.getSolutionCode() != null && !query.getSolutionCode().isEmpty()) {
            wrapper.eq(ServiceEquitySolution::getSolutionCode, query.getSolutionCode());
        }
        if (query.getDemandCode() != null && !query.getDemandCode().isEmpty()) {
            wrapper.eq(ServiceEquitySolution::getDemandCode, query.getDemandCode());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(ServiceEquitySolution::getClientCode, query.getClientCode());
        }
        if (query.getButlerCode() != null && !query.getButlerCode().isEmpty()) {
            wrapper.eq(ServiceEquitySolution::getButlerCode, query.getButlerCode());
        }
        if (query.getSolutionType() != null) {
            wrapper.eq(ServiceEquitySolution::getSolutionType, query.getSolutionType());
        }
        if (query.getIsAccepted() != null) {
            wrapper.eq(ServiceEquitySolution::getIsAccepted, query.getIsAccepted());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ServiceEquitySolution::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ServiceEquitySolution requireSolutionByCode(String solutionCode) {
        ServiceEquitySolution entity = solutionMapper.selectOne(
                new LambdaQueryWrapper<ServiceEquitySolution>()
                        .eq(ServiceEquitySolution::getSolutionCode, solutionCode));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "方案不存在: solutionCode=" + solutionCode);
        }
        return entity;
    }

    private String generateSolutionCode() {
        long seq = sequenceProvider.next(SO_SEQ_KEY);
        return SO_PREFIX + String.format("%0" + SO_SEQ_WIDTH + "d", seq);
    }

    private ServiceEquitySolutionVO toVO(ServiceEquitySolution entity) {
        ServiceEquitySolutionVO vo = new ServiceEquitySolutionVO();
        vo.setId(entity.getId());
        vo.setSessionCode(entity.getSessionCode());
        vo.setDemandCode(entity.getDemandCode());
        vo.setClientCode(entity.getClientCode());
        vo.setButlerCode(entity.getButlerCode());
        vo.setSolutionCode(entity.getSolutionCode());
        vo.setSolutionName(entity.getSolutionName());
        vo.setSolutionType(entity.getSolutionType());
        vo.setRecommendedParks(entity.getRecommendedParks());
        vo.setPlanSummary(entity.getPlanSummary());
        vo.setServiceItems(entity.getServiceItems());
        vo.setEstimatedCost(entity.getEstimatedCost());
        vo.setCostBreakdown(entity.getCostBreakdown());
        vo.setTimeline(entity.getTimeline());
        vo.setAdvantages(entity.getAdvantages());
        vo.setRisks(entity.getRisks());
        vo.setComparison(entity.getComparison());
        vo.setPresentationTime(entity.getPresentationTime());
        vo.setPresentationMethod(entity.getPresentationMethod());
        vo.setClientFeedback(entity.getClientFeedback());
        vo.setIsAccepted(entity.getIsAccepted());
        vo.setAdjustCount(entity.getAdjustCount());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
