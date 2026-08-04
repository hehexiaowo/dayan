package com.dayan.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.ServiceVisitRecordCreateDTO;
import com.dayan.service.dto.ServiceVisitRecordQueryDTO;
import com.dayan.service.dto.ServiceVisitRecordUpdateDTO;
import com.dayan.service.entity.ServiceVisitRecord;
import com.dayan.service.mapper.ServiceVisitRecordMapper;
import com.dayan.service.service.ServiceVisitRecordService;
import com.dayan.service.vo.ServiceVisitRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 探访记录（service_visit_record）服务实现。
 *
 * <p>按 butlerCode/parkCode 聚合；overallScore 综合评分（0-100）；
 * 6 项检查（facility/service/hygiene/food/safety 文本 + issuesFound 发现问题）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceVisitRecordServiceImpl implements ServiceVisitRecordService {

    /** 探访记录默认状态：1=有效 */
    private static final int STATUS_NORMAL = 1;
    /** 默认探访目的：1=常规巡检 */
    private static final int DEFAULT_VISIT_PURPOSE = 1;

    private final ServiceVisitRecordMapper visitRecordMapper;

    @Override
    public PageResult<ServiceVisitRecordVO> page(ServiceVisitRecordQueryDTO query) {
        LambdaQueryWrapper<ServiceVisitRecord> wrapper = buildWrapper(query);
        Page<ServiceVisitRecord> page = visitRecordMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ServiceVisitRecordVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ServiceVisitRecordVO> list(ServiceVisitRecordQueryDTO query) {
        return visitRecordMapper.selectList(buildWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ServiceVisitRecordVO getDetail(Long id) {
        return toVO(requireVisitRecord(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ServiceVisitRecordCreateDTO dto) {
        ServiceVisitRecord entity = new ServiceVisitRecord();
        entity.setButlerCode(dto.getButlerCode());
        entity.setParkCode(dto.getParkCode());
        entity.setVisitDate(dto.getVisitDate() == null ? LocalDate.now() : dto.getVisitDate());
        entity.setVisitPurpose(dto.getVisitPurpose() == null ? DEFAULT_VISIT_PURPOSE : dto.getVisitPurpose());
        entity.setFacilityCheck(dto.getFacilityCheck());
        entity.setServiceCheck(dto.getServiceCheck());
        entity.setHygieneCheck(dto.getHygieneCheck());
        entity.setFoodCheck(dto.getFoodCheck());
        entity.setSafetyCheck(dto.getSafetyCheck());
        entity.setOverallScore(dto.getOverallScore());
        entity.setIssuesFound(dto.getIssuesFound());
        entity.setImprovementSuggestions(dto.getImprovementSuggestions());
        entity.setImages(dto.getImages());
        entity.setStatus(STATUS_NORMAL);
        entity.setRemark(dto.getRemark());
        visitRecordMapper.insert(entity);
        log.info("创建探访记录成功: butlerCode={}, parkCode={}, id={}",
                dto.getButlerCode(), dto.getParkCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ServiceVisitRecordUpdateDTO dto) {
        ServiceVisitRecord existing = requireVisitRecord(id);

        ServiceVisitRecord update = new ServiceVisitRecord();
        update.setId(existing.getId());
        if (dto.getVisitDate() != null) update.setVisitDate(dto.getVisitDate());
        if (dto.getVisitPurpose() != null) update.setVisitPurpose(dto.getVisitPurpose());
        if (dto.getFacilityCheck() != null) update.setFacilityCheck(dto.getFacilityCheck());
        if (dto.getServiceCheck() != null) update.setServiceCheck(dto.getServiceCheck());
        if (dto.getHygieneCheck() != null) update.setHygieneCheck(dto.getHygieneCheck());
        if (dto.getFoodCheck() != null) update.setFoodCheck(dto.getFoodCheck());
        if (dto.getSafetyCheck() != null) update.setSafetyCheck(dto.getSafetyCheck());
        if (dto.getOverallScore() != null) update.setOverallScore(dto.getOverallScore());
        if (dto.getIssuesFound() != null) update.setIssuesFound(dto.getIssuesFound());
        if (dto.getImprovementSuggestions() != null) update.setImprovementSuggestions(dto.getImprovementSuggestions());
        if (dto.getImages() != null) update.setImages(dto.getImages());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        visitRecordMapper.updateById(update);
        log.info("更新探访记录成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ServiceVisitRecord existing = requireVisitRecord(id);
        visitRecordMapper.deleteById(existing.getId());
        log.info("删除探访记录成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ServiceVisitRecord> buildWrapper(ServiceVisitRecordQueryDTO query) {
        LambdaQueryWrapper<ServiceVisitRecord> wrapper = new LambdaQueryWrapper<ServiceVisitRecord>()
                .orderByDesc(ServiceVisitRecord::getVisitDate)
                .orderByDesc(ServiceVisitRecord::getId);
        if (query.getButlerCode() != null && !query.getButlerCode().isEmpty()) {
            wrapper.eq(ServiceVisitRecord::getButlerCode, query.getButlerCode());
        }
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ServiceVisitRecord::getParkCode, query.getParkCode());
        }
        if (query.getVisitPurpose() != null) {
            wrapper.eq(ServiceVisitRecord::getVisitPurpose, query.getVisitPurpose());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ServiceVisitRecord::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ServiceVisitRecord requireVisitRecord(Long id) {
        ServiceVisitRecord entity = visitRecordMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "探访记录不存在: id=" + id);
        }
        return entity;
    }

    private ServiceVisitRecordVO toVO(ServiceVisitRecord entity) {
        ServiceVisitRecordVO vo = new ServiceVisitRecordVO();
        vo.setId(entity.getId());
        vo.setButlerCode(entity.getButlerCode());
        vo.setParkCode(entity.getParkCode());
        vo.setVisitDate(entity.getVisitDate());
        vo.setVisitPurpose(entity.getVisitPurpose());
        vo.setFacilityCheck(entity.getFacilityCheck());
        vo.setServiceCheck(entity.getServiceCheck());
        vo.setHygieneCheck(entity.getHygieneCheck());
        vo.setFoodCheck(entity.getFoodCheck());
        vo.setSafetyCheck(entity.getSafetyCheck());
        vo.setOverallScore(entity.getOverallScore());
        vo.setIssuesFound(entity.getIssuesFound());
        vo.setImprovementSuggestions(entity.getImprovementSuggestions());
        vo.setImages(entity.getImages());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
