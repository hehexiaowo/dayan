package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkFacilityTypeCreateDTO;
import com.dayan.park.dto.ParkFacilityTypeQueryDTO;
import com.dayan.park.dto.ParkFacilityTypeUpdateDTO;
import com.dayan.park.entity.ParkFacilityType;
import com.dayan.park.mapper.ParkFacilityTypeMapper;
import com.dayan.park.service.ParkFacilityTypeService;
import com.dayan.park.vo.ParkFacilityTypeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构设施类型（park_facility_type）服务实现。
 *
 * <p>facilityTypeCode 唯一校验（同 parkCode 下）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkFacilityTypeServiceImpl implements ParkFacilityTypeService {

    private final ParkFacilityTypeMapper facilityTypeMapper;

    @Override
    public PageResult<ParkFacilityTypeVO> page(ParkFacilityTypeQueryDTO query) {
        LambdaQueryWrapper<ParkFacilityType> wrapper = buildWrapper(query);
        Page<ParkFacilityType> page = facilityTypeMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkFacilityTypeVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkFacilityTypeVO> listByPark(String parkCode) {
        return facilityTypeMapper.selectList(new LambdaQueryWrapper<ParkFacilityType>()
                .eq(ParkFacilityType::getParkCode, parkCode)
                .orderByAsc(ParkFacilityType::getSortOrder)
                .orderByAsc(ParkFacilityType::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkFacilityTypeVO getDetail(Long id) {
        return toVO(requireFacilityType(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkFacilityTypeCreateDTO dto) {
        Long count = facilityTypeMapper.selectCount(new LambdaQueryWrapper<ParkFacilityType>()
                .eq(ParkFacilityType::getParkCode, dto.getParkCode())
                .eq(ParkFacilityType::getFacilityTypeCode, dto.getFacilityTypeCode()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "设施类型编码已存在: " + dto.getFacilityTypeCode());
        }

        ParkFacilityType entity = new ParkFacilityType();
        entity.setParkCode(dto.getParkCode());
        entity.setFacilityTypeCode(dto.getFacilityTypeCode());
        entity.setFacilityTypeName(dto.getFacilityTypeName());
        entity.setFacilityTypeCategory(dto.getFacilityTypeCategory());
        entity.setBuildingName(dto.getBuildingName());
        entity.setFloor(dto.getFloor());
        entity.setArea(dto.getArea());
        entity.setCapacity(dto.getCapacity());
        entity.setOpenTime(dto.getOpenTime());
        entity.setFacilityTypeDescription(dto.getFacilityTypeDescription());
        entity.setCoverImage(dto.getCoverImage());
        entity.setImages(dto.getImages());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        facilityTypeMapper.insert(entity);
        log.info("创建机构设施类型成功: parkCode={}, facilityTypeCode={}, id={}",
                dto.getParkCode(), dto.getFacilityTypeCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkFacilityTypeUpdateDTO dto) {
        ParkFacilityType existing = requireFacilityType(id);
        ParkFacilityType update = new ParkFacilityType();
        update.setId(existing.getId());
        if (dto.getFacilityTypeName() != null) update.setFacilityTypeName(dto.getFacilityTypeName());
        if (dto.getFacilityTypeCategory() != null) update.setFacilityTypeCategory(dto.getFacilityTypeCategory());
        if (dto.getBuildingName() != null) update.setBuildingName(dto.getBuildingName());
        if (dto.getFloor() != null) update.setFloor(dto.getFloor());
        if (dto.getArea() != null) update.setArea(dto.getArea());
        if (dto.getCapacity() != null) update.setCapacity(dto.getCapacity());
        if (dto.getOpenTime() != null) update.setOpenTime(dto.getOpenTime());
        if (dto.getFacilityTypeDescription() != null) update.setFacilityTypeDescription(dto.getFacilityTypeDescription());
        if (dto.getCoverImage() != null) update.setCoverImage(dto.getCoverImage());
        if (dto.getImages() != null) update.setImages(dto.getImages());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        facilityTypeMapper.updateById(update);
        log.info("更新机构设施类型成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkFacilityType existing = requireFacilityType(id);
        facilityTypeMapper.deleteById(existing.getId());
        log.info("删除机构设施类型成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkFacilityType> buildWrapper(ParkFacilityTypeQueryDTO query) {
        LambdaQueryWrapper<ParkFacilityType> wrapper = new LambdaQueryWrapper<ParkFacilityType>()
                .orderByAsc(ParkFacilityType::getSortOrder)
                .orderByAsc(ParkFacilityType::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkFacilityType::getParkCode, query.getParkCode());
        }
        if (query.getFacilityTypeCode() != null && !query.getFacilityTypeCode().isEmpty()) {
            wrapper.eq(ParkFacilityType::getFacilityTypeCode, query.getFacilityTypeCode());
        }
        if (query.getFacilityTypeName() != null && !query.getFacilityTypeName().isEmpty()) {
            wrapper.like(ParkFacilityType::getFacilityTypeName, query.getFacilityTypeName());
        }
        if (query.getFacilityTypeCategory() != null) {
            wrapper.eq(ParkFacilityType::getFacilityTypeCategory, query.getFacilityTypeCategory());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkFacilityType::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkFacilityType requireFacilityType(Long id) {
        ParkFacilityType entity = facilityTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构设施类型不存在: id=" + id);
        }
        return entity;
    }

    private ParkFacilityTypeVO toVO(ParkFacilityType entity) {
        ParkFacilityTypeVO vo = new ParkFacilityTypeVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setFacilityTypeCode(entity.getFacilityTypeCode());
        vo.setFacilityTypeName(entity.getFacilityTypeName());
        vo.setFacilityTypeCategory(entity.getFacilityTypeCategory());
        vo.setBuildingName(entity.getBuildingName());
        vo.setFloor(entity.getFloor());
        vo.setArea(entity.getArea());
        vo.setCapacity(entity.getCapacity());
        vo.setOpenTime(entity.getOpenTime());
        vo.setFacilityTypeDescription(entity.getFacilityTypeDescription());
        vo.setCoverImage(entity.getCoverImage());
        vo.setImages(entity.getImages());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
