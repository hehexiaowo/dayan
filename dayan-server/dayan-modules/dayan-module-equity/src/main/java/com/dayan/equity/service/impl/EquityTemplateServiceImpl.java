package com.dayan.equity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.equity.dto.EquityTemplateCreateDTO;
import com.dayan.equity.dto.EquityTemplateQueryDTO;
import com.dayan.equity.dto.EquityTemplateUpdateDTO;
import com.dayan.equity.entity.EquityTemplate;
import com.dayan.equity.mapper.EquityTemplateMapper;
import com.dayan.equity.service.EquityTemplateService;
import com.dayan.equity.vo.EquityTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权益模板服务实现。
 *
 * <p>{@code template_code} = {@code "ET" + String.format("%05d", sequenceProvider.next("code:seq:ET:0"))}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquityTemplateServiceImpl implements EquityTemplateService {

    /** 模板编码前缀 */
    private static final String CODE_PREFIX = "ET";
    /** SequenceProvider key */
    private static final String SEQ_KEY = "code:seq:ET:0";
    /** 序列宽度（不足左侧补零） */
    private static final int SEQ_WIDTH = 5;
    /** 默认状态：启用 */
    private static final int STATUS_DEFAULT = 1;

    private final EquityTemplateMapper templateMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<EquityTemplateVO> page(EquityTemplateQueryDTO query) {
        LambdaQueryWrapper<EquityTemplate> wrapper = buildQueryWrapper(query);
        Page<EquityTemplate> page = templateMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<EquityTemplateVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<EquityTemplateVO> list(EquityTemplateQueryDTO query) {
        return templateMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public EquityTemplateVO getDetail(String templateCode) {
        return toVO(requireTemplate(templateCode));
    }

    @Override
    public EquityTemplate requireTemplate(String templateCode) {
        EquityTemplate template = templateMapper.selectOne(new LambdaQueryWrapper<EquityTemplate>()
                .eq(EquityTemplate::getTemplateCode, templateCode)
                .last("LIMIT 1"));
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权益模板不存在: " + templateCode);
        }
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(EquityTemplateCreateDTO dto) {
        String templateCode = generateCode();

        EquityTemplate entity = new EquityTemplate();
        entity.setTemplateCode(templateCode);
        entity.setTemplateName(dto.getTemplateName());
        entity.setEquityType(dto.getEquityType());
        entity.setEquityLevel(dto.getEquityLevel());
        entity.setEquityValue(dto.getEquityValue());
        entity.setCostPrice(dto.getCostPrice());
        entity.setContentDescription(dto.getContentDescription());
        entity.setServiceItems(dto.getServiceItems());
        entity.setApplicableParks(dto.getApplicableParks());
        entity.setApplicableCities(dto.getApplicableCities());
        entity.setValidDays(dto.getValidDays());
        entity.setShelfLifeDays(dto.getShelfLifeDays());
        entity.setIsTransferable(dto.getIsTransferable() == null ? 0 : dto.getIsTransferable());
        entity.setIsStackable(dto.getIsStackable() == null ? 0 : dto.getIsStackable());
        entity.setMaxUseCount(dto.getMaxUseCount());
        entity.setCoverImage(dto.getCoverImage());
        entity.setCardDesignUrl(dto.getCardDesignUrl());
        entity.setTerms(dto.getTerms());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? STATUS_DEFAULT : dto.getStatus());
        entity.setRemark(dto.getRemark());

        templateMapper.insert(entity);
        log.info("创建权益模板成功: templateCode={}", templateCode);
        return templateCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String templateCode, EquityTemplateUpdateDTO dto) {
        EquityTemplate existing = requireTemplate(templateCode);
        EquityTemplate update = new EquityTemplate();
        update.setId(existing.getId());

        if (dto.getTemplateName() != null) update.setTemplateName(dto.getTemplateName());
        if (dto.getEquityLevel() != null) update.setEquityLevel(dto.getEquityLevel());
        if (dto.getEquityValue() != null) update.setEquityValue(dto.getEquityValue());
        if (dto.getCostPrice() != null) update.setCostPrice(dto.getCostPrice());
        if (dto.getContentDescription() != null) update.setContentDescription(dto.getContentDescription());
        if (dto.getServiceItems() != null) update.setServiceItems(dto.getServiceItems());
        if (dto.getApplicableParks() != null) update.setApplicableParks(dto.getApplicableParks());
        if (dto.getApplicableCities() != null) update.setApplicableCities(dto.getApplicableCities());
        if (dto.getValidDays() != null) update.setValidDays(dto.getValidDays());
        if (dto.getShelfLifeDays() != null) update.setShelfLifeDays(dto.getShelfLifeDays());
        if (dto.getIsTransferable() != null) update.setIsTransferable(dto.getIsTransferable());
        if (dto.getIsStackable() != null) update.setIsStackable(dto.getIsStackable());
        if (dto.getMaxUseCount() != null) update.setMaxUseCount(dto.getMaxUseCount());
        if (dto.getCoverImage() != null) update.setCoverImage(dto.getCoverImage());
        if (dto.getCardDesignUrl() != null) update.setCardDesignUrl(dto.getCardDesignUrl());
        if (dto.getTerms() != null) update.setTerms(dto.getTerms());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        templateMapper.updateById(update);
        log.info("更新权益模板成功: templateCode={}", templateCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String templateCode) {
        EquityTemplate existing = requireTemplate(templateCode);
        templateMapper.deleteById(existing.getId());
        log.info("删除权益模板成功: templateCode={}", templateCode);
    }

    // ====== 内部方法 ======

    private String generateCode() {
        long seq = sequenceProvider.next(SEQ_KEY);
        return CODE_PREFIX + String.format("%0" + SEQ_WIDTH + "d", seq);
    }

    private LambdaQueryWrapper<EquityTemplate> buildQueryWrapper(EquityTemplateQueryDTO query) {
        LambdaQueryWrapper<EquityTemplate> wrapper = new LambdaQueryWrapper<EquityTemplate>()
                .orderByDesc(EquityTemplate::getCreatedAt);
        if (query.getTemplateCode() != null && !query.getTemplateCode().isEmpty()) {
            wrapper.eq(EquityTemplate::getTemplateCode, query.getTemplateCode());
        }
        if (query.getTemplateName() != null && !query.getTemplateName().isEmpty()) {
            wrapper.like(EquityTemplate::getTemplateName, query.getTemplateName());
        }
        if (query.getEquityType() != null) {
            wrapper.eq(EquityTemplate::getEquityType, query.getEquityType());
        }
        if (query.getEquityLevel() != null) {
            wrapper.eq(EquityTemplate::getEquityLevel, query.getEquityLevel());
        }
        if (query.getStatus() != null) {
            wrapper.eq(EquityTemplate::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private EquityTemplateVO toVO(EquityTemplate entity) {
        EquityTemplateVO vo = new EquityTemplateVO();
        vo.setId(entity.getId());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setTemplateName(entity.getTemplateName());
        vo.setEquityType(entity.getEquityType());
        vo.setEquityLevel(entity.getEquityLevel());
        vo.setEquityValue(entity.getEquityValue());
        vo.setCostPrice(entity.getCostPrice());
        vo.setContentDescription(entity.getContentDescription());
        vo.setServiceItems(entity.getServiceItems());
        vo.setApplicableParks(entity.getApplicableParks());
        vo.setApplicableCities(entity.getApplicableCities());
        vo.setValidDays(entity.getValidDays());
        vo.setShelfLifeDays(entity.getShelfLifeDays());
        vo.setIsTransferable(entity.getIsTransferable());
        vo.setIsStackable(entity.getIsStackable());
        vo.setMaxUseCount(entity.getMaxUseCount());
        vo.setCoverImage(entity.getCoverImage());
        vo.setCardDesignUrl(entity.getCardDesignUrl());
        vo.setTerms(entity.getTerms());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
