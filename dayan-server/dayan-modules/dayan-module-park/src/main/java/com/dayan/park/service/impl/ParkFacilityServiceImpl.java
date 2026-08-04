package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkFacilityCreateDTO;
import com.dayan.park.dto.ParkFacilityQueryDTO;
import com.dayan.park.dto.ParkFacilityUpdateDTO;
import com.dayan.park.entity.ParkFacility;
import com.dayan.park.mapper.ParkFacilityMapper;
import com.dayan.park.service.ParkFacilityService;
import com.dayan.park.vo.ParkFacilityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构设施（park_facility）服务实现。
 *
 * <p>facilityCode 唯一校验（同 parkCode 下）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkFacilityServiceImpl implements ParkFacilityService {

    private final ParkFacilityMapper facilityMapper;

    @Override
    public PageResult<ParkFacilityVO> page(ParkFacilityQueryDTO query) {
        LambdaQueryWrapper<ParkFacility> wrapper = buildWrapper(query);
        Page<ParkFacility> page = facilityMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkFacilityVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkFacilityVO> listByPark(String parkCode) {
        return facilityMapper.selectList(new LambdaQueryWrapper<ParkFacility>()
                .eq(ParkFacility::getParkCode, parkCode)
                .orderByAsc(ParkFacility::getSortOrder)
                .orderByAsc(ParkFacility::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkFacilityVO getDetail(Long id) {
        return toVO(requireFacility(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkFacilityCreateDTO dto) {
        Long count = facilityMapper.selectCount(new LambdaQueryWrapper<ParkFacility>()
                .eq(ParkFacility::getParkCode, dto.getParkCode())
                .eq(ParkFacility::getFacilityCode, dto.getFacilityCode()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "设施编码已存在: " + dto.getFacilityCode());
        }

        ParkFacility entity = new ParkFacility();
        entity.setParkCode(dto.getParkCode());
        entity.setFacilityCode(dto.getFacilityCode());
        entity.setFacilityName(dto.getFacilityName());
        entity.setFacilityCategory(dto.getFacilityCategory());
        entity.setBuildingName(dto.getBuildingName());
        entity.setFloor(dto.getFloor());
        entity.setArea(dto.getArea());
        entity.setCapacity(dto.getCapacity());
        entity.setOpenTime(dto.getOpenTime());
        entity.setFacilityDescription(dto.getFacilityDescription());
        entity.setCoverImage(dto.getCoverImage());
        entity.setImages(dto.getImages());
        entity.setIsFree(dto.getIsFree() == null ? 1 : dto.getIsFree());
        entity.setFeeDescription(dto.getFeeDescription());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        facilityMapper.insert(entity);
        log.info("创建机构设施成功: parkCode={}, facilityCode={}, id={}",
                dto.getParkCode(), dto.getFacilityCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkFacilityUpdateDTO dto) {
        ParkFacility existing = requireFacility(id);
        ParkFacility update = new ParkFacility();
        update.setId(existing.getId());
        if (dto.getFacilityName() != null) update.setFacilityName(dto.getFacilityName());
        if (dto.getFacilityCategory() != null) update.setFacilityCategory(dto.getFacilityCategory());
        if (dto.getBuildingName() != null) update.setBuildingName(dto.getBuildingName());
        if (dto.getFloor() != null) update.setFloor(dto.getFloor());
        if (dto.getArea() != null) update.setArea(dto.getArea());
        if (dto.getCapacity() != null) update.setCapacity(dto.getCapacity());
        if (dto.getOpenTime() != null) update.setOpenTime(dto.getOpenTime());
        if (dto.getFacilityDescription() != null) update.setFacilityDescription(dto.getFacilityDescription());
        if (dto.getCoverImage() != null) update.setCoverImage(dto.getCoverImage());
        if (dto.getImages() != null) update.setImages(dto.getImages());
        if (dto.getIsFree() != null) update.setIsFree(dto.getIsFree());
        if (dto.getFeeDescription() != null) update.setFeeDescription(dto.getFeeDescription());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        facilityMapper.updateById(update);
        log.info("更新机构设施成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkFacility existing = requireFacility(id);
        facilityMapper.deleteById(existing.getId());
        log.info("删除机构设施成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ParkFacility> buildWrapper(ParkFacilityQueryDTO query) {
        LambdaQueryWrapper<ParkFacility> wrapper = new LambdaQueryWrapper<ParkFacility>()
                .orderByAsc(ParkFacility::getSortOrder)
                .orderByAsc(ParkFacility::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkFacility::getParkCode, query.getParkCode());
        }
        if (query.getFacilityCode() != null && !query.getFacilityCode().isEmpty()) {
            wrapper.eq(ParkFacility::getFacilityCode, query.getFacilityCode());
        }
        if (query.getFacilityName() != null && !query.getFacilityName().isEmpty()) {
            wrapper.like(ParkFacility::getFacilityName, query.getFacilityName());
        }
        if (query.getFacilityCategory() != null) {
            wrapper.eq(ParkFacility::getFacilityCategory, query.getFacilityCategory());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkFacility::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkFacility requireFacility(Long id) {
        ParkFacility entity = facilityMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构设施不存在: id=" + id);
        }
        return entity;
    }

    private ParkFacilityVO toVO(ParkFacility entity) {
        ParkFacilityVO vo = new ParkFacilityVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setFacilityCode(entity.getFacilityCode());
        vo.setFacilityName(entity.getFacilityName());
        vo.setFacilityCategory(entity.getFacilityCategory());
        vo.setBuildingName(entity.getBuildingName());
        vo.setFloor(entity.getFloor());
        vo.setArea(entity.getArea());
        vo.setCapacity(entity.getCapacity());
        vo.setOpenTime(entity.getOpenTime());
        vo.setFacilityDescription(entity.getFacilityDescription());
        vo.setCoverImage(entity.getCoverImage());
        vo.setImages(entity.getImages());
        vo.setIsFree(entity.getIsFree());
        vo.setFeeDescription(entity.getFeeDescription());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
