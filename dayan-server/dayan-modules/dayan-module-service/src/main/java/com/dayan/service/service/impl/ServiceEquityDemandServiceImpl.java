package com.dayan.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.ServiceEquityDemandCreateDTO;
import com.dayan.service.dto.ServiceEquityDemandQueryDTO;
import com.dayan.service.dto.ServiceEquityDemandUpdateDTO;
import com.dayan.service.entity.ServiceEquityDemand;
import com.dayan.service.mapper.ServiceEquityDemandMapper;
import com.dayan.service.service.ServiceEquityDemandService;
import com.dayan.service.vo.ServiceEquityDemandVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 需求收集（service_equity_demand）服务实现。
 *
 * <p>demandCode 生成：{@code "DM" + format(%010d, seq)}。budgetMin ≤ budgetMax 校验。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceEquityDemandServiceImpl implements ServiceEquityDemandService {

    private static final String DM_PREFIX = "DM";
    private static final String DM_SEQ_KEY = "code:seq:DM:0";
    private static final int DM_SEQ_WIDTH = 10;

    private final ServiceEquityDemandMapper demandMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<ServiceEquityDemandVO> page(ServiceEquityDemandQueryDTO query) {
        LambdaQueryWrapper<ServiceEquityDemand> wrapper = buildWrapper(query);
        Page<ServiceEquityDemand> page = demandMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ServiceEquityDemandVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ServiceEquityDemandVO> listBySession(String sessionCode) {
        return demandMapper.selectList(new LambdaQueryWrapper<ServiceEquityDemand>()
                .eq(ServiceEquityDemand::getSessionCode, sessionCode)
                .orderByDesc(ServiceEquityDemand::getCreatedAt)
                .orderByDesc(ServiceEquityDemand::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ServiceEquityDemandVO getDetail(Long id) {
        return toVO(requireDemand(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ServiceEquityDemandCreateDTO dto) {
        validateBudget(dto.getBudgetMin(), dto.getBudgetMax());

        ServiceEquityDemand entity = new ServiceEquityDemand();
        String demandCode = generateDemandCode();
        entity.setDemandCode(demandCode);
        entity.setSessionCode(dto.getSessionCode());
        entity.setClientCode(dto.getClientCode());
        entity.setButlerCode(dto.getButlerCode());
        entity.setDemandType(dto.getDemandType());
        entity.setUsePersonName(dto.getUsePersonName());
        entity.setUsePersonAge(dto.getUsePersonAge());
        entity.setUsePersonGender(dto.getUsePersonGender());
        entity.setHealthSummary(dto.getHealthSummary());
        entity.setCareLevelNeed(dto.getCareLevelNeed());
        entity.setCityPreference(dto.getCityPreference());
        entity.setAreaPreference(dto.getAreaPreference());
        entity.setBudgetMin(dto.getBudgetMin());
        entity.setBudgetMax(dto.getBudgetMax());
        entity.setRoomPreference(dto.getRoomPreference());
        entity.setFoodPreference(dto.getFoodPreference());
        entity.setSpecialNeeds(dto.getSpecialNeeds());
        entity.setExpectedTime(dto.getExpectedTime());
        entity.setContactPreference(dto.getContactPreference());
        entity.setCollectMethod(dto.getCollectMethod() == null ? 1 : dto.getCollectMethod());
        entity.setCollectTime(LocalDateTime.now());
        entity.setDemandSummary(dto.getDemandSummary());
        entity.setDemandImages(dto.getDemandImages());
        entity.setStatus(0);
        entity.setRemark(dto.getRemark());
        demandMapper.insert(entity);
        log.info("创建需求成功: sessionCode={}, demandCode={}", dto.getSessionCode(), demandCode);
        return demandCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ServiceEquityDemandUpdateDTO dto) {
        ServiceEquityDemand existing = requireDemand(id);
        validateBudget(dto.getBudgetMin(), dto.getBudgetMax());

        ServiceEquityDemand update = new ServiceEquityDemand();
        update.setId(existing.getId());
        if (dto.getDemandType() != null) update.setDemandType(dto.getDemandType());
        if (dto.getUsePersonName() != null) update.setUsePersonName(dto.getUsePersonName());
        if (dto.getUsePersonAge() != null) update.setUsePersonAge(dto.getUsePersonAge());
        if (dto.getUsePersonGender() != null) update.setUsePersonGender(dto.getUsePersonGender());
        if (dto.getHealthSummary() != null) update.setHealthSummary(dto.getHealthSummary());
        if (dto.getCareLevelNeed() != null) update.setCareLevelNeed(dto.getCareLevelNeed());
        if (dto.getCityPreference() != null) update.setCityPreference(dto.getCityPreference());
        if (dto.getAreaPreference() != null) update.setAreaPreference(dto.getAreaPreference());
        if (dto.getBudgetMin() != null) update.setBudgetMin(dto.getBudgetMin());
        if (dto.getBudgetMax() != null) update.setBudgetMax(dto.getBudgetMax());
        if (dto.getRoomPreference() != null) update.setRoomPreference(dto.getRoomPreference());
        if (dto.getFoodPreference() != null) update.setFoodPreference(dto.getFoodPreference());
        if (dto.getSpecialNeeds() != null) update.setSpecialNeeds(dto.getSpecialNeeds());
        if (dto.getExpectedTime() != null) update.setExpectedTime(dto.getExpectedTime());
        if (dto.getContactPreference() != null) update.setContactPreference(dto.getContactPreference());
        if (dto.getCollectMethod() != null) update.setCollectMethod(dto.getCollectMethod());
        if (dto.getDemandSummary() != null) update.setDemandSummary(dto.getDemandSummary());
        if (dto.getDemandImages() != null) update.setDemandImages(dto.getDemandImages());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        demandMapper.updateById(update);
        log.info("更新需求成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ServiceEquityDemand existing = requireDemand(id);
        demandMapper.deleteById(existing.getId());
        log.info("删除需求成功: id={}", id);
    }

    // ====== 内部方法 ======

    private void validateBudget(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预算下限不能大于上限");
        }
    }

    private LambdaQueryWrapper<ServiceEquityDemand> buildWrapper(ServiceEquityDemandQueryDTO query) {
        LambdaQueryWrapper<ServiceEquityDemand> wrapper = new LambdaQueryWrapper<ServiceEquityDemand>()
                .orderByDesc(ServiceEquityDemand::getCreatedAt)
                .orderByDesc(ServiceEquityDemand::getId);
        if (query.getSessionCode() != null && !query.getSessionCode().isEmpty()) {
            wrapper.eq(ServiceEquityDemand::getSessionCode, query.getSessionCode());
        }
        if (query.getDemandCode() != null && !query.getDemandCode().isEmpty()) {
            wrapper.eq(ServiceEquityDemand::getDemandCode, query.getDemandCode());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(ServiceEquityDemand::getClientCode, query.getClientCode());
        }
        if (query.getButlerCode() != null && !query.getButlerCode().isEmpty()) {
            wrapper.eq(ServiceEquityDemand::getButlerCode, query.getButlerCode());
        }
        if (query.getDemandType() != null) {
            wrapper.eq(ServiceEquityDemand::getDemandType, query.getDemandType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ServiceEquityDemand::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ServiceEquityDemand requireDemand(Long id) {
        ServiceEquityDemand entity = demandMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "需求不存在: id=" + id);
        }
        return entity;
    }

    private String generateDemandCode() {
        long seq = sequenceProvider.next(DM_SEQ_KEY);
        return DM_PREFIX + String.format("%0" + DM_SEQ_WIDTH + "d", seq);
    }

    private ServiceEquityDemandVO toVO(ServiceEquityDemand entity) {
        ServiceEquityDemandVO vo = new ServiceEquityDemandVO();
        vo.setId(entity.getId());
        vo.setDemandCode(entity.getDemandCode());
        vo.setSessionCode(entity.getSessionCode());
        vo.setClientCode(entity.getClientCode());
        vo.setButlerCode(entity.getButlerCode());
        vo.setDemandType(entity.getDemandType());
        vo.setUsePersonName(entity.getUsePersonName());
        vo.setUsePersonAge(entity.getUsePersonAge());
        vo.setUsePersonGender(entity.getUsePersonGender());
        vo.setHealthSummary(entity.getHealthSummary());
        vo.setCareLevelNeed(entity.getCareLevelNeed());
        vo.setCityPreference(entity.getCityPreference());
        vo.setAreaPreference(entity.getAreaPreference());
        vo.setBudgetMin(entity.getBudgetMin());
        vo.setBudgetMax(entity.getBudgetMax());
        vo.setRoomPreference(entity.getRoomPreference());
        vo.setFoodPreference(entity.getFoodPreference());
        vo.setSpecialNeeds(entity.getSpecialNeeds());
        vo.setExpectedTime(entity.getExpectedTime());
        vo.setContactPreference(entity.getContactPreference());
        vo.setCollectMethod(entity.getCollectMethod());
        vo.setCollectTime(entity.getCollectTime());
        vo.setDemandSummary(entity.getDemandSummary());
        vo.setDemandImages(entity.getDemandImages());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
