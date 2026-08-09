package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkServiceTypeCreateDTO;
import com.dayan.park.dto.ParkServiceTypeQueryDTO;
import com.dayan.park.dto.ParkServiceTypeUpdateDTO;
import com.dayan.park.entity.ParkServiceType;
import com.dayan.park.mapper.ParkServiceTypeMapper;
import com.dayan.park.service.ParkServiceTypeService;
import com.dayan.park.vo.ParkServiceTypeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构服务类型（park_service_type）服务实现。
 *
 * <p>serviceTypeCode 唯一校验（同 parkCode 下）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkServiceTypeServiceImpl implements ParkServiceTypeService {

    private final ParkServiceTypeMapper serviceTypeMapper;

    @Override
    public PageResult<ParkServiceTypeVO> page(ParkServiceTypeQueryDTO query) {
        LambdaQueryWrapper<ParkServiceType> wrapper = buildWrapper(query);
        Page<ParkServiceType> page = serviceTypeMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkServiceTypeVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkServiceTypeVO> listByPark(String parkCode) {
        return serviceTypeMapper.selectList(new LambdaQueryWrapper<ParkServiceType>()
                .eq(ParkServiceType::getParkCode, parkCode)
                .orderByAsc(ParkServiceType::getSortOrder)
                .orderByAsc(ParkServiceType::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkServiceTypeVO getDetail(Long id) {
        return toVO(requireServiceType(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkServiceTypeCreateDTO dto) {
        Long count = serviceTypeMapper.selectCount(new LambdaQueryWrapper<ParkServiceType>()
                .eq(ParkServiceType::getParkCode, dto.getParkCode())
                .eq(ParkServiceType::getServiceTypeCode, dto.getServiceTypeCode()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "服务类型编码已存在: " + dto.getServiceTypeCode());
        }

        ParkServiceType entity = new ParkServiceType();
        entity.setParkCode(dto.getParkCode());
        entity.setServiceTypeCode(dto.getServiceTypeCode());
        entity.setServiceTypeName(dto.getServiceTypeName());
        entity.setServiceTypeCategory(dto.getServiceTypeCategory());
        entity.setServiceTypeDescription(dto.getServiceTypeDescription());
        entity.setServiceTypeFrequency(dto.getServiceTypeFrequency());
        entity.setServiceTypeDuration(dto.getServiceTypeDuration());
        entity.setCoverImage(dto.getCoverImage());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        serviceTypeMapper.insert(entity);
        log.info("创建机构服务类型成功: parkCode={}, serviceTypeCode={}, id={}",
                dto.getParkCode(), dto.getServiceTypeCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkServiceTypeUpdateDTO dto) {
        ParkServiceType existing = requireServiceType(id);
        ParkServiceType update = new ParkServiceType();
        update.setId(existing.getId());
        if (dto.getServiceTypeName() != null) update.setServiceTypeName(dto.getServiceTypeName());
        if (dto.getServiceTypeCategory() != null) update.setServiceTypeCategory(dto.getServiceTypeCategory());
        if (dto.getServiceTypeDescription() != null) update.setServiceTypeDescription(dto.getServiceTypeDescription());
        if (dto.getServiceTypeFrequency() != null) update.setServiceTypeFrequency(dto.getServiceTypeFrequency());
        if (dto.getServiceTypeDuration() != null) update.setServiceTypeDuration(dto.getServiceTypeDuration());
        if (dto.getCoverImage() != null) update.setCoverImage(dto.getCoverImage());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        serviceTypeMapper.updateById(update);
        log.info("更新机构服务类型成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkServiceType existing = requireServiceType(id);
        serviceTypeMapper.deleteById(existing.getId());
        log.info("删除机构服务类型成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkServiceType> buildWrapper(ParkServiceTypeQueryDTO query) {
        LambdaQueryWrapper<ParkServiceType> wrapper = new LambdaQueryWrapper<ParkServiceType>()
                .orderByAsc(ParkServiceType::getSortOrder)
                .orderByAsc(ParkServiceType::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkServiceType::getParkCode, query.getParkCode());
        }
        if (query.getServiceTypeCode() != null && !query.getServiceTypeCode().isEmpty()) {
            wrapper.eq(ParkServiceType::getServiceTypeCode, query.getServiceTypeCode());
        }
        if (query.getServiceTypeName() != null && !query.getServiceTypeName().isEmpty()) {
            wrapper.like(ParkServiceType::getServiceTypeName, query.getServiceTypeName());
        }
        if (query.getServiceTypeCategory() != null) {
            wrapper.eq(ParkServiceType::getServiceTypeCategory, query.getServiceTypeCategory());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkServiceType::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkServiceType requireServiceType(Long id) {
        ParkServiceType entity = serviceTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构服务类型不存在: id=" + id);
        }
        return entity;
    }

    private ParkServiceTypeVO toVO(ParkServiceType entity) {
        ParkServiceTypeVO vo = new ParkServiceTypeVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setServiceTypeCode(entity.getServiceTypeCode());
        vo.setServiceTypeName(entity.getServiceTypeName());
        vo.setServiceTypeCategory(entity.getServiceTypeCategory());
        vo.setServiceTypeDescription(entity.getServiceTypeDescription());
        vo.setServiceTypeFrequency(entity.getServiceTypeFrequency());
        vo.setServiceTypeDuration(entity.getServiceTypeDuration());
        vo.setCoverImage(entity.getCoverImage());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
