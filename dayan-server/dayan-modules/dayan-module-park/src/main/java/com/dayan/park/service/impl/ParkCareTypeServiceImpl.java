package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkCareTypeCreateDTO;
import com.dayan.park.dto.ParkCareTypeQueryDTO;
import com.dayan.park.dto.ParkCareTypeUpdateDTO;
import com.dayan.park.entity.ParkCareType;
import com.dayan.park.mapper.ParkCareTypeMapper;
import com.dayan.park.service.ParkCareTypeService;
import com.dayan.park.vo.ParkCareTypeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 照护类型（park_care_type）服务实现。
 *
 * <p>careTypeCode 同 parkCode 下唯一。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkCareTypeServiceImpl implements ParkCareTypeService {

    private final ParkCareTypeMapper careTypeMapper;

    @Override
    public PageResult<ParkCareTypeVO> page(ParkCareTypeQueryDTO query) {
        LambdaQueryWrapper<ParkCareType> wrapper = buildWrapper(query);
        Page<ParkCareType> page = careTypeMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkCareTypeVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkCareTypeVO> listByPark(String parkCode) {
        return careTypeMapper.selectList(new LambdaQueryWrapper<ParkCareType>()
                .eq(ParkCareType::getParkCode, parkCode)
                .orderByAsc(ParkCareType::getCareLevel)
                .orderByAsc(ParkCareType::getSortOrder)
                .orderByAsc(ParkCareType::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkCareTypeVO getDetail(Long id) {
        return toVO(requireCareType(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkCareTypeCreateDTO dto) {
        Long count = careTypeMapper.selectCount(new LambdaQueryWrapper<ParkCareType>()
                .eq(ParkCareType::getParkCode, dto.getParkCode())
                .eq(ParkCareType::getCareTypeCode, dto.getCareTypeCode()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "照护类型编码已存在: " + dto.getCareTypeCode());
        }

        ParkCareType entity = new ParkCareType();
        entity.setParkCode(dto.getParkCode());
        entity.setCareTypeCode(dto.getCareTypeCode());
        entity.setCareTypeName(dto.getCareTypeName());
        entity.setCareLevel(dto.getCareLevel());
        entity.setCareTarget(dto.getCareTarget());
        entity.setCareItems(dto.getCareItems());
        entity.setCareFrequency(dto.getCareFrequency());
        entity.setNursePatientRatio(dto.getNursePatientRatio());
        entity.setAssessmentCriteria(dto.getAssessmentCriteria());
        entity.setDescription(dto.getDescription());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        careTypeMapper.insert(entity);
        log.info("创建照护类型成功: parkCode={}, careTypeCode={}, id={}",
                dto.getParkCode(), dto.getCareTypeCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkCareTypeUpdateDTO dto) {
        ParkCareType existing = requireCareType(id);
        ParkCareType update = new ParkCareType();
        update.setId(existing.getId());
        if (dto.getCareTypeName() != null) update.setCareTypeName(dto.getCareTypeName());
        if (dto.getCareLevel() != null) update.setCareLevel(dto.getCareLevel());
        if (dto.getCareTarget() != null) update.setCareTarget(dto.getCareTarget());
        if (dto.getCareItems() != null) update.setCareItems(dto.getCareItems());
        if (dto.getCareFrequency() != null) update.setCareFrequency(dto.getCareFrequency());
        if (dto.getNursePatientRatio() != null) update.setNursePatientRatio(dto.getNursePatientRatio());
        if (dto.getAssessmentCriteria() != null) update.setAssessmentCriteria(dto.getAssessmentCriteria());
        if (dto.getDescription() != null) update.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        careTypeMapper.updateById(update);
        log.info("更新照护类型成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkCareType existing = requireCareType(id);
        careTypeMapper.deleteById(existing.getId());
        log.info("删除照护类型成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkCareType> buildWrapper(ParkCareTypeQueryDTO query) {
        LambdaQueryWrapper<ParkCareType> wrapper = new LambdaQueryWrapper<ParkCareType>()
                .orderByAsc(ParkCareType::getCareLevel)
                .orderByAsc(ParkCareType::getSortOrder)
                .orderByAsc(ParkCareType::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkCareType::getParkCode, query.getParkCode());
        }
        if (query.getCareTypeCode() != null && !query.getCareTypeCode().isEmpty()) {
            wrapper.eq(ParkCareType::getCareTypeCode, query.getCareTypeCode());
        }
        if (query.getCareTypeName() != null && !query.getCareTypeName().isEmpty()) {
            wrapper.like(ParkCareType::getCareTypeName, query.getCareTypeName());
        }
        if (query.getCareLevel() != null) {
            wrapper.eq(ParkCareType::getCareLevel, query.getCareLevel());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkCareType::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkCareType requireCareType(Long id) {
        ParkCareType entity = careTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "照护类型不存在: id=" + id);
        }
        return entity;
    }

    private ParkCareTypeVO toVO(ParkCareType entity) {
        ParkCareTypeVO vo = new ParkCareTypeVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setCareTypeCode(entity.getCareTypeCode());
        vo.setCareTypeName(entity.getCareTypeName());
        vo.setCareLevel(entity.getCareLevel());
        vo.setCareTarget(entity.getCareTarget());
        vo.setCareItems(entity.getCareItems());
        vo.setCareFrequency(entity.getCareFrequency());
        vo.setNursePatientRatio(entity.getNursePatientRatio());
        vo.setAssessmentCriteria(entity.getAssessmentCriteria());
        vo.setDescription(entity.getDescription());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
